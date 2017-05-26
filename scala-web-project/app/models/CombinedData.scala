package models

import play.api.libs.json.Json


case class CombinedData(sunInfo : SunInfo, temperature : Double, requests : Int)

object CombinedData {
  // no need to teach Play how to serialize temperature and requests
  implicit val writes = Json.writes[CombinedData]
}
