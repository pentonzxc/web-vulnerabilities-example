package model.auth

import model.{SessionId, UserId}

import java.time.Instant

case class Session(id: SessionId, iss: UserId, created: Instant, exp: Instant)
