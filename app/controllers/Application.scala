package controllers

import javax.inject._

import play.api.Configuration
import play.api.db.Database
import play.api.i18n.I18nSupport
import play.api.mvc._

import scala.collection.mutable.ListBuffer

@Singleton
class Application @Inject()(db: Database, cc: MessagesControllerComponents, config: Configuration)
  extends MessagesAbstractController(cc) with I18nSupport {

  val APP_VERSION = config.get[String]("ste.application.version")

  private val smilesList = ListBuffer[String]()

  def index = Action { implicit request =>
    smilesList.clear()
    Ok(views.html.index(TrendForm.form, routes.Application.search()))
  }

  def about = Action {
    Ok(views.html.about())
  }

  def search = Action { implicit request =>
    TrendForm.form.bindFromRequest.fold(
      formWithErrors => {
        BadRequest(views.html.about())
      },
      data => {
        smilesList += data.smiles
        Redirect(routes.Application.displayTrends(smilesList, data.property))
      }
    )
  }

  def displayTrends(smiles: Seq[String], property: String) = Action { implicit request =>

    // we'll assume we have up to 12 trends being plotted
    val colorPalette = List[String]("#a6cee3", "#1f78b4", "#b2df8a", "#33a02c",
      "#fb9a99", "#e31a1c", "#fdbf6f", "#ff7f00", "#cab2d6", "#6a3d9a",
      "#ffff99", "#b15928")

    val smiColMap = smiles.zip(colorPalette).toMap

    val chembl = new ChemblQueries(db)
    val trendData = smiles.map { s =>
      Map(s -> {
        property match {
          case "compounds" => chembl.compoundCounts(s)
          case "assays" => chembl.assayCounts(s)
        }
      })
    }
    Ok(views.html.trends(TrendForm.form, routes.Application.search(), property, smiles, smiColMap, trendData.flatten.toMap))
  }
}