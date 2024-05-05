package modules

import akka.http.scaladsl.server.Route
import facade.{AuthFacade, AuthFacadeImpl}
import routes.AuthRoute

class Routers(services : Services) {

  private val authFacade = new AuthFacadeImpl(services.userService , services.sessionService)
  private val authRoute = new AuthRoute(authFacade)




  val httpRoute : Route = authRoute.route
}
