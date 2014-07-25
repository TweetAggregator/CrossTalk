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

import controllers._
import models._

object GatheringControllerSpec extends Specification with Mockito with PlaySpecification {
  override implicit def defaultAwaitTimeout: Timeout = 20.seconds

  def getDataStore = {
    val store = mock[DataStore]
    store.containsId(any)(any) returns false
    store
  }

  class TestController(store: DataStore) extends RESTfulGathering(store) with Controller

  "RESTful Gathering controller" should {

    //TODO: this test fails
    "add query to database and notify TweetManager upon start" in new WithApplication {
      val dataStore = getDataStore
      val gathering = new TestController(dataStore)
      val coordinates = List(GeoSquare(-129.4, 20.0, -79.0, 50.6))
      val keywords = (List("Obama"), List("Beer", "biere", "pression"))
      val jsCoords = Json.toJson(coordinates)
      val body = Json.toJson(Map(
        "coordinates" -> jsCoords,
        "keys1" -> Json.toJson(keywords._1),
        "keys2" -> Json.toJson(keywords._2)
      ))

      implicit val request = FakeRequest("POST", "/gathering").withJsonBody(body)
      val result = await(gathering.start()(request).run)
      
      result.header.status must equalTo(OK)
      val id = result.session.get("id")
      id.nonEmpty should beTrue
      there was one(dataStore).addSession(M.eq(id.get.toLong), M.eq(coordinates.map((_, any, any))), M.eq(keywords), M.eq(true))(any)
    }

    "not add query to database if id is already present upon start" in new WithApplication {
      val dataStore = getDataStore
      dataStore.containsId(M.eq(1L))(any) returns true
      dataStore.getNextId(any) returns 1L

      val gathering = new TestController(dataStore)
      val coordinates = List(GeoSquare(-129.4, 20.0, -79.0, 50.6))
      val keywords = (List("Obama"), List("Beer", "biere", "pression"))
      val jsCoords = Json.toJson(coordinates)
      val body = Json.toJson(Map(
        "coordinates" -> jsCoords,
        "keys1" -> Json.toJson(keywords._1),
        "keys2" -> Json.toJson(keywords._2)
      ))

      implicit val request = FakeRequest("POST", "/gathering").withJsonBody(body)
      val result = await(gathering.start()(request).run)

      result.header.status must equalTo(BAD_REQUEST)
      there was no(dataStore).addSession(any, any, any, any)(any)
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

  }
}
