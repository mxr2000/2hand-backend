ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.1"

val http4sVersion     = "1.0.0-M40"
val slickVersion      = "3.5.0-M5"
val logbackVersion    = "1.4.7"
val redis4CatsVersion = "1.5.2"

libraryDependencies ++= Seq(
  "org.http4s"           %% "http4s-ember-client" % http4sVersion,
  "org.http4s"           %% "http4s-ember-server" % http4sVersion,
  "org.http4s"           %% "http4s-dsl"          % http4sVersion,
  "org.http4s"           %% "http4s-circe"        % http4sVersion,
  "ch.qos.logback"        % "logback-classic"     % "1.4.12",
  "org.postgresql"        % "postgresql"          % "42.7.1",
  "com.typesafe.slick"   %% "slick"               % slickVersion,
  "com.typesafe.slick"   %% "slick-codegen"       % slickVersion,
  "com.typesafe.slick"   %% "slick-hikaricp"      % slickVersion,
  "com.github.jwt-scala" %% "jwt-circe"           % "9.4.5",
  "org.scalameta"        %% "munit"               % "0.7.29" % Test,
  "dev.profunktor"       %% "redis4cats-effects"  % redis4CatsVersion,
  "com.cloudinary"        % "cloudinary-core"     % "1.37.0",
  "com.cloudinary"        % "cloudinary-http44"   % "1.33.0",
  "com.sun.mail"          % "javax.mail"          % "1.6.2"
)

assembly / assemblyMergeStrategy := {
  case PathList("META-INF", "versions", "9", "module-info.class") =>
    MergeStrategy.discard
  case PathList("module-info.class")           => MergeStrategy.discard
  case "META-INF/io.netty.versions.properties" => MergeStrategy.first
  case x => (assembly / assemblyMergeStrategy).value(x)
}

lazy val root = (project in file("."))
  .settings(
    name := "http4s-learn"
  )
