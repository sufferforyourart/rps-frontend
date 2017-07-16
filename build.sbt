name := """rps-frontend"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.8"

val mongoVersion="0.11.14"

libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test

libraryDependencies += ws

libraryDependencies ++= Seq(
  "org.reactivemongo" %% "play2-reactivemongo" % mongoVersion
)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

