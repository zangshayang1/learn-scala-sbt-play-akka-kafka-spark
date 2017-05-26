package services

import play.api.libs.ws.{WS, WSClient}
import play.api.Play.current // introduce an implicit "App" used by WS client. ???

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global // introduce an implicit ExecutionContext, used by Futures. ???

class WeatherService(wsClient : WSClient) { // create a constructor parameter wsClient, checkout AppLoader.scala to see WHY it is necessary.

  def getTemperature(lat : Double, lng : Double) : Future[Double] = {

    val weatherResponseF = wsClient.url( // does the F mean FLOW?
      "http://api.openweathermap.org/data/2.5/"
        + s"weather?lat=$lat&lon=$lng&appid=436acaae36ecba77f7198b22a2fe3f3a&units=metric"
      ).get()

    // why it renders responses this way??? To output a flow???
    weatherResponseF.map { response => {
      val weatherJson = response.json
      val temperature = (weatherJson \ "main" \ "temp").as[Double]
      temperature
      }
    }
  }
}
