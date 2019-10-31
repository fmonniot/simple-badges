val CirceVersion = "0.12.0-M4"
val Http4sVersion = "0.21.0-M3"
val LogbackVersion = "1.2.3"
val ScalaXmlVersion = "1.2.0"
val Specs2Version = "4.8.0"

lazy val root = (project in file("."))
  .aggregate(server, badges)

lazy val server = (project in file("./server"))
  .settings(commons)
  .dependsOn(badges)
  .settings(
    libraryDependencies ++= Seq(
      "org.http4s"             %% "http4s-blaze-server"  % Http4sVersion,
      "org.http4s"             %% "http4s-blaze-client"  % Http4sVersion,
      "org.http4s"             %% "http4s-circe"         % Http4sVersion,
      "org.http4s"             %% "http4s-dsl"           % Http4sVersion,
      "org.http4s"             %% "http4s-scala-xml"     % Http4sVersion,
      "io.circe"               %% "circe-generic"        % CirceVersion,
      "org.scala-lang.modules" %% "scala-xml"            % ScalaXmlVersion,
      "org.specs2"             %% "specs2-core"          % Specs2Version % Test,
      "org.specs2"             %% "specs2-matcher-extra" % Specs2Version % Test,
      "ch.qos.logback"         % "logback-classic"       % LogbackVersion,
    ),
    addCompilerPlugin("org.typelevel" %% "kind-projector"     % "0.10.3"),
    addCompilerPlugin("com.olegpy"    %% "better-monadic-for" % "0.3.0")
  )

lazy val badges = (project in file("./badges"))
  .settings(commons)
  .settings(
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-effect" % "2.0.0-M5",
      "co.fs2"                 %% "fs2-io"               % "1.1.0-M1",
      "io.circe"               %% "circe-generic"        % CirceVersion,
      "io.circe"               %% "circe-jawn"        % CirceVersion,
      "org.http4s"             %% "jawn-fs2"         % "0.15.0-M1",
    "org.scala-lang.modules" %% "scala-xml"            % ScalaXmlVersion,
    "org.specs2"             %% "specs2-core"          % Specs2Version % Test,
    "org.specs2"             %% "specs2-matcher-extra" % Specs2Version % Test,
    )
  )

lazy val commons = Seq(
  organization := "eu.monniot",
  name := "simple-badges",
  version := "0.0.1-SNAPSHOT",
  scalaVersion := "2.13.0",
  scalacOptions ++= Seq(
    "-deprecation",
    "-encoding",
    "UTF-8",
    "-language:higherKinds",
    "-language:postfixOps",
    "-language:experimental.macros",
    "-feature",
    "-Xfatal-warnings",
  )
)
