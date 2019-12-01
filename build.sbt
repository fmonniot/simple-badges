val ScalaVersion = "2.13.1"

val CatsEffectTestingVersion = "0.3.0"
val CirceVersion = "0.12.3"
val Fs2Version = "2.1.0"
val Http4sVersion = "0.21.0-M6"
val JawnFs2Version = "0.15.0"
val LogbackVersion = "1.2.3"
val ScalaXmlVersion = "1.2.0"
val Specs2Version = "4.8.1"
val PureConfigVersion = "0.12.1"

lazy val `simple-badges` = (project in file("."))
  .aggregate(server, badges)

lazy val server = (project in file("./server"))
  .settings(commons)
  .dependsOn(badges)
  .settings(
    name := "server",
    libraryDependencies ++= Seq(
      "org.http4s"             %% "http4s-blaze-server"        % Http4sVersion,
      "org.http4s"             %% "http4s-blaze-client"        % Http4sVersion,
      "org.http4s"             %% "http4s-circe"               % Http4sVersion,
      "org.http4s"             %% "http4s-dsl"                 % Http4sVersion,
      "org.http4s"             %% "http4s-scala-xml"           % Http4sVersion,
      "io.circe"               %% "circe-generic"              % CirceVersion,
      "org.scala-lang.modules" %% "scala-xml"                  % ScalaXmlVersion,
      "com.github.pureconfig"  %% "pureconfig"                 % PureConfigVersion,
      "com.github.pureconfig"  %% "pureconfig-cats-effect"     % PureConfigVersion,
      "com.codecommit"         %% "cats-effect-testing-specs2" % CatsEffectTestingVersion % Test,
      "org.specs2"             %% "specs2-core"                % Specs2Version % Test,
      "org.specs2"             %% "specs2-matcher-extra"       % Specs2Version % Test,
      "ch.qos.logback"         % "logback-classic"             % LogbackVersion
    ),
    addCompilerPlugin("org.typelevel" %% "kind-projector"     % "0.10.3"),
    addCompilerPlugin("com.olegpy"    %% "better-monadic-for" % "0.3.1")
  )

lazy val badges = (project in file("./badges"))
  .settings(commons)
  .settings(
    name := "badges",
    libraryDependencies ++= Seq(
      "co.fs2"                 %% "fs2-io"               % Fs2Version,
      "io.circe"               %% "circe-generic"        % CirceVersion,
      "io.circe"               %% "circe-jawn"           % CirceVersion,
      "org.http4s"             %% "jawn-fs2"             % JawnFs2Version,
      "org.scala-lang"         % "scala-reflect"         % ScalaVersion,
      "org.scala-lang.modules" %% "scala-xml"            % ScalaXmlVersion,
      "org.specs2"             %% "specs2-core"          % Specs2Version % Test,
      "org.specs2"             %% "specs2-matcher-extra" % Specs2Version % Test
    )
  )

lazy val commons = Seq(
  organization := "eu.monniot.badges",
  version := "0.1.0-SNAPSHOT",
  scalaVersion := ScalaVersion,
  scalacOptions ++= Seq(
    "-deprecation",
    "-encoding",
    "UTF-8",
    "-language:higherKinds",
    "-language:postfixOps",
    "-language:experimental.macros",
    "-feature",
    "-Xfatal-warnings"
  )
)
