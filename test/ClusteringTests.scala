import jobs._
import models._
import models._
import org.scalatest.FunSuite

class ClusteringTests extends FunSuite {
  val square: GeoSquare = GeoSquare(-129.4,20.0, -79, 50.6) 
  val queries = TweetQuery("prout"::Nil, square, 100, 100).subqueries
  val coords = queries.map(_.area).zipWithIndex.map{x => (x._1, (x._2 % 100, x._2 / 100))}.toMap
  var res = coords.keys.map{ k => 
    val p = coords(k)
    if(p._2 < 25 && p._1 < 25){
      (k, 20L)
    }else if(p._2 >= 25 && p._1 < 25){
      (k, 10L)
    }else if (p._2 > 75 && p._1 < 25){
      (k, 30L)
    }else if (p._2 > 75 && p._1 > 80){
      (k, 50L)
    }else {
      (k, 1L)
    }
  }.toMap 
  /*val clust: ClustHC = new ClustHC(coords, res, 100, 100)

  test("HC: Should correctly compute the total number of tweets"){
    assert(clust.total == 150600 , "Wrong number of tweets computed")
  }
  test("HC: Should have the correct density"){
    assert(clust.totalTweetDensity == 15.06, "Wrong density")
  }
  test("HC: Should cluster well !"){
    val result = clust.clusterResult
    println(clust.cleanClusters(result.last) map (x => x.numTweets))
  }*/
  
}
