import org.scalatestplus.play.PlaySpec
import org.scalatest.mock.MockitoSugar
import org.scalatest.concurrent.ScalaFutures

class ApplicationSpec extends PlaySpec with MockitoSugar with ScalaFutures {

  // silly test but interesting stuff
  // DateTime(0): 1969-12-31T16:00:00.000-08:00
  // DateTime(1): 1969-12-31T16:00:00.001-08:00
  // DateTime(Integer.MAX_VALUE): 1970-01-25T12:31:23.647-08:00
  import org.joda.time.DateTime
  import org.joda.time.format.DateTimeFormat
  "DateTimeFormat" must {
    "return 1969 as the beginning of epoch" in {
      val beginning = new DateTime(0)
      val formattedYear = DateTimeFormat.forPattern("YYYY").print(beginning)
      formattedYear mustBe "1969"
    }
  }

  import play.api.libs.json.Json
  import scala.concurrent.Future
  import services.SunService
  import play.api.libs.ws.{WSResponse, WSRequest, WSClient}
  import org.mockito.Mockito.when
  "SunService" must {
    "retrieve correct sunset and sunrise information" in {
      val wsClientStub = mock[WSClient]
      val wsRequestStub = mock[WSRequest]
      val wsResponseStub = mock[WSResponse]

      // UTC retrieved
      val expectedResponse = """
        {
          "results" : {
            "sunrise" : "2017-05-26T12:43:08+00:00",
            "sunset":"2017-05-27T02:53:04+00:00"
          }
        }
                             """
      val jsResult = Json.parse(expectedResponse)
      val lat = 33.696281
      val lng = -117.735588
      val url = "http://api.sunrise-sunset.org/" + s"json?lat=$lat&lng=$lng&formatted=0"

      when(wsClientStub.url(url)).thenReturn(wsRequestStub)
      when(wsRequestStub.get()).thenReturn(Future.successful(wsResponseStub))
      when(wsResponseStub.json).thenReturn(jsResult)

      val sunService = new SunService(wsClientStub)
      val resultF = sunService.getSunInfo(lat, lng)

      // timeZone adjusted
      whenReady(resultF) { res =>
        res.sunrise mustBe "05:43:08"
        res.sunset mustBe "19:53:04"
      }
    }
  }
}
