import controllers.Assets
import play.api.ApplicationLoader.Context
import play.api._
import play.api.db.{DBComponents, HikariCPComponents}
import play.api.db.evolutions.{DynamicEvolutions, EvolutionsComponents}
import play.api.routing.Router
import router.Routes
import com.softwaremill.macwire._
import controllers._
import dao._
import scalikejdbc.config.DBs
import security.{UserAuthAction, UserAwareAction}
import services._

import scala.concurrent.Future

class AppLoader extends ApplicationLoader {
  def load(context: Context) = {
    LoggerConfigurator(context.environment.classLoader).foreach { configurator =>
      configurator.configure(context.environment)
    }
    (new BuiltInComponentsFromContext(context) with AppComponents).application
  }
}

trait AppComponents extends BuiltInComponents
                      with EvolutionsComponents
                      with DBComponents
                      with HikariCPComponents {
  lazy val assets: Assets = wire[Assets]
  lazy val prefix: String = "/"
  lazy val router: Router = wire[Routes]
  lazy val maybeRouter = Option(router)
  override lazy val httpErrorHandler = wire[ProdErrorHandler]

  lazy val mainController = wire[MainController]
  lazy val authController = wire[AuthController]

  lazy val sessionDao = wire[SessionDao]
  lazy val userDao = wire[UserDao]

  lazy val userService = wire[UserService]
  lazy val authService = wire[AuthService]
  lazy val userAuthAction = wire[UserAuthAction]
  lazy val userAwareAction = wire[UserAwareAction]

  /* We don't need to wire [InMemoryReadDAO] because ...

  The instantiation of TagController requires [UserAuthAction], [ReadService], [TagEventProducer]
  The instantiation of UserAuthAction is done.
  The instantiation of ReadService requires [ActorSystem], [LogDao]. The former I guess comes in along with "import akka.actor.ActorSystem", the latter is done below.

    * InMemoryReadDAO is instantiated in InMemoryReadActor, which is instantiated in ReadService.

  The instantiation of TagEventProducer requires [ActorSystem], [ReadService], [LogDao], which is done along the way.
  */
  lazy val logDao = wire[LogDao]
  lazy val readService = wire[ReadService]
  lazy val tagEventProducer = wire[TagEventProducer]
  lazy val tagController = wire[TagController]

  lazy val tagEventConsumer = wire[TagEventConsumer]
  lazy val logRecordConsumer = wire[LogRecordConsumer]
  lazy val consumerAggregator = wire[ConsumerAggregator]

  override lazy val dynamicEvolutions = new DynamicEvolutions

  applicationLifecycle.addStopHook { () =>
    DBs.closeAll()
    Future.successful(Unit)
  }

  val onStart = {
    DBs.setupAll()
    applicationEvolutions
  }
}
