import models._

object ClusteringLib {
  //TODO add the home Cooked clustering algorithm here
  //TODO clean up this shit
  //Implementation of Slic algorithm
  var K: Long = 0L
  var pixels: List[Pixel] = Nil
  var superPixels: List[SuperPixel] = Nil
  //TODO determine a correct value
  val SIGMA: Double = 1/2
  val LOW_THRESHOLD = 100 // TODO find that
  val ALPHA: Double = 20 //TODO find that
  var total = 0L
  var E: Double = 0

  def slic_init(squareCoords: Map[GeoSquare, (Int,Int)], densities: Map[GeoSquare, Long]){
    K = Math.sqrt(squareCoords.size).toLong
    total = densities.map(_._2).sum
    pixels = squareCoords.map(e => Pixel(e._2, -1, -1)).toList
    val S: Int = Math.sqrt(K).toInt
    var inter: List[((Int, Int), Double)] = Nil
    for(x <- (0 to S); y <- (0 to S)) {
      if(!inter.exists(e => e._1 == ((x,y)))){
        val f = squareCoords.filter(s => s._2._1 / S == x && s._2._2 / S == y)
        val res = ((x,y), f.map(e => densities(e._1).toDouble / (K * K).toDouble).sum / f.size)
        inter :+= res
      }
    }
    superPixels = inter.zipWithIndex.map(i => SuperPixel(i._2, i._1._1, i._1._2))
  } 
  def slic_assignment {
   pixels.foreach{ pix =>
    val (d, k): (Double, Int)= superPixels.map(s => (D(pix, s), s.k)).minBy(_._1)
    if(pix.d == -1 || pix.d > d){
      pix.d = d
      pix.l = k
    }
   }
   updates
   //TODO call residual change
   if(E > LOW_THRESHOLD)
    slic_assignment
  }

  def updates {
    superPixels.foreach{ s =>
      val copy = s.getCopy
      val (xs, ys, ds) = pixels.filter(_.l == s.k).map(p => (p.pos._1, p.pos._2, p.d)).unzip3
      s.pos = ((s.pos._1 + xs.sum) / xs.size, (s.pos._2 + ys.sum) / ys.size)
      s.d = (ds.sum + s.d) / ds.size
      E += Math.sqrt(sqr(copy.pos._1 - s.pos._1) + sqr(copy.pos._2 - s.pos._2) + sqr(copy.d - s.d))
    }
    E = E / K
  }

  def suppression {
    superPixels = superPixels.filter(_.d > ALPHA * total.toDouble / K.toDouble) 
  }

  def merge {
    //TODO implement this
  }

  def D(a: Pixel, b: SuperPixel): Double = {
    val ds: Double = Math.sqrt((sqr(a.pos._1 - b.pos._1) + sqr(a.pos._2 - b.pos._2)).toDouble)
    val dt: Double = Math.abs(a.d - b.d)
    Math.sqrt(sqr(dt) + sqr(ds) / K * SIGMA) 
  }

  def sqr(x: Double): Double = x * x 
}
