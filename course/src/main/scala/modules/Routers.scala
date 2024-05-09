package modules

import akka.http.scaladsl.server.Route
import facade.AuthFacadeImpl
import controller.{AkkaLogging, AuthController}

class Routers(services: Services) {

  private val authFacade = new AuthFacadeImpl(services.userService, services.sessionService)
  private val authRoute = new AuthController(authFacade)

  val httpRoute: Route = AkkaLogging.logDirective {
    authRoute.route
  }
}
