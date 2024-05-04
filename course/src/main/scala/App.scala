import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives.path
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route.seal


import scala.concurrent.ExecutionContext

object App {
  def main(args: Array[String]): Unit = {
    implicit val system : ActorSystem[_] = ActorSystem(Behaviors.empty, "app")
    implicit val ec = system.executionContext

    val binding = initHttp()

    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    scala.io.StdIn.readLine() // let it run until user presses return
    binding.flatMap(_.unbind())
      .onComplete(_ => system.terminate())
  }

  def initHttp()(implicit system : ActorSystem[_], ex : ExecutionContext) = {

    val route = {
      path("hello") {
        get {
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Say hello to akka-http</h1>"))
        }
      }
    }


    Http().newServerAt("localhost", 8080).bind(route)
  }
}
