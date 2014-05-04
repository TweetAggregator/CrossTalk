package models

case class AllTranslations(
  translations: Seq[Translation]
)

case class Translation(
  targetLanguage :String, 
  originalKeyword: String,
  keywords: List[String]
)

