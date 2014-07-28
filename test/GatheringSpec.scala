import org.specs2.mutable._
import org.specs2.runner._
import org.specs2.mock._
import org.specs2.mock.Mockito
import org.mockito.{ Matchers => M }

import scala.concurrent.duration._
import play.api._
import play.api.http.Status
import play.api.mvc._
import play.api.mvc.Results._
import play.api.test._
import play.api.data._
import play.api.data.Forms._
import play.api.test._
import play.api.libs.json._
import akka.util.Timeout
import akka.actor.ScalaActorRef

import controllers._
import models._

object GatheringControllerSpec extends Specification with Mockito with PlaySpecification {
  override implicit def defaultAwaitTimeout: Timeout = 20.seconds

  def getDataStore = {
    val store = mock[DataStore]
    store.containsId(any)(any) returns false
    store
  }

  def makeRequest(keys1: List[String], keys2: List[String], coordinates: List[(Double, Double, Double, Double)]) = {
    val jsCoords = Json.toJson(coordinates.map(new GeoSquare(_)))
    val body = Json.toJson(Map(
      "coordinates" -> jsCoords,
      "keys1" -> Json.toJson(keys1),
      "keys2" -> Json.toJson(keys2)
    ))

    FakeRequest(
      POST,
      "/gathering",
      FakeHeaders(Seq(CONTENT_TYPE->Seq("application/pdf"))),
      body
    )
  }

  class TestController(store: DataStore) extends Gathering(store, (_, _) => mock[ScalaActorRef]) with Controller

  "Gathering controller" should {

    "add query to database and notify TweetManager upon start" in new WithApplication {
      val dataStore = getDataStore
      val gathering = new TestController(dataStore)
      implicit val request = makeRequest(List("Obama"),
                                List("Beer", "biere", "pression"),
                                List((-129.4, 50.6, -79.0, 20.0)))

      val result = await(gathering.start()(request))
      
      result.header.status must equalTo(OK)
      val id = result.session.get("id")
      id.nonEmpty should beTrue
      there was one(dataStore).addSession(any, any, any)(any)
    }

    "set the session state upon update" in new WithApplication {
      val dataStore = getDataStore
      dataStore.containsId(M.eq(1L))(any) returns true
      dataStore.getNextId(any) returns 1L
      val gathering = new TestController(dataStore)
      val request = FakeRequest()

      val result1 = await(gathering.update(1, false)(request))
      there was one(dataStore).setSessionState(M.eq(1L), M.eq(false))(any)
      result1.header.status must equalTo(303)

      val result2 = await(gathering.update(1, true)(request))
      there was one(dataStore).setSessionState(M.eq(1L), M.eq(true))(any)
      result2.header.status must equalTo(303)
    }

    "not set the session upon update to an invalid id" in new WithApplication {
      val dataStore = getDataStore
      val gathering = new TestController(dataStore)
      val request = FakeRequest()

      val result = await(gathering.update(1, false)(request))
      there was no(dataStore).setSessionState(any, any)(any)
      result.header.status must equalTo(BAD_REQUEST)
    }

    "not add a new session if the coordinates are overlapping" in new WithApplication {
      val dataStore = getDataStore
      val gathering = new TestController(dataStore)

      implicit val request = makeRequest(List("Obama"),
        List("Beer", "biere", "pression"),
        List((-129.4, 50.6, -79.0, 20.0), (-100.0, 60.0, -50.0, 30.0))
      )

      val result = await(gathering.start()(request))
      result.header.status must equalTo(BAD_REQUEST)
    }

    "not add a new session if the coordinates are empty" in new WithApplication {
      val dataStore = getDataStore
      val gathering = new TestController(dataStore)

      implicit val request = makeRequest(List("Obama"),
        List("Beer", "biere", "pression"),
        List()
      )

      val result = await(gathering.start()(request))
      result.header.status must equalTo(BAD_REQUEST)
    }

    "not add a new session if either of the keywords are empty" in new WithApplication {
      val dataStore = getDataStore
      val gathering = new TestController(dataStore)

      implicit val request = makeRequest(List("Obama"),
                                List(),
                                List((-129.4, 50.6, -79.0, 20.0)))

      val result = await(gathering.start()(request))
      result.header.status must equalTo(BAD_REQUEST)
    }

    "not add a new session if a keyword appears in both groups" in new WithApplication {
      val dataStore = getDataStore
      val gathering = new TestController(dataStore)

      implicit val request = makeRequest(List("Obama", "Green"),
                                List("Beer", "Obama"),
                                List((-129.4, 50.6, -79.0, 20.0)))

      val result = await(gathering.start()(request))
      result.header.status must equalTo(BAD_REQUEST)
    }

    "not add a new session if a group contains duplicate keywords" in new WithApplication {
      val dataStore = getDataStore
      val gathering = new TestController(dataStore)

      implicit val request = makeRequest(List("Obama", "Obama"),
                                List("Beer"),
                                List((-129.4, 50.6, -79.0, 20.0)))

      val result = await(gathering.start()(request))
      result.header.status must equalTo(BAD_REQUEST)
    }

  }
}
