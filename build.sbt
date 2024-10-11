import play.sbt.routes.RoutesKeys
import sbt.Def
import scoverage.ScoverageKeys
import uk.gov.hmrc.DefaultBuildSettings
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion
import scala.sys.process._

lazy val appName: String = "api-hub-frontend"
lazy val jsTest = taskKey[Unit]("jsTest")
lazy val jsHint = taskKey[Unit]("jsHint")

ThisBuild / majorVersion := 0
ThisBuild / scalaVersion := "3.5.0"
Compile / javaOptions += "-Xmx2G"

lazy val root = (project in file("."))
  .enablePlugins(PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin) //Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .settings(inConfig(Test)(testSettings)*)
  .settings(ThisBuild / useSuperShell := false)
  .settings(
    name := appName,
    RoutesKeys.routesImport ++= Seq(
      "models.{CheckMode, Mode, NormalMode, AddAnApiContext, AddAnApi, AddEndpoints}",
      "uk.gov.hmrc.play.bootstrap.binders.RedirectUrl"
    ),
    TwirlKeys.templateImports ++= Seq(
      "play.twirl.api.HtmlFormat",
      "play.twirl.api.HtmlFormat._",
      "uk.gov.hmrc.govukfrontend.views.html.components._",
      "uk.gov.hmrc.hmrcfrontend.views.html.components._",
      "uk.gov.hmrc.hmrcfrontend.views.html.helpers._",
      "uk.gov.hmrc.hmrcfrontend.views.config._",
      "views.ViewUtils._",
      "models.Mode",
      "controllers.routes._",
      "viewmodels.govuk.all._"
    ),
    PlayKeys.playDefaultPort := 9000,
    PlayKeys.devSettings ++= Seq(
      "play.http.router" -> "testOnlyDoNotUseInAppConf.Routes",
      "microservice.services.internal-auth.url" -> "http://localhost:9000/integration-hub/test-only",
      "urls.loginWithLdap" -> "http://localhost:9000/integration-hub/test-only/sign-in"
    ),
    ScoverageKeys.coverageExcludedFiles := "<empty>,Reverse.*,.*handlers.*,.*components.*," +
      ".*Routes.*,.*viewmodels.govuk.*,",
    ScoverageKeys.coverageMinimumStmtTotal := 78,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true,
    scalacOptions ++= Seq(
      "-feature",
      "-Wconf:msg=deprecation:w,msg=feature:w,msg=optimizer:w,src=target/.*:s"
    ),
    libraryDependencies ++= AppDependencies(),
    retrieveManaged := true,
    update / evictionWarningOptions :=
      EvictionWarningOptions.default.withWarnScalaVersionEviction(false),
    resolvers ++= Seq(Resolver.jcenterRepo),
    // concatenate js
    Concat.groups := Seq(
      "javascripts/application.js" ->
        group(Seq(
          "javascripts/app.js"
        ))
    ),
    pipelineStages := Seq(digest),
    // below line required to force asset pipeline to operate in dev rather than only prod
    Assets / pipelineStages := Seq(concat),
    Global / excludeLintKeys += update / evictionWarningOptions,
    scalacOptions ++= Seq("-deprecation", "-feature"),
    jsTest := {
      val exitCode = ("npm ci" #&& "npm test").!
      if (exitCode != 0) {
        throw new MessageOnlyException("npm install and test failed")
      }
    },
    jsHint := {
      val exitCode = ("npm ci" #&& "npm run jshint").!
      if (exitCode != 0) {
        throw new MessageOnlyException("jsHint checks failed")
      }
    },
    dependencyOverrides ++= Seq(
      "com.fasterxml.jackson.core"     %% "jackson-databind"        % "2.17.0",
      "com.fasterxml.jackson.core"     %% "jackson-core"            % "2.17.0",
      "com.fasterxml.jackson.core"     %% "jackson-annotations"     % "2.17.0",
      "com.fasterxml.jackson.datatype" %% "jackson-datatype-jsr310" % "2.17.0",
    )
  )
  .settings(scalacOptions := scalacOptions.value.diff(Seq("-Wunused:all")))
  .settings(scalacOptions += "-Wconf:msg=Flag.*repeatedly:s")

lazy val it = (project in file("it"))
  .enablePlugins(PlayScala)
  .dependsOn(root % "test->test")
  .settings(DefaultBuildSettings.itSettings())
  .settings(libraryDependencies ++= AppDependencies.it)
  .settings(scalacOptions := scalacOptions.value.diff(Seq("-Wunused:all")))
  .settings(scalacOptions += "-Wconf:msg=Flag.*repeatedly:s")

lazy val testSettings: Seq[Def.Setting[?]] = Seq(
  fork := true,
  unmanagedSourceDirectories += baseDirectory.value / "test-utils"
)

test := {
  jsTest.value
  jsHint.value
  (test in Test).value
}