val libraries = Seq(
  "org.scalatest" % "scalatest_2.11" % "2.2.6" % "test",
  "org.scala-lang.modules" % "scala-parser-combinators_2.11" % "1.0.4",
  "com.assembla.scala-incubator" % "graph-core_2.11" % "1.11.0",
  "com.beachape" % "enumeratum_2.11" % "1.4.4"
)

lazy val root = (project in file(".")).
  settings(
    name := "KaiMing",
    organization := "edu.psu.ist.plato",
    version := "0.1",
    scalaVersion := "2.11.8",
    libraryDependencies ++= libraries
  )
