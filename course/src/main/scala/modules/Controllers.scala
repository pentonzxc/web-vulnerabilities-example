package modules

import akka.http.scaladsl.server.{Directives, Route}
import controller.{AkkaLogging, AuthController, PostsController}
import facade.AuthFacadeImpl

class Controllers(services: Services) {

  private val authController = new AuthController(services.authFacade)
  private val postsController = new PostsController(services.postsFacade, services.sessionFacade)

  val httpRoute: Route = AkkaLogging.logDirective {
    Directives.concat(
      authController.route,
      postsController.route
    )
  }
}
