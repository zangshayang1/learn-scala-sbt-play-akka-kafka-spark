package com.appliedscala.events

import play.api.libs.json.JsValue
/**
A general trait that all kinds of events should extend so that
1. an action name must be provided
2. a serialize method must be defined.
*/
trait EventData {
  def action: String
  def json: JsValue
}
