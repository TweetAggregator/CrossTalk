package models

trait DataStore {
  def addSession(id: Long, coordinates: List[(Double, Double, Double, Double)], keywords: (List[String], List[String]), running: Boolean)
  def getSessionInfo(id: Long): (List[(Double, Double, Double, Double)], (List[String], List[String]), Boolean)
  def setSessionState(id: Long, running: Boolean): Boolean
  def getSessionTweets(id: Long): Unit//TODO
  def containsId(id: Long): Boolean
}

class RealDataStore extends DataStore {
  def addSession(id: Long, coordinates: List[(Double, Double, Double, Double)], keywords: (List[String], List[String]), running: Boolean) = ???
  def getSessionInfo(id: Long): (List[(Double, Double, Double, Double)], (List[String], List[String]), Boolean) = ???
  def setSessionState(id: Long, running: Boolean): Boolean = ???
  def getSessionTweets(id: Long): Unit = ???//TODO
  def containsId(id: Long): Boolean = ???
}

