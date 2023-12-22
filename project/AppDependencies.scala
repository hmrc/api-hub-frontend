import sbt._

object AppDependencies {
  import play.core.PlayVersion

  private val bootstrapPlayVersion = "7.22.0"
  private val hmrcMongoVersion = "1.3.0"

  val compile = Seq(
    play.sbt.PlayImport.ws,
    "uk.gov.hmrc"       %% "play-frontend-hmrc"             % "7.23.0-play-28" excludeAll(ExclusionRule("uk.gov.hmrc","url-builder_2.12")),
    "uk.gov.hmrc"       %% "play-conditional-form-mapping"  % "1.13.0-play-28",
    "uk.gov.hmrc"       %% "bootstrap-frontend-play-28"     % bootstrapPlayVersion,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-28"             % hmrcMongoVersion,
    "uk.gov.hmrc"       %% "internal-auth-client-play-28"   % "1.6.0",
    "uk.gov.hmrc"       %% "crypto-json-play-28"            % "7.3.0",
    "org.commonmark"    %  "commonmark"                     % "0.21.0",
    "org.commonmark"    %  "commonmark-ext-gfm-tables"      % "0.21.0"
  )

  val test = Seq(
    "org.scalatest"           %% "scalatest"               % "3.2.10",
    "org.scalatestplus"       %% "scalacheck-1-15"         % "3.2.10.0",
    "org.scalatestplus"       %% "mockito-3-4"             % "3.2.10.0",
    "org.scalatestplus.play"  %% "scalatestplus-play"      % "5.1.0",
    "org.pegdown"             %  "pegdown"                 % "1.6.0",
    "org.jsoup"               %  "jsoup"                   % "1.14.3",
    "com.typesafe.play"       %% "play-test"               % PlayVersion.current,
    "org.mockito"             %% "mockito-scala"           % "1.16.42",
    "org.scalacheck"          %% "scalacheck"              % "1.15.4",
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-28" % hmrcMongoVersion,
    "com.vladsch.flexmark"    %  "flexmark-all"            % "0.62.2",
    "uk.gov.hmrc"             %% "bootstrap-test-play-28"  % bootstrapPlayVersion,
    "nu.validator"            %  "validator"               % "20.7.2"
  ).map(_ % "test, it")

  def apply(): Seq[ModuleID] = compile ++ test
}
