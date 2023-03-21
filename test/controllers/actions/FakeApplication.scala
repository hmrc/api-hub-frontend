package controllers.actions

import models.application.{Application, Creator, Environments, TeamMember}

import java.time.LocalDateTime

object FakeApplication extends Application(
  "fake-application-id",
  "fake-application-name",
  LocalDateTime.now(),
  Creator(FakeUser.email.get),
  LocalDateTime.now(),
  Seq(TeamMember(FakeUser.email.get)),
  Environments()
)
