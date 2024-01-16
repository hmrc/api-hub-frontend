import sbt._

object AppDependencies {
  import play.core.PlayVersion

  private val bootstrapPlayVersion = "8.4.0"
  private val hmrcMongoVersion = "1.7.0"

  val compile = Seq(
    play.sbt.PlayImport.ws,
    "uk.gov.hmrc"       %% "play-frontend-hmrc-play-30"     % "8.3.0" excludeAll(ExclusionRule("uk.gov.hmrc","url-builder_2.12")),
    "uk.gov.hmrc"       %% "play-conditional-form-mapping-play-30"  % "2.0.0",
    "uk.gov.hmrc"       %% "bootstrap-frontend-play-30"     % bootstrapPlayVersion,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-30"             % hmrcMongoVersion,
    "uk.gov.hmrc"       %% "internal-auth-client-play-30"   % "1.9.0",
    "uk.gov.hmrc"       %% "crypto-json-play-30"            % "7.6.0"
  )

  val test = Seq(
    "org.scalatestplus"       %% "scalacheck-1-17"         % "3.2.17.0",
    "org.mockito"             %% "mockito-scala"           % "1.17.30",
    "org.scalacheck"          %% "scalacheck"              % "1.17.0",
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-30" % hmrcMongoVersion,
    "uk.gov.hmrc"             %% "bootstrap-test-play-30"  % bootstrapPlayVersion,
    "nu.validator"            %  "validator"               % "20.7.2"
  ).map(_ % Test)

  val it = Seq.empty

  def apply(): Seq[ModuleID] = compile ++ test
}
