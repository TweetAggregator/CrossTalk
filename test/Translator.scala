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
import org.apache.http._
import org.apache.http.entity.StringEntity
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpPost
import org.apache.http.message.BasicNameValuePair
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.client.methods.HttpGet
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.BufferedOutputStream
import java.io.InputStream
import play.api.libs.json.JsString
import play.api.libs.json.JsUndefined

@RunWith(classOf[JUnitRunner])
class Translator extends Specification {
  
  "Translator" should {
    "translate" in new WithApplication {
      
      val from = "deu"
      val dest = "fra"
      val dest2 = "eng"
      val keyword = "Bier"

      val ret = translateAndSynonyms(from, List(dest, dest2), keyword)
      println("Result TRANSLATION: " + ret._1)
      println("Result SYNONYMS   : " + ret._2)
    }
  }
  
  def askFor(request: String) = {
    val httpRequest = new HttpGet(request)
    httpRequest.addHeader("Content-Type", "application/x-www-form-urlencoded")
    val client = new DefaultHttpClient()
    // println(httpRequest)
    val twitterResponse = client.execute(httpRequest) /* Send the request and get the response */
    twitterResponse.getEntity().getContent()
  }

  /** Read all the data present in the InputStream in and return it as a String */
  def readAll(in: InputStream): String = {
    val inr = new BufferedReader(new InputStreamReader(in))
    val bf = new StringBuilder
    var rd = inr.readLine
    while (rd != null) { bf.append(rd); rd = inr.readLine }
    bf.toString
  }

  /** Parse a string into a list of JsValue  containing translation */
  def parseJson(str: String): List[JsValue] = {
    val json = Json.parse(str.toString)
    (json \ "tuc").as[JsArray].value.map(x => (x \ "phrase" \ "text") ).filter(json => !json.isInstanceOf[JsUndefined]).toList
  }

  def translate(from: String, dest: String, keyword: String): List[JsValue] = {
      val serverAddr = "http://glosbe.com/gapi/translate?from=" + from + "&dest=" + dest + "&format=json&phrase=" + keyword + "&pretty=true&tm=false"
      
      val stream = askFor(serverAddr)
      val ret = readAll(stream)
      stream.close
      parseJson(ret)
  }

  def translate(from: String, destL: List[String], keyword: String): List[List[JsValue]] = {
    destL.map(e => translate(from, e, keyword))
  }

  /* "tries" to get synonyms of the keyword in the user language */
  def synonyms(lang: String, keyword: String): List[JsValue] = {
      val serverAddr = "http://glosbe.com/gapi/translate?from=" + lang + "&dest=" + lang + "&format=json&phrase=" + keyword + "&pretty=true&tm=false"
      
      val stream = askFor(serverAddr)
      val ret = readAll(stream)
      stream.close
      parseJson(ret)
  }

  def translateAndSynonyms(from: String, destL: List[String], keyword: String): (List[List[JsValue]], List[JsValue]) = {
    (translate(from, destL, keyword), synonyms(from, keyword))
  }


}
