import play.api._
import play.api.mvc._
import play.api.mvc.Results._
import scala.concurrent.Future

object Global extends GlobalSettings {

  override def onStart(app: Application) {
    Logger.info("Crosstalk: starting Big Brother...")
  }

  override def onStop(app: Application) {
    Logger.info("Crosstalk: Big Brother is sad...")
  }

  override def onError(request: RequestHeader, ex: Throwable) = {
    Future.successful(InternalServerError(
      views.html.errorPage("Error", ex.toString)))
  }

  override def onBadRequest(request: RequestHeader, error: String) = {
    Future.successful(BadRequest("Bad Request: " + error))
  }
  
    override def onHandlerNotFound(request: RequestHeader) = {
    Future.successful(NotFound(
      views.html.errorPage("404 Not Found", request.path)
    ))
  }

}