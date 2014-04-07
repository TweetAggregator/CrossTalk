import utils.Http._
import play.api.libs.json.Json
import play.api.libs.json.Reads
import play.api.libs.json.JsValue
import play.api.libs.json.JsObject
import play.api.libs.json.JsPath
import play.api.libs.json.JsArray
import play.api.libs.json.JsString
import play.api.libs.json.JsUndefined

// class Translator(from: String, destL: List[String], keyword: String => (List[List[JsValue]], List[JsValue]) ) {
case class Translator(from: String, destL: List[String], keyword: String){
	def apply(): (List[List[JsValue]], List[JsValue]) = {
		( translate(from, destL, keyword), synonyms(from, keyword) )
		// translate(from, destL, keyword)
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


}