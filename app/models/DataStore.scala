package models

import java.sql.Connection
import anorm._ 

trait DataStore {
  def addSession(id: Long, coordinates: List[(Double, Double, Double, Double)], keywords: (List[String], List[String]), running: Boolean)(implicit c: Connection)
  def getSessionInfo(id: Long)(implicit c: Connection): (List[(Double, Double, Double, Double)], (List[String], List[String]), Boolean)
  def setSessionState(id: Long, running: Boolean)(implicit c: Connection): Boolean
  def getSessionTweets(id: Long)(implicit c: Connection): Unit//TODO
  def containsId(id: Long)(implicit c: Connection): Boolean
}

class SQLDataStore extends DataStore {
  //TODO: sort out id
  def addSession(id: Long, coordinates: List[(Double, Double, Double, Double)], keywords: (List[String], List[String]), running: Boolean)(implicit c: Connection) = {
    val Some(sessionId) = SQL"""
      insert into sessions(state)
      values ($running)
    """.executeInsert()
    for (coord <- coordinates) {
      SQL"""
        insert into coords(session_id, c1, c2, c3, c4)
        values ($sessionId, ${coord._1}, ${coord._2}, ${coord._3}, ${coord._4})
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
    val state = if (stateInt == 0) false else true

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

    (coords.toList, (keys1, keys2), state)
  }
  def setSessionState(id: Long, running: Boolean)(implicit c: Connection): Boolean = {
    val state = if (running) 1 else 0
    SQL"""
      update sessions set state = $running where id = $id
    """.executeUpdate() == 1
  }
  def getSessionTweets(id: Long)(implicit c: Connection): Unit = ???//TODO
  def containsId(id: Long)(implicit c: Connection): Boolean = {
    SQL"""
      select 1 from sessions where id = $id
    """().nonEmpty
  }
}

