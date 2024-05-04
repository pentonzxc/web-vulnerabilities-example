import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import db.ScalaSqlClient
import org.postgresql.ds.PGSimpleDataSource

import scala.concurrent.ExecutionContext

object App {
  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem[_] = ActorSystem(Behaviors.empty, "app")
    implicit val ec = system.executionContext

    val pg = new PGSimpleDataSource()

    pg.setURL("jdbc:postgresql://localhost:5432/postgres")
    pg.setUser("postgres")
    pg.setPassword("postgres")

    val app = for {
      dbSource <- ScalaSqlClient.postgresClient(pg)
      binding <- initHttp()
    } yield (binding, dbSource)

    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    scala.io.StdIn.readLine() // let it run until user presses return

    // shutdown
    (for {
      (binding, _) <- app
      _ <- binding.unbind()
    } yield ()).onComplete(_ => system.terminate())
  }

  def initHttp()(implicit
      system: ActorSystem[_],
      ex: ExecutionContext) = {

    val route = {
      path("hello") {
        get {
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Say hello to akka-http</h1>"))
        }
      }
    }

    Http().newServerAt("localhost", 8080).bind(route).andThen()
  }
}
