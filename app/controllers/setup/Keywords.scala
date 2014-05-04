package controllers.setup

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._

import views._

import models._

object Keywords extends Controller {

     val targetLanguages = List(("en","Synonyms in English"),("fra","French"), ("deu","German"), ("ru","Russian"))
  /**
   * Contact Form definition.
   */
  val keywordsForm: Form[AllTranslations] = Form(
    mapping(
      "translations" -> seq(
        mapping(
          "targetLanguage" -> text,
          "originalKeyword" -> text,
          "keywords" -> list(text))  (Translation.apply)(Translation.unapply)))(AllTranslations.apply)(AllTranslations.unapply))


  val initialInputForm: Form[InitialInput] = Form(

        mapping(
          "languages" -> list(text),
          "keywords" -> list(text)  )(InitialInput.apply)(InitialInput.unapply))


  /**
   * Display an empty form.
   */
  def form = Action {
    Ok(html.keywordsForm(initialInputForm));
  }

  /**
   * Display a form pre-filled with an existing Contact.
   */
  def finalSubmission = Action { implicit request =>
    val resultForm = keywordsForm.bindFromRequest.get

    println("THIS IS THE END " + resultForm)
    Ok(html.keywordsEnd("pipi"))
  }

  /**
   * Handle form submission.
   */
  def initialSubmission = Action { implicit request =>

    val resultForm = initialInputForm.bindFromRequest.get
    println(resultForm)
    //println("RESULT :D:D:D:D " + resultForm.translations.apply(0).keywords)

    val keywords = resultForm.keywords
    val translationLanguages = resultForm.languages
  //resultForm.translations.apply(1).keywords
println("MEEEE " + translationLanguages)
    val startLanguage = "en"

    val tradsAndSyns =
      for (keyword <- keywords) yield {
        for (language <- translationLanguages) yield {
        val (trads, syns) = jobs.Translator(startLanguage, List(targetLanguages.apply(language.toInt)._1), keyword)()
          Translation( targetLanguages.apply(language.toInt)._2,keyword,(trads.flatten).map(_.as[String]) )
        }
      }
    

    Ok(html.keywordsSummary(keywordsForm.fill(AllTranslations(tradsAndSyns.flatten))))
//  Ok(html.keywordsSummary(keywordsForm.fill(AllTranslations(List(Translation("French","mouse",List("souris", "rat", "souriceau")), Translation("German","mouse",List("Maus", "Computermaus", "mausen", "Mäuserich", "Mäuse fangen", "Ratte")), Translation("French","bottle",List("bouteille", "biberon", "embouteiller", "canette")), Translation("German","bottle",List("Flasche", "Flaschen", "in Flaschen abfüllen", "Pulle")))))))

  }
}
