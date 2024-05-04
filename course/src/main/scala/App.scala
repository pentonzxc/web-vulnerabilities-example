import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import org.postgresql.ds.PGSimpleDataSource
import scalasql.core.DbClient
import zio.{Scope, ZIO, ZIOAppArgs, ZIOAppDefault}

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

object App extends ZIOAppDefault {

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = {
    implicit val actorSystem: ActorSystem[_] = ActorSystem(Behaviors.empty, "app")
    implicit val actorEx = actorSystem.executionContext

    val dependencies = for {
      appConfig <- ZIO.fromTry(AppConfig.make())

      _ <- ZIO.fromFuture(implicit ex =>
        LiquibaseMigrationRunner.run(appConfig.liquibaseConfig))

      _ <- ZIO.fromFuture { implicit ex =>
        initPostgres(appConfig.postgresConfig)
      }

      _ <- ZIO.acquireRelease(ZIO.fromFuture(_ => initHttpServer())) {
        bind => ZIO.fromFuture(implicit ex => bind.unbind()).orDie
      }

    } yield ()

    val onShutdownCloseActorSystem = ZIO.acquireRelease(ZIO.unit) { _ =>
      zio.Console.printLine("Closing actor system").orDie *>
        ZIO.succeed(actorSystem.terminate())
    }


    for {
      _ <- dependencies
      _ <- onShutdownCloseActorSystem
      _ <- ZIO.never
    } yield ()
  }

  def initPostgres(config: PostgresConfig)(implicit
      ex: ExecutionContext): Future[DbClient.DataSource] = {
    val pg = new PGSimpleDataSource()

    pg.setUrl(config.url)
    pg.setUser(config.user)
    pg.setPassword(config.password)

    PostgresClient.create(pg)
  }

  def initHttpServer()(implicit
      system: ActorSystem[_],
      executionContext: ExecutionContext): Future[Http.ServerBinding] = {
    val onShutdownCloseRequestTimeout = FiniteDuration(5, TimeUnit.SECONDS)

    val route = {
      path("hello") {
        get {
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Say hello to akka-http</h1>"))
        }
      }
    }

    Http().newServerAt("localhost", 8080).bind(route)
      .map(_.addToCoordinatedShutdown(onShutdownCloseRequestTimeout))
      .andThen {
        case Success(value) => println(s"Server is started on ${value.localAddress.getPort}")
      }
  }
}
