package controllers

import utils.Http._
import play.api.mvc._
import play.api.libs.json._

object Translation extends Controller {
  def translate(source: String, dests: List[String], keyword: String) = Action {
    val translator = Translator(source, dests, keyword)
    val (translations, synonyms) = translator()

    val jsonResponse = Json.obj(
      "translations" -> translations,
      "synonyms" -> synonyms
    )
    Ok(jsonResponse)
  }
}

case class Translator(from: String, destL: List[String], keyword: String){
	def apply(): (List[List[String]], List[String]) = {
    (translate(from, destL, keyword), synonyms(from, keyword))
	}

	/** Parse a string into a list of JsValue  containing translation */
	def parseJson(str: String): List[String] = {
	  try { 
      val json = Json.parse(str.toString)
      (json \ "tuc").as[JsArray].value.map(x => (x \ "phrase" \ "text") ).filter(json => !json.isInstanceOf[JsUndefined]).toList.map(_.as[String])
    } catch {
      case e: JsResultException => List()
    }
	}


	def translate(from: String, dest: String, keyword: String): List[String] = {
	  val serverAddr = "http://glosbe.com/gapi/translate?from=" + from + "&dest=" + dest + "&format=json&phrase=" + keyword + "&pretty=true&tm=false"
	  
	  val stream = askForGet(serverAddr)
	  val ret = readAll(stream)
	  stream.close
	  parseJson(ret)
	}

	def translate(from: String, destL: List[String], keyword: String): List[List[String]] = {
		destL.map(e => translate(from, e, keyword))
	}

	/* "tries" to get synonyms of the keyword in the user language */
	def synonyms(lang: String, keyword: String): List[String] = {
	  val serverAddr = "http://glosbe.com/gapi/translate?from=" + lang + "&dest=" + lang + "&format=json&phrase=" + keyword + "&pretty=true&tm=false"
	  
	  val stream = askForGet(serverAddr)
	  val ret = readAll(stream)
	  stream.close
	  parseJson(ret)
	}


}
