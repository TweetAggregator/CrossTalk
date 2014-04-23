import models._

class ClusteringLib(squareCoords: Map[GeoSquare, (Int, Int)], densities: Map[GeoSquare, Long]) {
  //TODO replace the sqr By Math pow
  //Implementation of Slic algorithm
  val K: Long = Math.sqrt(squareCoords.size).toLong
  val S: Int = Math.sqrt(K).toInt
  val pixels: List[Pixel] = squareCoords.map(e => Pixel(e._2, -1, -1)).toList
  var superPixels: List[SuperPixel] = Nil
  //TODO determine a correct value
  val SIGMA: Double = 1.0/8.0
  val LOW_THRESHOLD = 0.01 // TODO find that
  val ALPHA: Double = 0.0003  //TODO find that
  var E: Double = 0.0

  val total = densities.map(_._2).sum
  
  def slic_me: List[SuperPixel] = {
    slic_init
    slic_assignment
    suppression
    superPixels
  }
  
  def slic_init {
    var inter: List[((Int, Int), Double)] = Nil
    for(x <- (0 until S); y <- (0 until S)) {
      if(!inter.exists(e => e._1 == ((x,y)))){
        val f = squareCoords.filter(s => s._2._1 / S == x && s._2._2 / S == y)
        val sorted: List[(Int,Int)] = f.map(_._2).toList.sorted
        val pos = sorted(sorted.size / 2)
        val res = (pos, f.map(e => densities(e._1).toDouble / (K * K).toDouble).sum / f.size)
        inter :+= res
      }
    }
    superPixels = inter.zipWithIndex.map( i => SuperPixel(i._2, i._1._1, i._1._2))
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
      assert(xs.size == ys.size && xs.size == ds.size)
      s.pos = ((s.pos._1 + xs.sum) / xs.size, (s.pos._2 + ys.sum) / ys.size)
      s.d = (ds.sum + s.d) / ds.size
      //TODO here is one bug
      E += Math.sqrt(sqr(copy.pos._1 - s.pos._1) + sqr(copy.pos._2 - s.pos._2) + sqr(copy.d - s.d))
      
    }
    E = E / K
  }

  def suppression {
    //superPixels = superPixels.filter(_.d > ALPHA * total.toDouble / K.toDouble) 
    superPixels = superPixels.filter(s => pixels.exists(p => p.l == s.k))
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
