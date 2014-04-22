package models

class ClustHC(squareCoords: Map[GeoSquare, (Int, Int)], results: Map[GeoSquare, Long], row: Int, col: Int) {
  //TODO play around with this value
  val CORRECTOR = 4
  val total = results.map(_._2).sum
  def totalArea = row * col
  def totalTweetDensity: Double = total.toDouble / totalArea.toDouble

  def originalClusters: Set[Cluster] = {
    squareCoords.map(e => Cluster(Set(LeafCluster(e._2, results(e._1))))).toSet
  }

  def originalLeaf: Set[LeafCluster] = {
    squareCoords.map(e => LeafCluster(e._2, results(e._1))).toSet
  }

   /*@brief  Iterates to obtain 10 different hierarchy of clusters*/
  def clusterResult: List[Set[Cluster]] = {
     var res: List[Set[Cluster]] = (originalClusters)::Nil
     (1 to 5).foreach{ i =>
        println("Fuck it good "+i)
        val beta: Double = i.toFloat / 10.0
        res :+= clusterOnce(res.last, beta * totalArea.toDouble / CORRECTOR, (1.0 - beta * totalTweetDensity))
     }
     res
  }
  
  /*@brief  Iterates at one granularity until no more clusters are formed */
  def clusterOnce(oldC: Set[Cluster], maxArea: Double, threshold: Double): Set[Cluster] = {
   println("FUCK IT ")
   val p = (for(s <- oldC; e <- oldC) yield (aggregate(s, e))).toSet.filter(c => c.area <= maxArea && c.tweetMeter >= threshold)
   val filtered = p.filter(c => !p.exists(c2 => c2.intersect(c) && c2.tweetMeter > c.tweetMeter))
   //TODO check that, I'm not sure
   val res = filtered ++ oldC.filter(c => !filtered.exists(cc => cc.intersect(c)))
   if(res == oldC)
    oldC
   else 
    clusterOnce(res, maxArea, threshold)
    res
  } 
 
  def toGeoSquare(cluster: Cluster): Option[GeoSquare] = {
    if(!squareCoords.values.toList.contains(cluster.topLeft) 
        || !squareCoords.values.toList.contains(cluster.bottomRight)){
      None
    }else {
      val reversed = squareCoords.map{_.swap}
      val top = reversed(cluster.topLeft)
      val bottom = reversed(cluster.bottomRight)
      Some(GeoSquare(top.long1, top.lat1, bottom.long2, bottom.lat2))
    }
  }
  /*@brief  Aggregates two cluster by computing the rectangel area that joins them
            And adds all the LeafClusters contained in this one*/
  def aggregate(c1: Cluster, c2: Cluster): Cluster = {
    val aggreg = Cluster(c1.subClusters ++ c2.subClusters)
    val toAdd = originalLeaf.filter(x => aggreg.contains(x.pos))
    aggreg.subClusters ++= toAdd
    aggreg
  }

  /*@brief  Removes the clusters that are not important significatif
            TODO find a way to define significance*/
  def cleanClusters(clusters: Set[Cluster]): Set[Cluster] = {
    val maxDensity = clusters.maxBy(_.tweetMeter).tweetMeter
    clusters.filter(c => c.tweetMeter >= (maxDensity / CORRECTOR))
  }


}
