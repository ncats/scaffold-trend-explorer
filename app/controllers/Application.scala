package controllers

import java.util.UUID
import javax.inject._

import chemaxon.util.MolHandler
import play.api.cache.SyncCacheApi
import play.api.db.Database
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc._
import play.api.{Configuration, Logger}

import scala.collection.mutable.ListBuffer


class Application @Inject()(cache: SyncCacheApi,
                            db: Database,
                            cc: MessagesControllerComponents,
                            config: Configuration)
  extends MessagesAbstractController(cc) with I18nSupport {

  // allows us to track the SMILES being specified
  val SESSION_UUID_KEY = "STE_UUID"

  val APP_VERSION = config.get[String]("ste.application.version")

  def index = Action { implicit request =>
    // on loading the main page we start fresh, so: first, remove smiles list from cache
    request.session.get(SESSION_UUID_KEY) match {
      case Some(x) => cache.remove(x)
      case None => {} // nothing to do if we have no session key
    }
    // generate new UUID and set it in the session
    Ok(views.html.index(this, TrendForm.form, routes.Application.search())).withSession(
      request.session + (SESSION_UUID_KEY -> UUID.randomUUID().toString)
    )
  }

  def marvin = Action {
    Ok(views.html.marvin())
  }

  def molconvert = Action(parse.json) { request =>
    val json = request.body
    val mol = (json \ "structure").as[String]
    val format = (json \ "parameters").as[String]
    val mh: MolHandler = new MolHandler(mol)
    val out = mh.getMolecule.toFormat(format)
    val outJson = Json.obj(
      "structure" -> out,
      "format" -> format,
      "contentUrl" -> ""
    )
    Ok(outJson).as("application/json")
  }

  def about = Action {
    Ok(views.html.about(this))
  }

  def delete(smiles: String) = Action { implicit request =>
    val uuid = request.session.get(SESSION_UUID_KEY).getOrElse("")
    var smilesList = cache.get(uuid).getOrElse(new ListBuffer[String]())
    smilesList = smilesList.filter(!_.equals(smiles))
    cache.set(uuid, smilesList)
    Redirect(routes.Application.displayTrends(smilesList, "compounds"))
  }

  def download(smiles: String, property: String) = Action { implicit request =>
    val data: Map[Int, Double] = cache.get(smiles + "$" + property).get
    val builder = new StringBuilder
    builder ++= s"#$smiles\n#$property\nYear,Count\n"
    for ((k, v) <- data) builder ++= k + "," + v + "\n"

    Ok(builder.toString)
      .withHeaders(
        CONTENT_TYPE -> "text/csv",
        CONTENT_DISPOSITION -> s"attachment; filename=ste-$smiles-$property.csv")
  }

  def search = Action { implicit request =>
    val uuid = request.session.get(SESSION_UUID_KEY).getOrElse("")
    val smilesList = cache.get(uuid).getOrElse(new ListBuffer[String]())

    TrendForm.form.bindFromRequest.fold(
      formWithErrors => {
        BadRequest(views.html.about(this))
      },
      data => {
        data.smiles match {
          case "" if smilesList.isEmpty => BadRequest(views.html.about(this))
          case "" if smilesList.nonEmpty => {
            Redirect(routes.Application.displayTrends(smilesList, data.property))
          }
          case _ => {
            smilesList += data.smiles
            cache.set(uuid, smilesList)
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
        val value = cache.get[Map[Int, Double]](key)
        value.getOrElse({
          Logger.info("Adding " + s + "/" + property + " to cache")
          val v = property match {
            case "compounds" => chembl.compoundCounts(s)
            case "assays" => chembl.assayCounts(s)
            case "qed" => chembl.medianQED(s)
            case "logS" => chembl.medianSolubility(s)
            case "Fsp3" => chembl.medianFsp3(s)
          }
          cache.set(key, v)
          v
        })
      })
    }
    Ok(views.html.trends(this, TrendForm.form, routes.Application.search(), property, smiles, smiColMap, trendData.flatten.toMap))
  }
}