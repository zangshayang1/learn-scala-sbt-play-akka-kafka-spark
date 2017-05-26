package controllers

import play.api._
import play.api.mvc.{Controller, Action}

import services.{WeatherService, SunService, AuthService, UserAuthAction}
import models.{SunInfo, CombinedData, UserLoginData} // note: it is the actual class that's specified here rather than module names, which might be unnecessary.

import scala.concurrent.ExecutionContext.Implicits.global // introduce an implicit ExecutionContext, used by Futures. ???

import java.util.concurrent.TimeUnit
import akka.util.Timeout
import akka.pattern.ask
import akka.actor.ActorSystem
import actors.StatsActor

class Application(
  sunService : SunService,
  weatherService : WeatherService,
  actorSystem: ActorSystem,
  authService: AuthService,
  userAuthAction: UserAuthAction
  ) extends Controller {

  def index = Action.async {

    val lat = 33.696281
    val lng = -117.735588

    /** This is to wait 5s for responses from actor.StatsActor() before "timeout" the webpage.
    * the ask pattern must be imported separately from akka.pattern
    * an implicit value specifying timeout must be in scope
    * the ? returns an untyped Future[Any] that must be manually cast with mapTo
    */
    implicit val timeout = Timeout(5, TimeUnit.SECONDS)
    // send statsActor() a "GetStats" msg and map the output to Int
    // I guess the "?" is the core of this "ASK" pattern because it sent msg to ask for return value, unlike the other two msgs.
    val requestsF = (actorSystem.actorSelection(StatsActor.path) ? StatsActor.GetStats).mapTo[Int]

    for {
      temperature <- weatherService.getTemperature(lat, lng)
      sunInfo <- sunService.getSunInfo(lat, lng)
      request <- requestsF
    } yield {
      Ok(views.html.index(sunInfo, temperature, request)) // notorious lowercase "k"
    }
  }

  def data = Action.async {
    import play.api.libs.json.Json

    val lat = 33.696281
    val lng = -117.735588

    /** This is to wait 5s for responses from actor.StatsActor() before "timeout" the webpage.
    * the ask pattern must be imported separately from akka.pattern
    * an implicit value specifying timeout must be in scope
    * the ? returns an untyped Future[Any] that must be manually cast with mapTo
    */
    implicit val timeout = Timeout(5, TimeUnit.SECONDS)
    // send statsActor() a "GetStats" msg and map the output to Int
    // I guess the "?" is the core of this "ASK" pattern because it sent msg to ask for return value, unlike the other two msgs.
    val requestsF = (actorSystem.actorSelection(StatsActor.path) ? StatsActor.GetStats).mapTo[Int]

    for {
      temperature <- weatherService.getTemperature(lat, lng)
      sunInfo <- sunService.getSunInfo(lat, lng)
      request <- requestsF
    } yield {
      // finally, this is how you send json, like a RESTful API
      Ok(Json.toJson(CombinedData(sunInfo, temperature, request))) // notorious lowercase "k"
    }
  }

  // display login page on GET
  def login = Action {
    Ok(views.html.login(None)) // None became necessary when @maybeErrorMessage is provided in login.scala.html
  }

  import play.api.data.Form
  import play.api.data.Forms._
  val userDataForm = Form {
    mapping(
      "username" -> nonEmptyText,
      "password" -> nonEmptyText
    )(UserLoginData.apply)(UserLoginData.unapply)
  }

  // where do "parse" & "cookie" come from ???
  def doLogin = Action(parse.anyContent) { implicit request =>
    userDataForm.bindFromRequest.fold(
      // formWithErrors => BadRequest, BadRequest goes 400, just a blank page, which should be avoided in app.
      formWithErrors => Ok(views.html.login(Some("Wrong Input"))),
      userData => {
        val maybeCookie = authService.login(userData.username, userData.password)
        maybeCookie match {
          case Some(cookie) => Redirect("/").withCookies(cookie)
          case None => Ok(views.html.login(Some("Login Credentials Failed.")))
        }
      }
    )
  }

  // create my own "Action", defined in services/UserAuthAction.scala
  // so that when users who already loggedin within 10h want to access restricted resources, they don't need to login again.
  // userAuthAction is provisioned in AppLoader.scala, but not sure where does "userAuthRequest" come from.
  def restricted = userAuthAction { userAuthRequest =>
    Ok(views.html.restricted(userAuthRequest.user))
  }
}
