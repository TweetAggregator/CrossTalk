package models

case class AllTranslations(
  translations: Seq[Translation]
)

case class Translation(
  language: Option[String],
  keywords: List[String]
)

