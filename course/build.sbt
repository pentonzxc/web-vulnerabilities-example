scalaVersion := "2.13.12"

lazy val course = project.in(file("."))
  .settings(
    name := "course",
    libraryDependencies ++= akka
  )


val AkkaVersion = "2.8.5"
val AkkaHttpVersion = "10.5.3"
lazy val akka = Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
  "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion
)





