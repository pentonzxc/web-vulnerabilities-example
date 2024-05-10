import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import doobie.Transactor
import modules.{AppConfig, PostgresConfig, Repositories, Controllers, Services}
import org.postgresql.ds.PGSimpleDataSource
import zio.{RIO, Scope, Task, ZIO, ZIOAppArgs, ZIOAppDefault}

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

      pg <- initPostgres(appConfig.postgresConfig)

      repositories = new Repositories(postgresTx = pg)
      services = new Services(repositories)
      routes = new Controllers(services)

      _ <- ZIO.acquireRelease(ZIO.fromFuture(_ => initHttpServer(route = routes.httpRoute))) {
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

  def initPostgres(config: PostgresConfig): RIO[Scope, Transactor[Task]] = {
    val pg = new PGSimpleDataSource()

    pg.setUrl(config.url)
    pg.setUser(config.user)
    pg.setPassword(config.password)

    PostgresClient.create(pg)
  }

  def initHttpServer(route : Route)(implicit
      system: ActorSystem[_],
      executionContext: ExecutionContext): Future[Http.ServerBinding] = {
    val onShutdownCloseRequestTimeout = FiniteDuration(5, TimeUnit.SECONDS)

    Http().newServerAt("localhost", 8080).bind(route)
      .map(_.addToCoordinatedShutdown(onShutdownCloseRequestTimeout))
      .andThen {
        case Success(value) => println(s"Server is started on ${value.localAddress.getPort}")
      }
  }
}
