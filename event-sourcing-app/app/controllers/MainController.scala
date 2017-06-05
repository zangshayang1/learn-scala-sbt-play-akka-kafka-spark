package controllers

import java.util.UUID

import play.api.libs.json.JsValue
import play.api.mvc._
import security.{UserAuthAction, UserAwareAction, UserAwareRequest}


class MainController(userAuthAction: UserAuthAction,
                     userAwareAction: UserAwareAction) extends Controller {

  import model.WebPageData
  def index = userAwareAction { request =>
    Ok(views.html.pages.react(buildNavData(request),
      WebPageData("Home")))
  }

  def error500 = Action {
    InternalServerError(views.html.errorPage())
  }

  // take users' request and make a NavigationData() obj out of it.
  import model.NavigationData
  private def buildNavData(request: UserAwareRequest[_]): NavigationData = {
    NavigationData(request.user, isLoggedIn = request.user.isDefined)
  }
}
