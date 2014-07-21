import org.specs2.mutable._
import org.specs2.specification._
import anorm._ 
import java.sql.Connection
import play.api.Application
import play.api.test._
import play.api.db.DB

import models._

object DataStoreSpec extends Specification with PlaySpecification {
  def appWithMemoryDatabase = FakeApplication(additionalConfiguration = inMemoryDatabase("test"))

  val coords1 = List(((0.0, 1.1, 2.2, 3.3), 10, 20), ((0.0, 0.0, 1.0, 1.0), 25, 25))
  val keywords1 = (List("hello", "bonjour"), List("paper", "papier"))
  val coords2 = List(((5.1, 5.2, 5.3, 5.4), 5, 5), ((0.0, 0.0, 1.0, 1.0), 5, 5))
  val keywords2 = (List("#tag", "#hashtag"), List("@user"))

  def withCleanDatabase[A](block: Connection => A)(implicit app: Application) = {
    DB.withConnection { implicit c =>
      SQL"""
        CREATE TABLE  IF NOT EXISTS SESSIONS(ID INT AUTO_INCREMENT PRIMARY KEY, STATE INT);
        CREATE TABLE  IF NOT EXISTS COORDS(ID INT AUTO_INCREMENT PRIMARY KEY, SESSION_ID INT, C1 DOUBLE, C2 DOUBLE, C3 DOUBLE, C4 DOUBLE, ROWS INT, COLS INT);
        CREATE TABLE  IF NOT EXISTS KEYWORDS(ID INT AUTO_INCREMENT PRIMARY KEY, SESSION_ID INT, KEYWORD VARCHAR, GRP INT);
        CREATE TABLE  IF NOT EXISTS TWEETS(ID INT AUTO_INCREMENT PRIMARY KEY, SESSION_ID INT, LONG1 DOUBLE, LAT1 DOUBLE, LONG2 DOUBLE, LAT2 DOUBLE, QUANTITY INT);
      """.execute()
      block(c)
      SQL"""
        DROP TABLE SESSIONS;
        DROP TABLE COORDS;
        DROP TABLE KEYWORDS;
        DROP TABLE TWEETS;
      """.execute()
    }
  }

  def withTwoSessions[A](store: DataStore)(block: Connection => A)(implicit app: Application) = {
    withCleanDatabase { implicit c =>
      store.addSession(1, coords1, keywords1, true)
      store.addSession(2, coords2, keywords2, false)
      block(c)
    }
  }

  "SQL DataStore" should {
    "get the next id without errors" in new WithApplication(appWithMemoryDatabase) {
      val store = new SQLDataStore()
      withCleanDatabase { implicit c =>
        store.getNextId should be equalTo 1
        store.addSession(1, coords1, keywords1, true)
        store.getNextId should be equalTo 2
      }
    }

    "keep track of added sessions" in new WithApplication(appWithMemoryDatabase) {
      val store = new SQLDataStore()
      withTwoSessions(store) { implicit c =>
        store.getSessionInfo(2) should be equalTo (coords2.map(_._1), keywords2, false)
        store.setSessionState(1, false)
        store.getSessionInfo(1) should be equalTo (coords1.map(_._1), keywords1, false)
        store.containsId(3) should beFalse
        store.containsId(2) should beTrue
        store.getNextId should be equalTo(3)
        store.getCoordsInfo(1, 0.0, 1.1, 2.2, 3.3) should be equalTo (10, 20)
        store.getCoordsInfo(2, 0.0, 0.0, 1.0, 1.0) should be equalTo (5, 5)
      }
    }

    "keep track of tweet counts" in new WithApplication(appWithMemoryDatabase) {
      val store = new SQLDataStore()
      withTwoSessions(store) { implicit c =>
        store.increaseSessionTweets(1, 1.0, 1.0, 1.0, 2.0, 5)
        store.increaseSessionTweets(2, 1.3, 1.3, 3.1, 1.0, 2)
        store.getSessionTweets(1)(c)(1.0, 1.0, 1.0, 2.0) should be equalTo 5
        store.getSessionTweets(2)(c)(1.3, 1.3, 3.1, 1.0) should be equalTo 2

        store.increaseSessionTweets(1, 1.0, 1.0, 1.0, 2.0, 4)
        store.increaseSessionTweets(2, 1.3, 1.3, 3.1, 1.0, 3)
        store.getSessionTweets(1)(c)(1.0, 1.0, 1.0, 2.0) should be equalTo 9
        store.getSessionTweets(2)(c)(1.3, 1.3, 3.1, 1.0) should be equalTo 5
      }
    }
  }
}
