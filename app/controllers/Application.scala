package controllers

import javax.inject._

import play.api.cache.SyncCacheApi
import play.api.db.Database
import play.api.i18n.I18nSupport
import play.api.mvc._
import play.api.{Configuration, Logger}

import scala.collection.mutable.ListBuffer

@Singleton
class Application @Inject()(cache: SyncCacheApi, db: Database, cc: MessagesControllerComponents, config: Configuration)
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
        data.smiles match {
          case "" if smilesList.isEmpty => BadRequest(views.html.about())
          case "" if smilesList.nonEmpty => {
            Redirect(routes.Application.displayTrends(smilesList, data.property))
          }
          case _ => {
            smilesList += data.smiles
            Redirect(routes.Application.displayTrends(smilesList, data.property))
          }
        }
      }
    )
  }

  def displayTrends(smiles: Seq[String], property: String) = Action { implicit request =>

    // we'll assume we have up to 9 trends being plotted
    val colorPalette = List[String]("#e41a1c", "#377eb8", "#4daf4a", "#984ea3", "#ff7f00", "#ffff33", "#a65628", "#f781bf", "#999999")

    val smiColMap = smiles.zip(colorPalette.take(smiles.size).reverse).toMap

    val chembl = new ChemblQueries(db)
    val trendData = smiles.map { s =>
      Map(s -> {
        val key = s + "$" + property
        val value = cache.get[Map[Int, Int]](key)
        value.getOrElse({
          Logger.info("Adding " + s + "/" + property + " to cache")
          val v = property match {
            case "compounds" => chembl.compoundCounts(s)
            case "assays" => chembl.assayCounts(s)
          }
          cache.set(key, v)
          v
        })
      })
    }
    Ok(views.html.trends(TrendForm.form, routes.Application.search(), property, smiles, smiColMap, trendData.flatten.toMap))
  }
}