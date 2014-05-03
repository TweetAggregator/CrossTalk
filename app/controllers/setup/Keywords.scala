package controllers.setup

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._

import views._

import models._

object Keywords extends Controller {
  
  /**
   * Contact Form definition.
   */
  val contactForm: Form[AllTranslations] = Form(
    
    // Defines a mapping that will handle Contact values
    mapping(
      // Defines a repeated mapping
      "translations" -> seq(
        mapping(
          "language" -> optional(email),
          "keywords" -> list(
            text) 
        )(Translation.apply)(Translation.unapply)
      )
      
    )(AllTranslations.apply)(AllTranslations.unapply)
  )
  
  /**
   * Display an empty form.
   */
  def form = Action {
    Ok(html.form(contactForm));
  }
  
  /**
   * Display a form pre-filled with an existing Contact.
   */
  def finalSubmission = Action { implicit request =>
val resultForm = contactForm.bindFromRequest.get


println("THIS IS THE END " + resultForm)
Ok(html.end())
  }

  
  /**
   * Handle form submission.
   */
  def initialSubmission = Action { implicit request =>

    println("MAMAN : " + request)
val resultForm = contactForm.bindFromRequest.get
println(resultForm)
println("RESULT :D:D:D:D " + resultForm.translations.apply(0).keywords)

val keywords = resultForm.translations.apply(0).keywords

    val startLanguage = "en"
    val targetLanguages = List("fra","de")


/*    val tradsAndSyns =
      for (keyword <- keywords) yield {
        val (trads, syns) = routes.Translator(startLanguage, targetLanguages, keyword)()
        (trads.flatten ++ syns).map(_.as[String])
      }*/
    //TODO rajouter liste de requete de traducitons 
    //renvoyer l anglais en premier
    //les autres langues sont optionnelles
    val existingContact = AllTranslations(
     translations = List(
        Translation(
         Some("Afficher l anglais en premier toujours!"), keywords
        ),
        Translation(
         Some("Français"), List("mouton", "belier")
        )
      )
    )
    Ok(html.summary(contactForm.fill(existingContact)))
    
  }
}
