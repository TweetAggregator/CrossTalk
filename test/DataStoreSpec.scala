import org.specs2.mutable._
import org.specs2.specification._
import anorm._ 
import java.sql.Connection
import play.api.Application
import play.api.test._
import play.api.db.DB

import models._

object DataStoreSpec extends Specification with PlaySpecification {
  def appWithMemoryDatabase = FakeApplication(additionalConfiguration = inMemoryDatabase("test") + ("db.default.url" -> "jdbc:h2:mem:play"))

  val coords1 = List((GeoSquare(0.0, 1.1, 2.2, 0.3), 10, 20), (GeoSquare(0.0, 1.0, 1.0, 0.0), 25, 25))
  val keywords1 = (List("hello", "bonjour"), List("paper", "papier"))
  val coords2 = List((GeoSquare(5.1, 5.4, 5.3, 5.2), 5, 5), (GeoSquare(0.0, 1.0, 1.0, 0.0), 5, 5))
  val keywords2 = (List("#tag", "#hashtag"), List("@user"))

  def withCleanDatabase[A](block: Connection => A)(implicit app: Application) = {
    DB.withConnection { implicit c =>
      SQL"""
        create table if not exists users(id int auto_increment, name varchar);
        create table if not exists sessions(id int auto_increment primary key, user_id int, state int);
        create table if not exists coords(id int auto_increment primary key, session_id int, c1 double, c2 double, c3 double, c4 double, rows int, cols int);
        create table if not exists keywords(id int auto_increment primary key, session_id int, keyword varchar, grp int);
        create table if not exists tweets(id int auto_increment primary key, session_id int, long1 double, lat1 double, long2 double, lat2 double, grp int, quantity int);
      """.execute()
      block(c)
      SQL"""
        drop table users;
        drop table sessions;
        drop table coords;
        drop table keywords;
        drop table tweets;
      """.execute()
    }
  }

  def withTwoSessions[A](store: DataStore)(block: (Connection, Long, Long) => A)(implicit app: Application) = {
    withCleanDatabase { implicit c =>
      val i = store.addSession(1, coords1, keywords1, true)
      val j = store.addSession(1, coords2, keywords2, false)
      block(c, i, j)
    }
  }

  "SQL DataStore" should {
    "get the next id without errors" in new WithApplication(appWithMemoryDatabase) {
      val store = new SQLDataStore()
      withCleanDatabase { implicit c =>
        store.addSession(1, coords1, keywords1, true)
      }
    }

    "keep track of added sessions" in new WithApplication(appWithMemoryDatabase) {
      val store = new SQLDataStore()
      withTwoSessions(store) { case (c, i, j) =>
        implicit val conn = c
        store.getSessionInfo(j) should be equalTo SessionInfo(1, coords2.map(_._1), keywords2._1, keywords2._2, false)
        store.setSessionState(i, false)
        store.getSessionInfo(i) should be equalTo SessionInfo(1, coords1.map(_._1), keywords1._1, keywords1._2, false)
        store.containsId(3) should beFalse
        store.containsId(2) should beTrue
        store.getCoordsInfo(i, 0.0, 1.1, 2.2, 0.3) should be equalTo (10, 20)
        store.getCoordsInfo(j, 0.0, 1.0, 1.0, 0.0) should be equalTo (5, 5)
      }
    }

    "keep track of tweet counts" in new WithApplication(appWithMemoryDatabase) {
      val store = new SQLDataStore()
      withTwoSessions(store) { case (c, i, j) =>
        implicit val conn = c 
        store.increaseSessionTweets(i, 1.0, 1.0, 1.0, 2.0, FirstGroup, 5)
        store.increaseSessionTweets(j, 1.3, 1.3, 3.1, 1.0, SecondGroup, 2)
        store.getSessionTweets(i, FirstGroup)(c)(1.0, 1.0, 1.0, 2.0) should be equalTo 5
        store.getSessionTweets(i, SecondGroup)(c).keys should not contain ((1.0, 1.0, 1.0, 2.0))
        store.getSessionTweets(j, SecondGroup)(c)(1.3, 1.3, 3.1, 1.0) should be equalTo 2

        store.increaseSessionTweets(i, 1.0, 1.0, 1.0, 2.0, FirstGroup, 4)
        store.increaseSessionTweets(j, 1.3, 1.3, 3.1, 1.0, SecondGroup, 3)
        store.getSessionTweets(i, FirstGroup)(c)(1.0, 1.0, 1.0, 2.0) should be equalTo 9
        store.getSessionTweets(j, SecondGroup)(c)(1.3, 1.3, 3.1, 1.0) should be equalTo 5
      }
    }

    "keep track of users and their sessions" in new WithApplication(appWithMemoryDatabase) {
      val store = new SQLDataStore()
      DB.withConnection { implicit c =>
        val userId1 = store.addUser("test1")
        val userId2 = store.addUser("test2")
        val session1 = store.addSession(userId1, coords1, keywords1, true)
        val session2 = store.addSession(userId2, coords2, keywords2, true)
        val session3 = store.addSession(userId1, coords2, keywords2, true)

        val userInfo1 = store.getUserInfo("test1")
        val userInfo2 = store.getUserInfo("test2")

        userInfo1.sessions should be equalTo List(session1, session3)
        userInfo2.sessions should be equalTo List(session2)

        val sessionInfo2 = store.getSessionInfo(session2)

        sessionInfo2.coordinates should be equalTo (coords2.map(_._1))
      }
    }
  }
}
