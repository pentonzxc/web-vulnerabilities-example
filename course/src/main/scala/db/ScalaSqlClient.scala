package db

import org.postgresql.ds.{PGSimpleDataSource => PgDataSource}
import scalasql.core.DbClient.{DataSource => LibDataSource}
import scalasql.dialects.PostgresDialect._

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.Success

object ScalaSqlClient {
  def postgresClient(pgConfig: PgDataSource)(implicit
      ex: ExecutionContext): Future[LibDataSource] = {
    val client = new LibDataSource(pgConfig, config = new scalasql.Config {})

    val postgresCheck = Future(pgConfig.getConnection()).map(_.close())

    postgresCheck.onComplete {
      case Success(_) => println(s"Postgres is started on ${pgConfig.getUrl}")
    }

    // init connection check
    postgresCheck.map(_ => client)
  }

}
