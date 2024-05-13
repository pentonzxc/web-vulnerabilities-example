package modules

import akka.http.scaladsl.server.{Directives, Route}
import controller.{AkkaLogging, AuthController, AuthDirectives, PostsController}

class Controllers(services: Services) {

  private val authDirectives = new AuthDirectives(services.sessionFacade)


  private val authController = new AuthController(services.authFacade, services.sessionFacade)
  private val postsController = new PostsController(services.postsFacade, services.sessionFacade, authDirectives)

  val httpRoute: Route = AkkaLogging.logDirective {
    Directives.concat(
      authController.route,
      postsController.route
    )
  }
}
