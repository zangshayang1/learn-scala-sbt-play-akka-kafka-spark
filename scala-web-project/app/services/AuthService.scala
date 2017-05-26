package services

import models.User
import java.util.UUID
import play.api.mvc.{Cookie, RequestHeader}


// To handle cookie storage, if we create a Map for ourselves, we'd need to take care of cookie expiration as well.
// So we use the following CacheApi, coming from cache library in build.sbt dependencies
// Passed in as a constructor parameter because it needs to stand on top of AuthService so that when service restarts, cookies on client side still work.
import play.api.cache.CacheApi

class AuthService(cacheApi: CacheApi) {

  /** public API - login
    input: user credentials
    output: cookies if the authentication goes through
            else None

    Note:
      two private methods required to exe the logic here: verify & generate cookies
  */
  def login(userCode: String, password: String) : Option[Cookie] = {
    for {
      user <- checkUser(userCode, password)
      cookie <- Some(createCookie(user))
    } yield {
      cookie
    }
  }

  /** private - checkUser
    input: user credentials
    output: corresponding User obj if it goes through
            else None
  */
  import scalikejdbc._ // which exposes DB allowing a number of querying methods.
  private def checkUser(userCode: String, password: String): Option[User] = {
    DB.readOnly { implicit session =>
      val maybeUser = sql"SELECT * FROM users WHERE user_code = $userCode".map(User.fromRS).single().apply()
      // DB.readOnly {} method explained at the bottom of the class.
      // .apply() method does the actual work, taking DBSession as an implicit parameter
      // The above string translates into a prepared statement, which is used to execute the same SQL statements repeatedly with high efficiency.
      // .map() transform data from database rows to domain object - User() through predefined User.fromRS
      // .single() limits the maximum return # == 1.

      maybeUser.flatMap {user =>
        import org.mindrot.jbcrypt.BCrypt // it's not efficient to import it here but just for clarity
        if (BCrypt.checkpw(password, user.password)) {
          Some(user)
        }
        else None
      }
    }
  }

  // instead of generating these every time createCookie() is called, we make it available and final for every AuthService
  import java.security.MessageDigest
  val mda = MessageDigest.getInstance("SHA-512")
  val cookieHeader = "X-Auth-Token"

  /** private - createCookie
   input: User obj
   output: Cookie

   Note:
    if checkUser step goes through, then a cookie must be generated.
    Else, this step will not be executed.
  */
  private def createCookie(user: User) : Cookie = {
    val randomPart = UUID.randomUUID().toString.toUpperCase // why all UpperCase?
    val userPart = user.userId.toString.toUpperCase
    val key = s"$randomPart|$userPart"
    import org.apache.commons.codec.binary.Base64
    val token = Base64.encodeBase64String(mda.digest(key.getBytes)) // use a mda.digest() to make it equal length
    import java.util.concurrent.TimeUnit
    import scala.concurrent.duration.Duration
    val duration = Duration.create(10, TimeUnit.HOURS) // cookies on both server and client side will be removed after 10h
    cacheApi.set(token, user, duration)
    Cookie(cookieHeader, token, maxAge = Some(duration.toSeconds.toInt))
  }

  /** public API - checkCookie

    used in UserAuthAction to allow users to access restricted resources within 10h after login
  */
  def checkCookie(header: RequestHeader) : Option[User] = {
    for {
      cookie <- header.cookies.get(cookieHeader) // from GET request, expose the cookies and find the one with header being "X-Auth-Token"
      user <- cacheApi.get[User](cookie.value) // find the user from cacheApi, by token value of the cookie
    } yield {
      user
      // None will be returned if no match found
    }
  }

  /** def readOnly[A](execution: DBSession => A)(implicit context: CPContext = NoCPContext) : A
    curried function (same thing in StatsFilter.scala) takes the following 2 arguments
    1. a functional block {execution : DBsession => A} defines this READ operation
    2. an implicit connection pool context brought in via scalikejdb
    So the whole checkUser() method is defined by defining the READ operation within an implicit DB context, which I guess is defined in application.conf
  */
}
