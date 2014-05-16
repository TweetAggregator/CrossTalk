package models

import utils.Enrichments._

/* Models for the HC clustering */
case class Cluster(var subClusters: Set[LeafCluster]) {
  val displayAreaCorrector = getConfDouble("clustHC.displayAreaCorrector", "Cluster: no corrector for the areas")
  private def xsYs: (Set[Int], Set[Int]) = (subClusters.map(_.pos._1), subClusters.map(_.pos._2))
  def topLeft = { 
    val rs = xsYs
  (rs._1.min, rs._2.min)}
  def bottomRight = {
    val rs = xsYs
    (rs._1.max, rs._2.max)}
  
  def area = {
    val longs1 = subClusters.map(_.area.long1)
    val lats1 = subClusters.map(_.area.lat1)
    val longs2 = subClusters.map(_.area.long2)
    val lats2 = subClusters.map(_.area.lat2)
    GeoSquare(longs1.min, lats1.min, longs2.max, lats2.max)
  }
  def center = ((area.long1 + area.long2) / 2, (area.lat1 + area.lat2) / 2)
  
  def numTweets: Long = subClusters.map(_.numTweets).sum
  def size = subClusters.size
  def tweetMeter = numTweets.toFloat / size.toFloat

  def intersect(that: Cluster): Boolean = subClusters.exists(that.subClusters.contains(_))

  def contains(point: (Int, Int)): Boolean =
    topLeft._1 <= point._1 && topLeft._2 <= point._2 && bottomRight._1 >= point._1 && bottomRight._2 >= point._2

  def <(that: Cluster): Boolean = subClusters.size < that.subClusters.size
  def >(that: Cluster): Boolean = subClusters.size > that.subClusters.size

  /** Radius for same are between a circle and the rectangle, used for the clustering. NB : approximation. */
  def radius = Math.sqrt((area.lat2 - area.lat1) * (area.long2 - area.long1) / Math.PI) * displayAreaCorrector

  def computeArea(c2: Cluster): Int = {
    val tops = List(this.topLeft, c2.topLeft)
    val bots = List(this.bottomRight, c2.bottomRight)

    val tx = tops.map(_._1).min; val ty = tops.map(_._2).min
    val bx = bots.map(_._1).max; val by = bots.map(_._2).max

    Math.abs(tx - bx) * Math.abs(ty - by)
  }

  def computeAreaBounds(c2: Cluster): ((Int, Int), (Int, Int)) = {
    val tops = List(this.topLeft, c2.topLeft)
    val bots = List(this.bottomRight, c2.bottomRight)

    val tx = tops.map(_._1).min; val ty = tops.map(_._2).min
    val bx = bots.map(_._1).max; val by = bots.map(_._2).max
    ((tx, ty), (bx, by))
  }
}

case class LeafCluster(pos: (Int, Int), numTweets: Long, area: GeoSquare) 

/* Model for the SLIC clustering */

case class Pixel(pos:(Int, Int), var l: Int,var d: Double)
case class SuperPixel (k: Int, var pos:(Double, Double), var d: Double){
  def getCopy: SuperPixel = SuperPixel(k, pos, d)
}
