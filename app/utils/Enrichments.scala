package utils

import scala.util.Random
import play.api.Play
import models._
/**
 * Add some functionalities to existing classes, such as list. Is mainly
 * used for the TweetManager.
 */
object Enrichments {

  /** Enrich a list with some functionalities */
  implicit class RichList[T](lst: List[T]) {

    /** Shuffle the list */
    def shuffle: List[T] = {
      val rds = new Random(System.currentTimeMillis)
      lst.map(entry => (entry, rds.nextInt)).sortBy(x => x._2).map(entry => entry._1)
    }

    /** Split the list into 'nb' parts of equal size (or a bit less depending of the size of the list) */
    def split(nb: Int) = {
      val partSize = Math.ceil(lst.size / nb.toDouble).toInt
      def loop(lst: List[T], out: List[List[T]]): List[List[T]] = lst match {
        case Nil => out
        case _ => loop(lst.drop(partSize), out :+ lst.take(partSize))
      }
      loop(lst, Nil)
    }
  }

  /**
   * get a configuration value
   *  @param key		The key to get
   *  @param error		The error message to return in case the configuration is not found
   *
   *  @return the string corresponding to the key
   */
  def getConfString(key: String, error: String) =
    Play.current.configuration.getString(key).getOrElse(sys.error(error))

  /**
   * get a configuration value
   *  @param key		The key to get
   *  @param error		The error message to return in case the configuration is not found
   *
   *  @return the Int corresponding to the key
   */
  def getConfInt(key: String, error: String) =
    Play.current.configuration.getInt(key).getOrElse(sys.error(error))

  /**
   * get a configuration value
   *  @param key		The key to get
   *  @param error		The error message to return in case the configuration is not found
   *
   *  @return the Double corresponding to the key
   */
  def getConfDouble(key: String, error: String) =
    Play.current.configuration.getDouble(key).getOrElse(sys.error(error))

  /**
   * get a configuration value
   *  @param key		The key to get
   *  @param error		The error message to return in case the configuration is not found
   *
   *  @return the Boolean corresponding to the key
   */
  def getConfBoolean(key: String, error: String) =
    Play.current.configuration.getBoolean(key).getOrElse(sys.error(error))
  /**
   * Multistep counter. The inner counter must be increased by 'step' in order to change
   * the output.
   * @param step		The step of the multistep counter.
   */
  case class MultiCounter(step: Int) {
    var inCount = 0
    var outCount = 0
    /** @return the next count */
    def incr: Int = if ((inCount + 1) % step == 0) {
      val ret = outCount
      outCount = outCount + 1
      inCount = 0
      ret
    } else {
      inCount = inCount + 1
      outCount
    }
  }

  /** Enrich a list of cluster (HC) to return some JSon */
  implicit class RichClusterList(lst: List[Set[Cluster]]) {
    def toJson = {
      def setToJson(set: Set[Cluster]): String = {
        def clustToJson(clust: Cluster) = s"""{"x": ${clust.center._2},"y": ${clust.center._1}, "r": ${clust.center._2 + clust.radius}, "d": ${clust.tweetMeter}}"""
        if(!set.isEmpty) s"""{"centers": [${set.tail.foldLeft(clustToJson(set.head))((acc, s) => acc + "," + clustToJson(s))}]}"""
        else s"""{"centers": [{}]}"""
      }
      s"""{"clusters": [${lst.tail.foldLeft(setToJson(lst.head))((acc, s) => acc + ", " + setToJson(s))}]}"""
    }
  }
  /** Enrich a list of superPixels (Slic) to return some JSon */
  implicit class RichPixelList(lst: List[SuperPixel]) {
    def toJson = {
      def pixelToJson(pix: SuperPixel) = s"""{"x": ${pix.pos._1}, "y": ${pix.pos._2}, "r": ${pix.d}}"""
      s"""{"clusters": [{"centers": [${lst.tail.foldLeft(pixelToJson(lst.head))((acc, s) => acc + ", " + pixelToJson(s))}]}]}"""
    }

  }
}
