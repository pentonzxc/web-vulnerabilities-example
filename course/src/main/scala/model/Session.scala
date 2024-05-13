package model

import java.time.Instant

case class Session(
    id: SessionId,
    secretToken: SecretToken,
    iss: UserId,
    created: Instant,
    exp: Instant
)
