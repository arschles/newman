import net.virtualvoid.sbt.graph.Plugin
import org.scalastyle.sbt.ScalastylePlugin
import NewmanReleaseSteps._
import sbtrelease._
import ReleaseStateTransformations._
import ReleasePlugin._
import ReleaseKeys._
import sbt._

name := "newman"

organization := "com.stackmob"

scalaVersion := "2.10.4"

scalacOptions := Seq("-unchecked", "-deprecation", "-feature")

scalacOptions in Test ++= Seq("-Yrangepos")

resolvers ++= List(
  "spray repo" at "http://repo.spray.io"
)

libraryDependencies ++= {
  val httpCoreVersion = "4.2.5"
  val httpClientVersion = "4.2.5"
  val scalaCheckVersion = "1.11.3"
  val specs2Version = "2.3.12"
  val mockitoVersion = "1.9.0"
  val liftJsonVersion = "3.0-M1"//"2.5.1"
  val sprayVersion = "1.2.0"
  val parboiledCore = ExclusionRule(organization = "org.parboiled")
  val scalap = ExclusionRule("org.scala-lang", "scalap")
  val commonsCodec = ExclusionRule("commons-codec", "commons-codec")
  val scalazCore = ExclusionRule("org.scalaz", "scalaz-core_2.10")
  val httpCore = ExclusionRule("org.apache.httpcomponents", "httpcore")
  Seq(
    "org.apache.httpcomponents" % "httpcore" % httpCoreVersion,
    "org.apache.httpcomponents" % "httpclient" % httpClientVersion excludeAll(httpCore),
    "io.spray" % "spray-client" % sprayVersion excludeAll(scalap),
    "io.spray" % "spray-caching" % sprayVersion excludeAll(scalap, parboiledCore),
    "com.typesafe.akka" %% "akka-actor" % "2.2.3" excludeAll(scalap, parboiledCore),
    "com.twitter" %% "finagle-http" % "6.5.0" excludeAll(commonsCodec, parboiledCore),
    "net.liftweb" %% "lift-json-scalaz7" % liftJsonVersion excludeAll(scalap, parboiledCore),
    "org.scalacheck" %% "scalacheck" % scalaCheckVersion % "test" excludeAll(parboiledCore),
    "org.specs2" %% "specs2" % specs2Version % "test" excludeAll(scalazCore, scalap, parboiledCore),
    "org.pegdown" % "pegdown" % "1.2.1" % "test" excludeAll(parboiledCore),
    "org.mockito" % "mockito-all" % mockitoVersion % "test"
  )
}

testOptions in Test += Tests.Argument("html", "console")

conflictManager := ConflictManager.strict

dependencyOverrides <+= (scalaVersion) { vsn => "org.scala-lang" % "scala-library" % vsn }

logBuffered := false

ScalastylePlugin.Settings

Plugin.graphSettings

releaseSettings

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  setReadmeReleaseVersion,
  tagRelease,
  publishArtifacts.copy(action = publishSignedAction),
  setNextVersion,
  commitNextVersion,
  pushChanges
)

conflictManager := ConflictManager.strict

publishTo <<= (version) { version: String =>
  val nexus = "https://oss.sonatype.org/"
  if (version.trim.endsWith("SNAPSHOT")) {
    Some("snapshots" at nexus + "content/repositories/snapshots")
   } else {
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
  }
}

publishMavenStyle := true

publishArtifact in Test := true

pomExtra := <url>https://github.com/stackmob/newman</url>
  <licenses>
    <license>
      <name>Apache 2</name>
      <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <scm>
    <url>git@github.com:stackmob/newman.git</url>
    <connection>scm:git:git@github.com:stackmob/newman.git</connection>
  </scm>
  <developers>
    <developer>
      <id>arschles</id>
      <name>Aaron Schlesinger</name>
      <url>http://www.stackmob.com</url>
    </developer>
    <developer>
      <id>devmage</id>
      <name>Andrew Harris</name>
      <url>http://www.stackmob.com</url>
    </developer>
    <developer>
      <id>taylorleese</id>
      <name>Taylor Leese</name>
      <url>http://www.stackmob.com</url>
    </developer>
    <developer>
      <id>kelseyq</id>
      <name>Kelsey Innis</name>
      <url>http://www.stackmob.com</url>
    </developer>
    <developer>
      <id>wpalmeri</id>
      <name>Will Palmeri</name>
      <url>http://www.stackmob.com</url>
    </developer>
    <developer>
      <id>jrwest</id>
      <name>Jordan West</name>
      <url>http://github.com/jrwest</url>
    </developer>
  </developers>
