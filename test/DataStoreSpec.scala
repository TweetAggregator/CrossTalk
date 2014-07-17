import org.specs2.mutable._
import org.specs2.specification._
import anorm._ 
import java.sql.Connection
import play.api.Application
import play.api.test._
import play.api.db.DB

import models.SQLDataStore

object DataStoreSpec extends Specification with PlaySpecification {
  val appWithMemoryDatabase = FakeApplication(additionalConfiguration = inMemoryDatabase("test"))

  def withCleanDatabase[A](block: Connection => A)(implicit app: Application) = {
    DB.withConnection { implicit c =>
      SQL"""
        CREATE TABLE SESSIONS(ID INT AUTO_INCREMENT PRIMARY KEY, STATE INT);
        CREATE TABLE COORDS(ID INT AUTO_INCREMENT PRIMARY KEY, SESSION_ID INT, C1 DOUBLE, C2 DOUBLE, C3 DOUBLE, C4 DOUBLE);
        CREATE TABLE KEYWORDS(ID INT AUTO_INCREMENT PRIMARY KEY, SESSION_ID INT, KEYWORD VARCHAR, GRP INT);
        CREATE TABLE TWEETS(ID INT AUTO_INCREMENT PRIMARY KEY, SESSION_ID INT, COORD_ID INT, KEYWORD_ID INT, X INT, Y INT, QUANTITY INT);
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

  "SQL DataStore" should {
    "keep track of added sessions" in new WithApplication(appWithMemoryDatabase) {
      val store = new SQLDataStore()
      withCleanDatabase { implicit c =>
        val coords1 = List((0.0, 1.1, 2.2, 3.3), (0.0, 0.0, 1.0, 1.0))
        val keywords1 = (List("hello", "bonjour"), List("paper", "papier"))
        store.addSession(1,
                         coords1,
                         keywords1,
                         true)
        val coords2 = List((5.1, 5.2, 5.3, 5.4), (0.0, 0.0, 1.0, 1.0))
        val keywords2 = (List("#tag", "#hashtag"), List("@user"))
        store.addSession(2,
                         coords2,
                         keywords2,
                         false)
        store.getSessionInfo(2) should be equalTo (coords2, keywords2, false)
        store.setSessionState(1, false)
        store.getSessionInfo(1) should be equalTo (coords1, keywords1, false)
        store.containsId(3) should beFalse
        store.containsId(2) should beTrue
      } 
    }
  }
}
