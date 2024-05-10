package modules

import facade.{AuthFacade, AuthFacadeImpl, PostsFacade, PostsFacadeImpl, SessionFacade, SessionFacadeImpl}
import service.{PostsService, PostsServiceImpl, SessionService, SessionServiceImpl, UserService, UserServiceImpl}

class Services(repositories: Repositories) {
  val userService: UserService = new UserServiceImpl(repositories.userRepository)
  val sessionService: SessionService = new SessionServiceImpl(repositories.sessionRepository)
  val postsService : PostsService = new PostsServiceImpl(userService, repositories.postsRepository)
  val sessionFacade : SessionFacade = new SessionFacadeImpl(sessionService, userService)
  val authFacade: AuthFacade = new AuthFacadeImpl(userService, sessionService)
  val postsFacade : PostsFacade = new PostsFacadeImpl(postsService)
}
