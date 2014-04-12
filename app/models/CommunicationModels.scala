package models

import play.api.libs.json.JsValue
import java.util.Date
import akka.actor.ActorRef

/* Messages for communication between actors */

case class Report(id: GeoSquare, count: Long)

case object TotalTweets

case object Winner

case object ReportCount

case object Start

case object Ping

case class AddQueries(queries: List[(TweetQuery, ActorRef)])

case object Stop