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
  val keyWordForm: Form[Contact] = Form(
    
    // Defines a mapping that will handle Contact values
    mapping(
      "keyWords" -> list(text)
    )
    (Contact.apply)(Contact.unapply)
  )
  
  /**
   * Display an empty form.
   */
  def form = Action {
    Ok(html.contact.form(keyWordForm));
  }
  
  def submit = Action { implicit request =>
    val form = keyWordForm.bindFromRequest
    println(form)
    val contact = Contact( form.data.values.toList) //form.value.get
    Ok(html.contact.summary(contact))
  }
  
}