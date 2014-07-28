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

import jobs.Translator

@RunWith(classOf[JUnitRunner])
class TranslatorSpec extends Specification {
  
  "Translator" should {
    "translate" in new WithApplication {
      
      val from = "deu"
      val dest = "fra"
      val dest2 = "eng"
      val keyword = "Bier"

      val (translations, synonyms) = Translator(from, List(dest, dest2), keyword)()

      translations(1) should contain ("beer")
      translations(1) should contain ("ale")
      synonyms should contain ("Gerstensaft")
    }
  }
}
