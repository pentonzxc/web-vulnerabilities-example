import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor
import liquibase.{Contexts, LabelExpression, Liquibase}

import java.sql.DriverManager
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

object LiquibaseMigrationRunner {
  def run(config: LiquibaseConfig)(implicit
      ec: ExecutionContext): Future[Unit] = {
    Future(
      DriverManager.getConnection(config.url, config.user, config.password)
    ).map { conn =>
      val ra = new ClassLoaderResourceAccessor(getClass().getClassLoader())
      val db = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(conn))
      val liquibase = new Liquibase("migrations/changelog.xml", ra, db)

      liquibase.update(new Contexts(), new LabelExpression())
      conn.close()
    }
  } andThen {
    case Success(_) => println("Migrations is succeed")
  }
}
