ThisBuild / version := "1.0.0"

ThisBuild / scalaVersion := "3.2.2"

lazy val root = (project in file("."))
  .settings(
    name := "NeonOut",
    idePackagePrefix := Some("io.pixel")
  )

mainClass in (Compile, run) := Some("io.pixel.main")

//TODO use logging instead of prints
// libraryDependencies += "com.outr" %% "scribe" % "3.10.7"
libraryDependencies += "com.softwaremill.sttp.client3" %% "core" % "3.8.10"
libraryDependencies += "com.lihaoyi" %% "os-lib" % "0.9.0"
libraryDependencies += "io.spray" %%  "spray-json" % "1.3.6"

outputStrategy := Some(StdoutOutput)

// import only Scala 3, JVM projects
//ideSkipProject := (scalaVersion.value != "scala3") || thisProjectRef.value.project.contains("JS") || thisProjectRef.value.project.contains("Native")
