import sbt._

object AppDependencies {
  import play.core.PlayVersion

  private val bootstrapPlayVersion = "9.11.0"
  private val hmrcMongoVersion = "2.6.0"

  val compile = Seq(
    play.sbt.PlayImport.ws,
    "uk.gov.hmrc"       %% "play-frontend-hmrc-play-30"             % "10.13.0",
    "uk.gov.hmrc"       %% "play-conditional-form-mapping-play-30"  % "3.2.0",
    "uk.gov.hmrc"       %% "bootstrap-frontend-play-30"             % bootstrapPlayVersion,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-30"                     % hmrcMongoVersion,
    "uk.gov.hmrc"       %% "internal-auth-client-play-30"           % "3.1.0",
    "uk.gov.hmrc"       %% "crypto-json-play-30"                    % "8.2.0",
    "org.commonmark"    %  "commonmark"                             % "0.22.0",
    "org.commonmark"    %  "commonmark-ext-gfm-tables"              % "0.22.0",
    "io.swagger.parser.v3" % "swagger-parser" % "2.1.22"
      excludeAll(
        ExclusionRule("com.fasterxml.jackson.core", "jackson-databind"),
        ExclusionRule("com.fasterxml.jackson.datatype", "jackson-datatype-jsr310")
      ),
    "io.swagger" % "swagger-inflector" % "2.0.12"
      excludeAll(
        ExclusionRule("com.fasterxml.jackson.core", "jackson-databind"),
        ExclusionRule("com.fasterxml.jackson.datatype", "jackson-datatype-jsr310")
    ),
  )

  val test = Seq(
    "org.scalatestplus"       %% "scalacheck-1-17"         % "3.2.18.0",
    "org.scalatestplus"       %% "mockito-4-11"            % "3.2.18.0",
    "org.scalacheck"          %% "scalacheck"              % "1.18.0",
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-30" % hmrcMongoVersion excludeAll(ExclusionRule("com.vladsch.flexmark","flexmark-all")),
    "uk.gov.hmrc"             %% "bootstrap-test-play-30"  % bootstrapPlayVersion excludeAll(ExclusionRule("com.vladsch.flexmark","flexmark-all")),
    "nu.validator"            %  "validator"               % "20.7.2",
    "com.vladsch.flexmark"    %  "flexmark-all"            % "0.62.2"   // Increasing from this version breaks nu.validator:validator
  ).map(_ % Test)

  val it = Seq.empty

  def apply(): Seq[ModuleID] = compile ++ test
}
