import jobs._
import models._
import models._
import org.scalatest.FunSuite

class ClusteringTests extends FunSuite {
  val square: GeoSquare = GeoSquare(-129.4,20.0, -79, 50.6) 
  val queries = TweetQuery("prout"::Nil, square, 9, 9).subqueries
  val coords = queries.map(_.area).zipWithIndex.map{x => (x._1, (x._2 % 9, x._2 / 9))}.toMap
  var res = coords.keys.map{ k => 
    val p = coords(k)
    if (p._1 == 8 && p._2 == 8){
      (k, 2L)
    }else if(p._2 < 2 && p._1 < 2){
      (k, 1L)
    }else if(p._2 >= 2 && p._2 < 4 && p._1 < 2){
      (k, 2L)
    }else if (p._2 >= 7 && p._1 < 1){
      (k, 4L)
    }else if (p._2 >=7 && p._1 >=7){
      (k, 3L)
    }else {
      (k, 0L)
    }
  }.toMap 
  val clust: ClustHC = new ClustHC(coords, res, 9, 9)
  val slic: ClusteringLib = new ClusteringLib(coords, res)
 
  test("HC: Should correctly compute the total number of tweets"){
    assert(clust.total == 72, "Wrong number of tweets computed")
  }
  test("HC: Should have the correct density"){
    assert(clust.totalTweetDensity == 72.0 / 81.0, "Wrong density")
  }
  test("HC: Should cluster well !"){
    //val result = clust.clusterResult
    //println(clust.cleanClusters(result.last))
  }
  test("Show me the coords"){
    assert(queries.size == 81)
    assert(coords.size == 81)
  }

  test("Slic: Should have correct total"){
    assert(slic.total == 72, "Slic doesn't have correct total")
  }
  test("Slic: K should be 9"){
    assert(slic.K == 9)
  }
  test("Slic: S should be 3"){
    assert(slic.S == 3)
  }
  test("Slic: Should cluster this shit"){
    println("New:")
    slic.slic_me
    slic.superPixels.foreach{s =>
      println(s)
    }
    println("The pixels:")
    slic.pixels.sortBy(_.pos).foreach{
      println(_)
    }
  }
  
}
