import doobie.Transactor
import org.postgresql.ds.{PGSimpleDataSource => PgDataSource}
import zio.interop.catz._
import zio.{RIO, Scope, Task, ZIO}

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext

object PostgresClient {
  def create(pgDataSource: PgDataSource): RIO[Scope, Transactor[Task]] = {
    val pool = Executors.newSingleThreadExecutor()
    val ex = ExecutionContext.fromExecutorService(pool)
    val transactor = Transactor.fromDataSource[Task](
      pgDataSource,
      ex
    )

    val checkConnection =
      ZIO.acquireRelease(ZIO.attemptBlocking(pgDataSource.getConnection))(conn => ZIO.succeed(conn.close()))

    (for {
      _ <- ZIO.scoped(checkConnection)
      _ <- zio.Console.printLine("Postgres is started")
    } yield transactor).withFinalizer(_ => ZIO.attempt(ex.shutdown()).orDie)
  }
}
