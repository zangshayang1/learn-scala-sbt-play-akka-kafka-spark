package services

import models.User
import play.api.mvc.{Result, Results, Request, WrappedRequest, ActionBuilder} // WrappedRequest reminds me of - import scalikejdbc.WrappedResultSet
import scala.concurrent.Future

// To build our own action - UserAuthAction, we need to extend ActionBuilder.
// ActionBuilder is parametrized with the request type, enabling developers to use their own request type, instead of the standard play.api.mv.Request
// So here we are, wrapping around the original request to create our own request - UserAuthRequest, passed in as follows: ActionBuilder[UserAuthRequest]
case class UserAuthRequest[A](user: User, request: Request[A]) extends WrappedRequest[A](request)
// In addition to adding a new field of type User, we'd like to keep the class parametrized so that it can be used with different HTTP body contents(form, JSON...)

/** UserAuthAction does the following:

  1. check whether the user is authenticated by inspecting the "X-Auth-Token" cookie presented in RequestHeader.
    Other "membership" cookies also exist therefore we found the login authentication cookie via this cookieHeader.
    Different cookies should grant different access authority.

  2. Grant access to restricted resources to authenticated users.
    Redirect others to login page.

  3. Pass the User obj into a controller action.
*/
class UserAuthAction(authService: AuthService) extends ActionBuilder[UserAuthRequest] {

  def invokeBlock[A](request: Request[A],
                    block: (UserAuthRequest[A]) => Future[Result]) : Future[Result] = {

    val maybeUser = authService.checkCookie(request) // check cookies in original request, authService is automatically wired in in AppLoader.scala
    maybeUser match {
      case Some(user) => block(UserAuthRequest(user, request)) // why it is so different from what we defined in Application.doLogin Action?
      case None => Future.successful(Results.Redirect("/login")) // my guess is here we provided a "framework", set rules for input & output. But the actual input & output is provided in Application.restricted Action. That also explains why we need Future[Result].
    }
  }
}
