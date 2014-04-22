package models

import play.api.libs.json.JsValue
import java.util.Date
import akka.actor.ActorRef

/* Classes and object modeling messages passed between actors. */

/* Messages for communication with the GeoPartionner and the Counter */

case class Report(id: GeoSquare, count: Long)
case object TotalTweets
case object Winner
case object ReportCount
case object StartGeo
case object Collect
case object Opacities

/* Messages for communication with the TweetManager, the TweetSearcher and TweetStreamer */

case object Start
case object Ping
case class AddQueries(queries: List[(TweetQuery, ActorRef)])
case object Stop
case object Pause
case object Resume
case object Cleanup
case object Refused
case object Wait