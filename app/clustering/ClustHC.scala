package clustering

import models._
import utils.Enrichments._

/**
 * Home Made clustering, based on a hiearchy of cluster of 1 to 10.
 * i.e there is up to 10 level of clustering.
 */
class ClustHC(leaves: List[LeafCluster], rows: Int, cols: Int) {

  /* TODO: find the better constants, see application.conf */
  val areaCorrector = getConfDouble("clustHC.areaCorrector", "ClustHC: areaCorrector constant not defined in conf.")
  val thresholdCorrector = getConfDouble("clustHC.thresholdCorrector", "ClustHC: thresholdCorrector constant not defined in conf.")
  val minDensityCorrector = getConfDouble("clustHC.minDensityCorrector", "ClustHC: minDensityCorrector constant not defined in conf.")
  
  val total = leaves.map(_.numTweets).sum
  val totalArea = rows * cols
  val totalTweetDensity: Double = total.toDouble / totalArea.toDouble

  val originalClusters: Set[Cluster] = leaves.map(l => Cluster(Set(l))) toSet

  /**
   *  @brief  Iterates to obtain 10 different hierarchy of clusters
   */
  def compute: List[Set[Cluster]] = {
    var res: List[Set[Cluster]] = (originalClusters) :: Nil
    (1 to 10).foreach { i =>
      val beta = i.toDouble / 10.0
      res :+= clusterOnce(res.last, beta * totalArea.toDouble * areaCorrector, (beta  * totalTweetDensity) * thresholdCorrector)
    }
    res.map(clst => cleanClusters(clst))
  }

  /** 
   *  @brief  Iterates at one granularity until no more clusters are formed 
   */
  private def clusterOnce(oldC: Set[Cluster], maxArea: Double, threshold: Double): Set[Cluster] = {
    println(s"Cluster Once entering for ${maxArea}")
    var lst = oldC.toList //TODO should put some order here
    println(s"The old ${oldC}")
    val couples = (for(i <- 0 until lst.size; j <- i + 1 until lst.size if (lst(i).computeArea(lst(j)) <= maxArea))yield(i,j)).toList.groupBy(_._1)
    val map: Map[Int, List[Int]] = couples.map(e => (e._1, e._2.map(_._2).sorted)).filter(_._2 == Nil)
    val p: Set[Cluster] = map.map(entry => findSweetSpot(entry._1, entry._2, lst,threshold)).filter(_ != None).map(_.get).toSet

    /*val p = (for (i <- 0 until lst.size; j <- i + 1 until lst.size if (lst(i).computeArea(lst(j)) <= maxArea))
      yield (aggregate(lst(i), lst(j)))).toSet.filter(c => c.size <= maxArea && c.tweetMeter >= threshold)*/
    println("Before the filter")
    val filtered = p.filter(c1 => !p.exists(c2 => c2.intersect(c1) && c2.tweetMeter > c1.tweetMeter))
    println("After the filter")
    val res = filtered ++ oldC.filter(l => !filtered.exists(c => c.intersect(l)))
    println("Cluster Once end")
    if (res == oldC) oldC
    else clusterOnce(res, maxArea, threshold)
  }
 
  /*Finds the biggest aggregation possible*/
  def findSweetSpot(key: Int, values: List[Int], lst: List[Cluster], threshold: Double): Option[Cluster] = {
    val sorted = (values.sorted).reverse
    for(j <- sorted ){
      val aggr = aggregate(lst(key), lst(j))
      if (aggr.size >= threshold)
        return Some(aggr)
    }
    None
  }
  /**
   * @brief  Aggregates two cluster by computing the rectangel area that joins them
   *  And adds all the LeafClusters contained in this one
   */
  private def aggregate(c1: Cluster, c2: Cluster): Cluster = {
    val aggreg = Cluster(c1.subClusters ++ c2.subClusters)
    Cluster(aggreg.subClusters ++ leaves.filter(x => aggreg.contains(x.pos)))
  }

  /**
   * @brief  Removes the clusters that are not important significatif
   *   TODO find a way to define significance
   */
  private def cleanClusters(clusters: Set[Cluster]): Set[Cluster] = {
    val maxDensity = if(!clusters.isEmpty) clusters.maxBy(_.tweetMeter).tweetMeter else 0
    clusters.filter(c => c.tweetMeter >= (maxDensity * minDensityCorrector))
  }
}
