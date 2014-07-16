import org.specs2.mutable._
import org.specs2.runner._
import org.specs2.mock._
import org.specs2.mock.Mockito
import org.mockito.Matchers._

import scala.concurrent.duration._
import play.api._
import play.api.http.Status
import play.api.mvc._
import play.api.mvc.Results._
import play.api.test._
import play.api.data._
import play.api.data.Forms._
import play.api.cache.Cache
import play.api.test._
import akka.util.Timeout

import controllers._
import models._

object GatheringControllerSpec extends Specification with Mockito with PlaySpecification {
  override implicit def defaultAwaitTimeout: Timeout = 20.seconds

  def getDataStore = {
    val store = mock[DataStore]
    store.containsId(any) returns false
    store
  }

  class TestController(store: DataStore) extends RESTfulGathering(store) with Controller

  "RESTful Gathering controller" should {

    "add query to database and notify TweetManager upon start" in new WithApplication {
      val dataStore = getDataStore
      val gathering = new TestController(dataStore)
      val coordinates = List((-129.4, 20.0, -79.0, 50.6))
      val keywords = (List("Obama"), List("Beer", "biere", "pression"))
      implicit val request = FakeRequest()
      val result = await(gathering.start(coordinates, keywords)(request))
      
      result.header.status must equalTo(OK)
      val id = result.session.get("id")
      id.nonEmpty should beTrue
      there was one(dataStore).addSession(id.get.toLong, coordinates, keywords, Running)
    }

    "not add query to database if id is already present upon start" in new WithApplication {
      val dataStore = getDataStore
      dataStore.containsId(1) returns true

      val gathering = new TestController(dataStore)
      val coordinates = List((-129.4, 20.0, -79.0, 50.6))
      val keywords = (List("Obama"), List("Beer", "biere", "pression"))
      val request = FakeRequest().withSession("id" -> "1")
      val result = await(gathering.start(coordinates, keywords)(request))

      result.header.status must equalTo(OK)
      there was no(dataStore).addSession(any, any, any, any)
    }

    "set the session state upon update" in new WithApplication {
      val dataStore = getDataStore
      dataStore.containsId(1) returns true
      val gathering = new TestController(dataStore)
      val request = FakeRequest().withSession("id" -> "1")

      val result1 = await(gathering.update(1, Paused)(request))
      there was one(dataStore).setSessionState(1, Paused)
      result1.header.status must equalTo(OK)

      val result2 = await(gathering.update(1, Running)(request))
      there was one(dataStore).setSessionState(1, Running)
      result2.header.status must equalTo(OK)
    }

    "not set the session upon update to an invalid id" in new WithApplication {
      val dataStore = getDataStore
      val gathering = new TestController(dataStore)
      val request = FakeRequest().withSession("id" -> "1")

      val result = await(gathering.update(1, Paused)(request))
      there was no(dataStore).setSessionState(any, any)
      result.header.status must equalTo(OK)
    }

  }
}
