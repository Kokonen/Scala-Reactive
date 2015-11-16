name := """Bank"""

version := "1.0"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.4.0" withSources() withJavadoc(),
  "com.typesafe.akka" %% "akka-testkit" % "2.4.0" % "test" withSources() withJavadoc(),
  "org.scalatest" %% "scalatest" % "2.2.4" % "test" withSources() withJavadoc(),
  "com.typesafe.akka" %% "akka-persistence" % "2.4.0" withSources() withJavadoc(),
  "org.iq80.leveldb"            % "leveldb"          % "0.7" withSources() withJavadoc(),
  "org.fusesource.leveldbjni"   % "leveldbjni-all"   % "1.8" withSources() withJavadoc(),
  "com.geteventstore" %% "akka-persistence-eventstore" % "2.1.0")
