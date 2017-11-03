package controllers

import javax.inject._

import play.api.Configuration
import play.api.i18n.I18nSupport
import play.api.mvc._

import scala.collection.mutable.ListBuffer

@Singleton
class Application @Inject()(cc: MessagesControllerComponents, config: Configuration)
  extends MessagesAbstractController(cc) with I18nSupport {

  val APP_VERSION = config.get[String]("ste.application.version")

  private val smilesList = ListBuffer[String]()

  def index = Action { implicit request =>
    Ok(views.html.index(SmilesForm.form, routes.Application.search()))
  }

  def about = Action {
    Ok(views.html.about())
  }

  def search = Action { implicit request =>
    SmilesForm.form.bindFromRequest.fold(
      formWithErrors => {
        BadRequest(views.html.about())
      },
      data => {
        smilesList += data.smiles
        Redirect(routes.Application.displayTrends(smilesList))
      }
    )
  }

  def displayTrends(smiles: Seq[String]) = Action {
    Ok(views.html.trends(SmilesForm.form, routes.Application.search()))
  }
}