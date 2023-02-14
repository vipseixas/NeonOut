scalaVersion := "3.2.2"

libraryDependencies ++= Seq(
  "com.softwaremill.sttp.client3" %% "core" % "3.8.11" ,
  "com.lihaoyi" %% "os-lib" % "0.9.0" ,
  "io.spray" %% "spray-json" % "1.3.6" ,
  "org.scala-lang.modules" %% "scala-parallel-collections" % "1.0.4" 
)

libraryDependencies ++= Seq(
  "com.softwaremill.sttp.client3" %% "core" % "3.8.11" % Test,
  "com.lihaoyi" %% "os-lib" % "0.9.0" % Test,
  "io.spray" %% "spray-json" % "1.3.6" % Test,
  "org.scala-lang.modules" %% "scala-parallel-collections" % "1.0.4" % Test
)

