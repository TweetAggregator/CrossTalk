package models

/* Model used for the forms for the keyword selection */

case class AllTranslations(
  translations: Seq[Translation]
) {
	def output : List[(String,List[String])] = {
		def rec (map: Map[String,List[String]], transList : List[Translation]): Map[String,List[String]] = transList match {
			case Nil => map
			case trans :: xs if (trans.targetLanguage == "Seperator") => rec(map, xs)
			case trans :: xs if (trans.targetLanguage != "Ignore") =>
				val list = map.getOrElse(trans.originalKeyword, Nil)
				rec ( map + (trans.originalKeyword -> (list ::: trans.keywords)), xs)
			case trans :: xs =>
				val list = map.getOrElse(trans.originalKeyword, Nil)
				rec ( map + (trans.originalKeyword -> list), xs)
		}
		rec( Map[String, List[String]](), translations.toList).toList
	}

}

case class Translation(
  targetLanguage :String, 
  originalKeyword: String,
  keywords: List[String]
)

case class InitialInput(
  languages: List[String],
  keywords: List[String]
)