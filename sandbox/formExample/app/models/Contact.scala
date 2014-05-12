package models

case class Contact(
  informations: Seq[ContactInformation]
)

case class ContactInformation(
  email: Option[String],
  phones: List[String]
)