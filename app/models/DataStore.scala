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

trait DataStore {
  def addSession(id: Long, coordinates: List[((Double, Double, Double, Double), Int, Int)], keywords: (List[String], List[String]), running: Boolean)(implicit c: Connection)
  def getSessionInfo(id: Long)(implicit c: Connection): (List[(Double, Double, Double, Double)], (List[String], List[String]), Boolean)
  def getCoordsInfo(id: Long, long1: Double, lat1: Double, long2: Double, lat2: Double)(implicit c: Connection): (Int, Int)
  def setSessionState(id: Long, running: Boolean)(implicit c: Connection): Boolean
  def getSessionTweets(id: Long, keywordGroup: KeywordGroup)(implicit c: Connection): Map[(Double, Double, Double, Double), Int]
  def increaseSessionTweets(id: Long, long1: Double, lat1: Double, long2: Double, lat2: Double, keywordGroup: KeywordGroup, quantity: Int)(implicit c: Connection): Boolean
  def containsId(id: Long)(implicit c: Connection): Boolean
  def getNextId(implicit c: Connection): Long
}

class SQLDataStore extends DataStore {
  DB.withConnection { implicit c =>
    SQL"""
        create table if not exists sessions(id int auto_increment primary key, state int);
        create table if not exists coords(id int auto_increment primary key, session_id int, c1 double, c2 double, c3 double, c4 double, rows int, cols int);
        create table if not exists keywords(id int auto_increment primary key, session_id int, keyword varchar, grp int);
        create table if not exists tweets(id int auto_increment primary key, session_id int, long1 double, lat1 double, long2 double, lat2 double, grp int, quantity int);
    """.execute()
  }

  def addSession(id: Long, coordinates: List[((Double, Double, Double, Double), Int, Int)], keywords: (List[String], List[String]), running: Boolean)(implicit c: Connection) = {
    val Some(sessionId) = SQL"""
      insert into sessions(state)
      values ($running)
    """.executeInsert()
    for ((coord, rows, cols) <- coordinates) {
      SQL"""
        insert into coords(session_id, c1, c2, c3, c4, rows, cols)
        values ($sessionId, ${coord._1}, ${coord._2}, ${coord._3}, ${coord._4}, $rows, $cols)
      """.executeInsert()
    }

    for ((k, g) <- (keywords._1.map(k => (k, 1)) ++ keywords._2.map(k => (k, 2)))) {
      SQL"""
        insert into keywords(session_id, keyword, grp)
        values ($sessionId, $k, $g)
      """.executeInsert()
    }
  }
  def getSessionInfo(id: Long)(implicit c: Connection): (List[(Double, Double, Double, Double)], (List[String], List[String]), Boolean) = {
    val stateInt = SQL"""
      select state from sessions where id = $id
    """().head[Int]("state")
    val running = if (stateInt == 0) false else true

    val coordRows = SQL"""
      select c1, c2, c3, c4 from coords where session_id = $id
    """
    val coords = for (row <- coordRows()) yield {
      (row[Double]("c1"), row[Double]("c2"), row[Double]("c3"), row[Double]("c4"))
    }

    val keywordRows = SQL"""
      select keyword, grp from keywords where session_id = $id
    """
    val (keyTs1, keyTs2) = keywordRows().partition(_[Int]("grp") == 1)
    val keys1 = keyTs1.map(_[String]("keyword")).toList
    val keys2 = keyTs2.map(_[String]("keyword")).toList

    (coords.toList, (keys1, keys2), running)
  }
  def getCoordsInfo(id: Long, long1: Double, lat1: Double, long2: Double, lat2: Double)(implicit c: Connection): (Int, Int) = {
    val infoRow = SQL"""
      select rows, cols from coords
      where session_id = $id and c1 = $long1 and c2 = $lat1
      and c3 = $long2 and c4 = $lat2
    """().head
    (infoRow[Int]("rows"), infoRow[Int]("cols"))
  }
  def setSessionState(id: Long, running: Boolean)(implicit c: Connection): Boolean = {
    val state = if (running) 1 else 0
    SQL"""
      update sessions set state = $running where id = $id
    """.executeUpdate() == 1
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
  def getNextId(implicit c: Connection) = {
    SQL"""
      select max(id) from sessions
    """().head[Option[Long]]("max(id)").map(_+1).getOrElse(1L)
  }
}

