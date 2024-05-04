scalaVersion := "2.13.12"

lazy val course = project.in(file("."))
  .settings(
    name := "course",
    libraryDependencies ++= akka ++ scalasql ++ postgres
  )


val AkkaVersion = "2.8.5"
val AkkaHttpVersion = "10.5.3"
lazy val akka = Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
  "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion
)

lazy val scalasql = Seq("com.lihaoyi" %% "scalasql" % "0.1.2")

lazy val postgres = Seq("org.postgresql" % "postgresql" % "42.6.0")




