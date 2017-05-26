import controllers.{Application, Assets}
import play.api.ApplicationLoader.Context
import play.api._
import play.api.mvc.{Filter}
import play.api.routing.Router
import router.Routes
import com.softwaremill.macwire._

import services.{WeatherService, SunService, AuthService, UserAuthAction}
import filters.StatsFilter
import actors.StatsActor
import actors.StatsActor.Ping

import akka.actor.Props

import scalikejdbc.config.DBs

import scala.concurrent.Future

class AppApplicationLoader extends ApplicationLoader {
  def load(context: Context) = {
    LoggerConfigurator(context.environment.classLoader).
      foreach { configurator =>
        configurator.configure(context.environment)
      }
    /**
    * This is the actual step to initialize the application but
    * all the services instantiation is wired in in AppComponents.
    *
    */
    (new BuiltInComponentsFromContext(context) with AppComponents).application
  }
}

/**
* The AppComponents trait initializes components that our application uses.
* This is basically where DI happens. The wire macro reduces boilerplate by liberating you from instantiating objects manually and passing necessary constructor parameters explicitly. At the same time, the lazy keyword ensures the correct order of initialization.
* It’s important to understand that creating our own application loader means that
* we also need to handle Play’s built-in services.
* For example, the WS will no longer work as a singleton.
* Instead, we will need to use the WSClient type, which is defined in the AhcWSComponents trait.
*/

import play.api.db.evolutions.{DynamicEvolutions, EvolutionsComponents}
import play.api.db.{HikariCPComponents, DBComponents}
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.cache.EhCacheComponents
trait AppComponents extends BuiltInComponents
                       with AhcWSComponents
                       with EvolutionsComponents // EC introduced DynamicEvolutions & DBApi, which are both abstract fileds.
                       with DBComponents // DynamicEvolutions will be instantiated below but DBApi will be instantiated through this DBComponents, which provides the instantiation.
                       with HikariCPComponents  // DBComponents introduced another abstract field, ConnectionPool, whose instance can be found in HikariCPComponents
                       with EhCacheComponents
                       {

  lazy val assets: Assets = wire[Assets]
  lazy val prefix: String = "/"
  lazy val router: Router = wire[Routes]

  // actorSystem is available as a field in BuiltInComponents
  // Props class is used to create an actor
  // now it is ready to receive and react to messages
  lazy val statsActor = actorSystem.actorOf(Props(wire[StatsActor]), StatsActor.name)

  // wire up the filter
  // httpFilters is from BuildInComponents trait
  // filters provided in Seq() will be registered through httpFilters, as well as invoked in chain [after the request came in but before the request goes to Controller]
  lazy val statsFilter : Filter = wire[StatsFilter]
  lazy override val httpFilters = Seq(statsFilter)

  lazy val sunService = wire[SunService]
  lazy val weatherService = wire[WeatherService]
  // since Application takes both services, they have to be instantiated before we can wire Application.
  lazy val applicationController = wire[Application]

  // this belongs to EvolutionsComponents
  // it runs upon instantiation
  lazy val dynamicEvolutions = new DynamicEvolutions

  // this belongs to EhCacheComponents
  // Not Sure Why - "As the wire macro chooses constructor arguments based on their type, it isn’t working here, and we’re instantiating the AuthService manually."
  lazy val authService = new AuthService(defaultCacheApi)
  // in order to pass the UserAuthAction instance to the Application controller, we need to wire[UserAuthAction]
  // again, the wire macro takes care of passing an AuthService to the UserAuthAction contructor.
  lazy val userAuthAction = wire[UserAuthAction]

  // applicationLifecycle.addStopHook allows you to register a callback that will be invoked when the app is about to stop - for example: sbt run -> ctrl + D
  // the callback must return Future[Unit], which means that we could wrap the entire method body in Future and perform the task asynchronously.
  applicationLifecycle.addStopHook { () => {
    Logger.info("The app is about to stop.")
    DBs.closeAll()
    Future.successful(Unit)
    }
  }
  // onStart is NOT "lazy"
  // Both applicationLifecycle and onStart belong to BuiltInComponents
  val onStart = {
    Logger.info("The app is about to start.")
    DBs.setupAll()

    // evolutions will be applied when the following field is resolved.
    // this filed belongs to EvolutionsComponents.
    // Therefore, to get it work, we need to have all those abstract fileds resolved.
    applicationEvolutions

    statsActor ! Ping // Ping imported from actor.StatsActor, sent to statsActor()
  }
}
