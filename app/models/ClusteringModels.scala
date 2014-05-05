package models

/* Models for the HC clustering */

case class Cluster(var subClusters: Set[LeafCluster]) {
  private val (xs, ys) = (subClusters.map(_.pos._1), subClusters.map(_.pos._2))
  val topLeft = (xs.min, ys.min)
  val bottomRight = (xs.max, ys.max)
  
  val area = {
    val longs1 = subClusters.map(_.area.long1)
    val lats1 = subClusters.map(_.area.lat1)
    val longs2 = subClusters.map(_.area.long2)
    val lats2 = subClusters.map(_.area.lat2)
    GeoSquare(longs1.min, lats1.min, longs2.max, lats2.max)
  }
  val center = ((area.long1 + area.long2) / 2, (area.lat1 + area.lat2) / 2)
  
  val numTweets: Long = subClusters.map(_.numTweets).sum
  val size = subClusters.size
  val tweetMeter = numTweets.toFloat / size.toFloat

  def intersect(that: Cluster): Boolean = subClusters.exists(that.subClusters.contains(_))

  def contains(point: (Int, Int)): Boolean =
    topLeft._1 <= point._1 && topLeft._2 <= point._2 && bottomRight._1 >= point._1 && bottomRight._2 >= point._2

  def <(that: Cluster): Boolean = subClusters.size < that.subClusters.size
  def >(that: Cluster): Boolean = subClusters.size > that.subClusters.size

}

case class LeafCluster(pos: (Int, Int), numTweets: Long, area: GeoSquare) 

/* Model for the SLIC clustering */

case class Pixel(pos:(Int, Int), var l: Int,var d: Double)
case class SuperPixel (k: Int, var pos:(Double, Double), var d: Double){
  def getCopy: SuperPixel = SuperPixel(k, pos, d)
}
