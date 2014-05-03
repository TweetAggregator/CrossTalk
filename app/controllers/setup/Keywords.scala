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
  val keywordsForm: Form[AllTranslations] = Form(
    mapping(
      "translations" -> seq(
        mapping(
          "language" -> optional(text),
          "keywords" -> list(text))(Translation.apply)(Translation.unapply)))(AllTranslations.apply)(AllTranslations.unapply))

  /**
   * Display an empty form.
   */
  def form = Action {
    Ok(html.keywordsForm(keywordsForm));
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

    val resultForm = keywordsForm.bindFromRequest.get
    println(resultForm)
    //println("RESULT :D:D:D:D " + resultForm.translations.apply(0).keywords)

    val keywords = resultForm.translations.apply(0).keywords

    val startLanguage = "en"
    val targetLanguages = List("fra", "de")

    val tradsAndSyns =
      for (keyword <- keywords) yield {
        val (trads, syns) = jobs.Translator(startLanguage, targetLanguages, keyword)()
//        (trads.flatten ++ syns).map(_.as[String])
          Translation( Some(keyword),(trads.flatten ++ syns).map(_.as[String]) )
      }

    Ok(html.keywordsSummary(keywordsForm.fill(AllTranslations(tradsAndSyns))))

  }
}
