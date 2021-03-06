val libraries = Seq(
  "org.scalatest" % "scalatest_2.11" % "3.0.0" % "test",
  "org.scala-graph" % "graph-core_2.11" % "1.11.3",
  "com.beachape" % "enumeratum_2.11" % "1.4.4",
  "com.lihaoyi" % "fastparse_2.11" % "0.4.2"
)

lazy val root = (project in file(".")).
  settings(
    name := "KaiMing",
    organization := "io.github.uraj",
    version := "0.1",
    scalaVersion := "2.11.8",
    libraryDependencies ++= libraries
  )

scalacOptions := Seq("-unchecked", "-deprecation", "-feature")

assemblyExcludedJars in assembly := { 
  val cp = (fullClasspath in assembly).value
  cp filter {_.data.getParentFile.getName != "lib"}
}
