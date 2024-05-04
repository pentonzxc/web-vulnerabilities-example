import com.typesafe.config.{Config, ConfigFactory}

import scala.util.Try

case class AppConfig(
    postgresConfig: PostgresConfig,
    liquibaseConfig: LiquibaseConfig
)


object AppConfig {
  def make() : Try[AppConfig] = Try {
    val appConfig = ConfigFactory.load(ConfigFactory.defaultApplication())

    val liquibaseCfg = appConfig.getConfig("liquibase")
    val liquibase = LiquibaseConfig(
      url = liquibaseCfg.getString("url"),
      user = liquibaseCfg.getString("user"),
      password = liquibaseCfg.getString("password"),
      changelogPath = liquibaseCfg.getString("changelog")
    )

    val postgresConfig = appConfig.getConfig("postgres")
    val postgres = PostgresConfig(
      url = postgresConfig.getString("url"),
      user = postgresConfig.getString("user"),
      password = postgresConfig.getString("password")
    )


    AppConfig(postgres, liquibase)
  }
}

case class PostgresConfig(url: String, user: String, password: String)
case class LiquibaseConfig(url: String, user: String, password: String, changelogPath : String)
