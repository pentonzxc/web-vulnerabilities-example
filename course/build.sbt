scalaVersion := "2.13.12"

lazy val course = project.in(file("."))
  .settings(
    name := "course",
    libraryDependencies ++= akka ++ doobie ++ postgres ++ liquibase ++ typesafeConfig ++ zio ++ circe ++ akkaHttpCirceSupport ++ logging
  )

val AkkaVersion = "2.8.5"
val AkkaHttpVersion = "10.5.3"
lazy val akka = Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
  "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion
)

lazy val zio = Seq(
  "dev.zio" %% "zio-interop-cats" % "23.1.0.2",
  "dev.zio" %% "zio" % "2.1-RC1"
)

lazy val postgres = Seq("org.postgresql" % "postgresql" % "42.6.0")

lazy val doobie = Seq(
  "org.tpolecat" %% "doobie-core" % "1.0.0-RC4",
  "org.tpolecat" %% "doobie-postgres" % "1.0.0-RC4"
)

lazy val akkaHttpCirceSupport = Seq(
  "de.heikoseeberger" %% "akka-http-circe" % "1.40.0-RC3"
)

val circeVersion = "0.14.1"

val circe = Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-generic-extras",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)

lazy val typesafeConfig = Seq("com.typesafe" % "config" % "1.4.3")

val liquibase = Seq(
  "org.liquibase" % "liquibase-core" % "3.5.3"
//  "com.mattbertolini" % "liquibase-slf4j" % "2.0.0"
)

val logging = Seq(
  "ch.qos.logback" % "logback-classic" % "1.2.10",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.4",
  "net.logstash.logback" % "logstash-logback-encoder" % "7.4"
)
