name := """scala-web-project"""
version := "1.0-SNAPSHOT"
scalaVersion := "2.11.8"

// corresponding to PlayPlugin in project/plugins.sbt
lazy val root = (project in file(".")).enablePlugins(PlayScala)

// corresponding to DigestPlugin in project/plugins.sbt
pipelineStages := Seq(digest)

// adds play modules
libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  evolutions,
  "com.softwaremill.macwire" %% "macros" % "2.2.0" % "provided",
  "com.softwaremill.macwire" %% "util" % "2.2.0",

  "org.postgresql" % "postgresql" % "9.4.1207.jre7",

  "org.scalikejdbc" %% "scalikejdbc" % "2.3.5",
  "org.scalikejdbc" %% "scalikejdbc-config" % "2.3.5",
  "ch.qos.logback" % "logback-classic" % "1.1.3",

  "de.svenkubiak" % "jBCrypt" % "0.4.1", // hash(x)

  // for testing
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.0" % "test",
  "org.mockito" % "mockito-core" % "2.0.45-beta"
)

// resolvers automatically contain Maven repo, it points to where you should be looking for libraries
resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"
