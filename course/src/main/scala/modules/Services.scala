package modules

import service.{SessionService, SessionServiceImpl, UserService, UserServiceImpl}

class Services(repositories : Repositories) {
  val userService : UserService = new UserServiceImpl(repositories.userRepository)
  val sessionService : SessionService = new SessionServiceImpl(repositories.sessionRepository)
}
