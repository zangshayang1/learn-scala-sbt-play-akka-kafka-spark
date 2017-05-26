val commonSettings = Seq(
    scalaVersion := "2.11.7", 
    organization := "com.appliedscala.sbt",
    version := "1.0-SNAPSHOT"
    )

lazy val common = (project in file("common"))
lazy val backend = (project in file("backend"))

lazy val root = (project in file(".")).
    settings(commonSettings).
    aggregate(common, backend)
