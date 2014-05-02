package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._

import views._

import models._

object Contacts extends Controller {
  
  /**
   * Contact Form definition.
   */
  val contactForm: Form[Contact] = Form(
    
    // Defines a mapping that will handle Contact values
    mapping(
      // Defines a repeated mapping
      "informations" -> seq(
        mapping(
          "email" -> optional(email),
          "phones" -> list(
            text) 
        )(ContactInformation.apply)(ContactInformation.unapply)
      )
      
    )(Contact.apply)(Contact.unapply)
  )
  
  /**
   * Display an empty form.
   */
  def form = Action {
    Ok(html.contact.form(contactForm));
  }
  
  /**
   * Display a form pre-filled with an existing Contact.
   */
  def editForm = Action { implicit request =>
val resultForm = contactForm.bindFromRequest.get
println("THIS IS THE END " + resultForm)
Ok(html.contact.end())
  }

  
  /**
   * Handle form submission.
   */
  def submit = Action { implicit request =>
val resultForm = contactForm.bindFromRequest.get
println(resultForm)
println("RESULT :D:D:D:D " + resultForm.informations.apply(0).phones)

val keywords = resultForm.informations.apply(0).phones


    //TODO rajouter liste de requete de traducitons 
    //renvoyer l anglais en premier
    //les autres langues sont optionnelles
    val existingContact = Contact(
     informations = List(
        ContactInformation(
         Some("Afficher l anglais en premier toujours!"), keywords
        ),
        ContactInformation(
         Some("Français"), List("mouton", "belier")
        )
      )
    )
    Ok(html.contact.summary(contactForm.fill(existingContact)))
    
  }

  def selectKeywords = Action { implicit request =>
val resultForm = contactForm.bindFromRequest.get
println("THIS IS THE END " + resultForm)
Ok(html.contact.summary(contactForm))
  }
  
}