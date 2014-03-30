import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.test._
import play.api.test.Helpers._
import akka.actor.ActorSystem
import akka.actor.Props
import jobs._
import play.api.libs.ws.WS
import scala.concurrent.Future
import play.api.libs.ws.WS.WSRequestHolder
import play.api.libs.json.Json
import play.api.libs.json.Reads
import play.api.libs.json.JsValue
import play.api.libs.json.JsObject
import play.api.libs.json.JsPath
import play.api.libs.json.JsArray
import play.api.mvc.Action
import dispatch._, Defaults._

@RunWith(classOf[JUnitRunner])
class Translator extends Specification {

  "Translator" should {
    "translate" in new WithApplication {
     
  val svc = url("http://glosbe.com/gapi/translate?from=deu&dest=fra&format=json&phrase=Bier&pretty=true&tm=false")
    val response  = Http(svc OK as.String)
    for (c <- response){ 

        val json: JsValue = Json.parse(c)

        val ret = (json \ "tuc") match {
          // case JsArray(e) => e.map(x => println( (x \ "phrase" \ "text") ) ) // print the result
          case JsArray(e) => (e.map(x => (x \ "phrase" \ "text") ) )
          case _ =>  println("TROP NUL") 
        }

        println(ret)
    }

    // would be nice to have something like below
    // val list:JsValue = (json \ "tuc").value.foreach(x => (x \ "phrase" \ "text")).toList()

    }
  }
}