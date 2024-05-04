lazy val root = (project in file("."))
  .aggregate(
    course
  )


lazy val course = project
