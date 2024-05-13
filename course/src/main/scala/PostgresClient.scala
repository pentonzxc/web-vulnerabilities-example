import com.typesafe.scalalogging.StrictLogging
import doobie.Transactor
import org.postgresql.ds.{PGSimpleDataSource => PgDataSource}
import zio.interop.catz._
import zio.{RIO, Scope, Task, ZIO}

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext

object PostgresClient extends StrictLogging {

  // TODO: add logging on failed operations
  def create(pgDataSource: PgDataSource): RIO[Scope, Transactor[Task]] = {
    val pool = Executors.newSingleThreadExecutor()
    val ex = ExecutionContext.fromExecutorService(pool)
    val transactor = Transactor.fromDataSource[Task](
      pgDataSource,
      ex
    )

    val checkConnection =
      ZIO.acquireRelease(ZIO.attemptBlocking(pgDataSource.getConnection))(conn => ZIO.succeed(conn.close()))

    ZIO.scoped(checkConnection)
      .as(transactor)
      .tap(_ => ZIO.succeed(logger.info("Postgres is started")))
      .withFinalizer(_ => ZIO.attempt(ex.shutdown()).orDie)
  }
}
