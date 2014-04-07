package models

import play.api.libs.json.JsValue
import java.util.Date
import akka.actor.ActorRef

// Messages for communication between actors

case class Report(id: (Int, Int), count: Long)

case object TotalTweets

case object Winner

case object ReportCount
