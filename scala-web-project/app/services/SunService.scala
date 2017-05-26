package services

import play.api.libs.ws.{WS, WSClient}
import play.api.Play.current // introduce an implicit "App" used by WS client. ???

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global // introduce an implicit ExecutionContext, used by Futures. ???
import models.SunInfo

import org.joda.time.{DateTimeZone, DateTime}
import org.joda.time.format.DateTimeFormat


class SunService(wsClient : WSClient) {
  def getSunInfo(lat : Double, lng : Double) : Future[SunInfo] = {

    val sunResponseF = wsClient.url(
      "http://api.sunrise-sunset.org/"
      + s"json?lat=$lat&lng=$lng&formatted=0"
    ).get()

    sunResponseF.map { response => {
      val sunJson = response.json
      val sunriseTimeStr = (sunJson \ "results" \ "sunrise").as[String]
      val sunsetTimeStr = (sunJson \ "results" \ "sunset").as[String]
      val sunriseTime = DateTime.parse(sunriseTimeStr)
      val sunsetTime = DateTime.parse(sunsetTimeStr)
      val formatter = DateTimeFormat.forPattern("HH:mm:ss").withZone(DateTimeZone.forID("America/Los_Angeles"))
      val sunInfo = SunInfo(formatter.print(sunriseTime),
                            formatter.print(sunsetTime))
      sunInfo
      }
    }
  }
}
