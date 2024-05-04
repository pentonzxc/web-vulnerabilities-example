import org.postgresql.ds.{PGSimpleDataSource => PgDataSource}
import scalasql.core.DbClient.{DataSource => LibDataSource}
import scalasql.dialects.PostgresDialect._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

object PostgresClient {
  def create(pgConfig: PgDataSource)(implicit
      ex: ExecutionContext): Future[LibDataSource] = {
    val client = new LibDataSource(pgConfig, config = new scalasql.Config {})
    val connectionCheck = Future(pgConfig.getConnection()).map(_.close())

    connectionCheck.map(_ => client)
  } andThen {
    case Success(_) => println(s"Postgres is started")
  }

}
