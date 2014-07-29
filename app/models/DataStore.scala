package models

import java.sql.Connection
import scala.collection.immutable.Map
import anorm._ 
import play.api.db.DB
import play.api.Play.current

trait KeywordGroup { def toInt: Int }
case object FirstGroup extends KeywordGroup { def toInt = 1 }
case object SecondGroup extends KeywordGroup { def toInt = 2 }
case object IntersectionGroup extends KeywordGroup { def toInt = 3 }

case class SessionInfo(userId: Long, coordinates: List[GeoSquare], keys1: List[String], keys2: List[String], state: Boolean)
case class UserInfo(id: Long, name: String, sessions: List[Long])

trait DataStore {
  def addUser(name: String)(implicit c: Connection): Long
  def addSession(userId: Long, coordinates: List[(GeoSquare, Int, Int)], keywords: (List[String], List[String]), running: Boolean)(implicit c: Connection): Long

  def getUserInfo(username: String)(implicit c: Connection): UserInfo
  def getSessionInfo(id: Long)(implicit c: Connection): SessionInfo
  def getCoordsInfo(id: Long, long1: Double, lat1: Double, long2: Double, lat2: Double)(implicit c: Connection): (Int, Int)
  def getSessionTweets(id: Long, keywordGroup: KeywordGroup)(implicit c: Connection): Map[(Double, Double, Double, Double), Int]

  def setSessionState(id: Long, running: Boolean)(implicit c: Connection): Boolean
  def increaseSessionTweets(id: Long, long1: Double, lat1: Double, long2: Double, lat2: Double, keywordGroup: KeywordGroup, quantity: Int)(implicit c: Connection): Boolean

  def containsId(id: Long)(implicit c: Connection): Boolean
}

class SQLDataStore extends DataStore {
  DB.withConnection { implicit c =>
    SQL"""
        create table if not exists users(id int auto_increment, name varchar);
        create table if not exists sessions(id int auto_increment primary key, user_id int, state int);
        create table if not exists coords(id int auto_increment primary key, session_id int, c1 double, c2 double, c3 double, c4 double, rows int, cols int);
        create table if not exists keywords(id int auto_increment primary key, session_id int, keyword varchar, grp int);
        create table if not exists tweets(id int auto_increment primary key, session_id int, long1 double, lat1 double, long2 double, lat2 double, grp int, quantity int);
    """.execute()
  }

  def addUser(name: String)(implicit c: Connection): Long = {
    SQL"""
      insert into users(name)
      values ($name)
    """.executeInsert().get
  }
  def addSession(userId: Long, coordinates: List[(GeoSquare, Int, Int)], keywords: (List[String], List[String]), running: Boolean)(implicit c: Connection): Long = {
    val Some(sessionId) = SQL"""
      insert into sessions(user_id, state)
      values ($userId, $running)
    """.executeInsert()
    for ((coord, rows, cols) <- coordinates) {
      SQL"""
        insert into coords(session_id, c1, c2, c3, c4, rows, cols)
        values ($sessionId, ${coord.long1}, ${coord.lat1}, ${coord.long2}, ${coord.lat2}, $rows, $cols)
      """.executeInsert()
    }

    for ((k, g) <- (keywords._1.map(k => (k, 1)) ++ keywords._2.map(k => (k, 2)))) {
      SQL"""
        insert into keywords(session_id, keyword, grp)
        values ($sessionId, $k, $g)
      """.executeInsert()
    }
    sessionId
  }

  def getUserInfo(username: String)(implicit c: Connection): UserInfo = {
    val userId = SQL"""
      select id from users where name = $username
    """().head[Long]("id")

    val sessions = SQL"""
      select id from sessions where user_id = $userId
    """().map(_[Long]("id"))

    UserInfo(userId, username, sessions.toList)
  }
  def getSessionInfo(id: Long)(implicit c: Connection): SessionInfo = {
    val sessionRow = SQL"""
      select user_id, state from sessions where id = $id
    """().head
    val stateInt = sessionRow[Int]("state")
    val running = if (stateInt == 0) false else true

    val coordRows = SQL"""
      select c1, c2, c3, c4 from coords where session_id = $id
    """
    val coords = for (row <- coordRows()) yield {
      GeoSquare(row[Double]("c1"), row[Double]("c2"), row[Double]("c3"), row[Double]("c4"))
    }

    val keywordRows = SQL"""
      select keyword, grp from keywords where session_id = $id
    """
    val (keyTs1, keyTs2) = keywordRows().partition(_[Int]("grp") == 1)
    val keys1 = keyTs1.map(_[String]("keyword")).toList
    val keys2 = keyTs2.map(_[String]("keyword")).toList

    SessionInfo(sessionRow[Long]("user_id"), coords.toList, keys1, keys2, running)
  }
  def getCoordsInfo(id: Long, long1: Double, lat1: Double, long2: Double, lat2: Double)(implicit c: Connection): (Int, Int) = {
    val infoRow = SQL"""
      select rows, cols from coords
      where session_id = $id and c1 = $long1 and c2 = $lat1
      and c3 = $long2 and c4 = $lat2
    """().head
    (infoRow[Int]("rows"), infoRow[Int]("cols"))
  }
  def getSessionTweets(id: Long, keywordGroup: KeywordGroup)(implicit c: Connection): Map[(Double, Double, Double, Double), Int] = {
    val quantityRows = SQL"""
      select long1, lat1, long2, lat2, quantity from tweets
      where session_id = $id and grp = ${keywordGroup.toInt}
    """()
    for ((c, kxyq) <- quantityRows.groupBy(_[Double]("long1"));
         (k, xyq) <- kxyq.groupBy(_[Double]("lat1"));
         (x, yq) <- xyq.groupBy(_[Double]("long2"));
         (y, q) <- yq.groupBy(_[Double]("lat2"))) yield {
      (c, k, x, y) -> q.head[Int]("quantity")
    }
  }

  def setSessionState(id: Long, running: Boolean)(implicit c: Connection): Boolean = {
    val state = if (running) 1 else 0
    SQL"""
      update sessions set state = $running where id = $id
    """.executeUpdate() == 1
  }
  def increaseSessionTweets(id: Long, long1: Double, lat1: Double, long2: Double, lat2: Double, keywordGroup: KeywordGroup, quantity: Int)(implicit c: Connection): Boolean = {
    val oldQuantity = SQL"""
      select quantity from tweets 
      where session_id = $id and long1 = $long1
      and lat1 = $lat1 and long2 = $long2 and lat2 = $lat2
      and grp = ${keywordGroup.toInt}
    """().headOption
    if (oldQuantity.nonEmpty) {
      val newQuantity = oldQuantity.get[Int]("quantity") + quantity

      SQL"""
        update tweets set quantity = $newQuantity
        where session_id = $id and long1 = $long1
        and lat1 = $lat1 and long2 = $long2 and lat2 = $lat2
        and grp = ${keywordGroup.toInt}
      """.executeUpdate() == 1
    }
    else {
      SQL"""
        insert into tweets(session_id, long1, lat1, long2, lat2, grp, quantity)
        values ($id, $long1, $lat1, $long2, $lat2, ${keywordGroup.toInt}, $quantity)
      """.executeInsert().nonEmpty
    }
  }
  def containsId(id: Long)(implicit c: Connection): Boolean = {
    SQL"""
      select 1 from sessions where id = $id
    """().nonEmpty
  }
}

