package clustering

import models._
import utils.Enrichments._

class ClustSlic(pixels: List[Pixel]){
  
  val K: Double = Math.sqrt(pixels.size).toDouble
  val S: Double = Math.sqrt(K)
  val superPixels: List[SuperPixel] = (0 until K.toInt).map(i => SuperPixel(i, (i % S,i / S), 0)).toList.sortBy(_.pos)

  val sigma: Double = getConfDouble("clustSlic.sigma", "ClustSlic: sigma not found")
  val low_threshold: Double = getConfDouble("clustSlic.low_threshold", "ClustSlic: low_threshold not found")
  val alpha: Double = getConfDouble("clustSlic.alpha", "ClustSlic: alpha not found")
  var E: Double = 0.0

  def first_pass {
    superPixels.foreach{ s =>
      val belongs = pixels.filter(p => p.pos._1 % S == s.pos._1.toInt && p.pos._2 / S == s.pos._2.toInt).sortBy(_.pos)
      assert(belongs.size == (K / S).toInt)
      val posCenter = belongs(belongs.size / 2).pos
      val density = belongs.map(_.d).sum
      s.pos = (posCenter._1.toDouble, posCenter._2.toDouble)
      s.d = density
      belongs.foreach{ b =>
        b.d = D(b, s)
        b.l = s.k
      }
    }
  }

  def assignment {
    pixels.foreach{ pix => 
      val (d, k): (Double, Int) = superPixels.map(s => (D(pix, s), s.k)).minBy(_._1)
      if(pix.d > d){
        pix.d = d
        pix.l = k
      }  
    }
    updates

    if(E > low_threshold)
      assignment
  }

 def updates {
  superPixels.foreach{ s =>
    val copy = s.getCopy
    val (xs, ys, ds) = pixels.filter(_.l == s.k).map(p => (p.pos._1, p.pos._2, p.d)).unzip3
    assert(xs.size == ys.size)
    s.pos = (xs.sum.toDouble / xs.size.toDouble, ys.sum.toDouble / ys.size.toDouble)
    s.d = ds.sum / ds.size.toDouble
    val diffx = Math.pow(copy.pos._1 - s.pos._1, 2)
    val diffy = Math.pow(copy.pos._2 - s.pos._2, 2)
    val diffd = Math.pow(copy.d - s.d, 2)
    E += Math.sqrt(diffx + diffy + diffd)
  }
  E = E / K
 }

 def suppression: List[SuperPixel] = {
  superPixels filter (s => pixels.exists(p => p.l == s.k))
 }

  def D(a: Pixel, b: SuperPixel): Double = {
    val ds: Double = Math.sqrt((Math.pow(a.pos._1 - b.pos._1, 2) + Math.pow(a.pos._2 - b.pos._2, 2)).toDouble)
    val dt: Double = Math.abs(a.d - b.d)
    Math.sqrt(Math.pow(dt, 2) + Math.pow(ds, 2) / K * sigma) 
  }

}
