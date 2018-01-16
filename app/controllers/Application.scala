package controllers

import java.util.UUID
import javax.inject._

import chemaxon.formats.{MolFormatException, MolImporter}
import chemaxon.util.MolHandler
import play.api.cache.SyncCacheApi
import play.api.db.Database
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc._
import play.api.{Configuration, Logger}
import play.twirl.api.Html

import scala.collection.mutable.ListBuffer


class Application @Inject()(cache: SyncCacheApi,
                            db: Database,
                            cc: MessagesControllerComponents,
                            configuration: Configuration)
  extends MessagesAbstractController(cc) with I18nSupport {

  // allows us to track the SMILES being specified
  val SESSION_UUID_KEY = "STE_UUID"

  val APP_VERSION = configuration.get[String]("ste.application.version")

  var splineCurve = false

  def index = Action { implicit request =>
    implicit lazy val config = configuration

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
    implicit lazy val config = configuration
    Ok(views.html.about(this))
  }

  def delete(smiles: String) = Action { implicit request =>
    val property = request.queryString("property").head
    val uuid = request.session.get(SESSION_UUID_KEY).getOrElse("")
    var smilesList = cache.get(uuid).getOrElse(new ListBuffer[String]())
    smilesList = smilesList.filter(!_.equals(smiles))
    cache.set(uuid, smilesList)
    Redirect(routes.Application.displayTrends(smilesList, property))
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

  def invalidSmiles(smi: String): Boolean = {
    var mol = MolImporter.importMol("C")
    try {
      mol = MolImporter.importMol(smi)
    } catch {
      case e: MolFormatException => return true
    }
    val valenceErrors = mol.hasValenceError
    val emptyMol = mol.isEmpty
    val hasQuery = mol.isQuery
    val hasPseudo = mol.getAtomArray.exists(_.isPseudo)

    mol.aromatize()
    mol.dearomatize()
    val badArom = mol.getBondArray.exists(_.isConjugated)

    valenceErrors || hasQuery || hasPseudo || emptyMol
  }

  /**
    * some simple check -
    *
    * - benzene ring
    * - linear chain of length <= 5
    */


  def trivialMolecule(smi: String): Boolean = {
    val benzene = "c1ccccc1"
    val pyrrole = "c1cc[nH]c1"

    val mh = new MolHandler(smi, false)

    val mol = mh.getMolecule
    mol.aromatize()
    val q = mol.toFormat("smiles:u")

    val isBenzene = q.equals(benzene)
    val isPyrrole = q.equals(pyrrole)

    // check for linear C chains
    val maxChainLength = 8
    val allCarbon = !mh.getMolecule.getAtomArray.exists(!_.getSymbol.equals("C"))
    val hasRing = mh.getMolecule.getSSSR.length > 0
    val noBranching = !mh.getMolecule.getAtomArray.exists(_.getBondCount > 2)

    isBenzene || isPyrrole || (allCarbon && !hasRing && noBranching && mh.getHeavyAtomCount < maxChainLength)
  }

  def search = Action { implicit request =>
    implicit lazy val config = configuration

    val uuid = request.session.get(SESSION_UUID_KEY).getOrElse("")
    val smilesList = cache.get(uuid).getOrElse(new ListBuffer[String]())

    TrendForm.form.bindFromRequest.fold(
      formWithErrors => {
        BadRequest(views.html.error(this, Html("<p class='lead'>The form was not correctly filled. This is probably a bug that should be reported to guhar@nih.gov</p>")))
      },
      data => {

        splineCurve = data.smoothCurve match {
          case Some(smoothCurve) => true
          case None => false
        }

        data.smiles.trim match {
          case "" if smilesList.isEmpty =>
            BadRequest(views.html.error(this, Html("<p class='lead'>No SMILES was specified. Maybe you specified an empty string by mistake.</p>")))
          case "" if smilesList.nonEmpty && smilesList.size <= 9 =>
            Redirect(routes.Application.displayTrends(smilesList, data.property))
          case _ if invalidSmiles(data.smiles.trim) => BadRequest(views.html.error(this, Html("<code>" + data.smiles + "</code> is an invalid SMILES string")))
          case _ if trivialMolecule(data.smiles.trim) => BadRequest(views.html.error(this, Html("<code>" + data.smiles + "</code> is too frequent a fragment")))
          case _ => {
            if (smilesList.size + 1 > 9)
              BadRequest(views.html.error(this, Html("A maximum of 9 substructures can be compared")))

            smilesList += data.smiles
            cache.set(uuid, smilesList)
            Redirect(routes.Application.displayTrends(smilesList, data.property))
          }
        }
      }
    )
  }

  def displayTrends(smiles: Seq[String], property: String) = Action { implicit request =>
    implicit lazy val config = configuration

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
            case "activity" => chembl.assayActivity(s)
            case "qed" => chembl.medianQED(s)
            case "logS" => chembl.medianSolubility(s)
            case "Fsp3" => chembl.medianFsp3(s)
            case "sa" => chembl.medianSA(s)
          }
          cache.set(key, v)
          v
        })
      })
    }


    Ok(views.html.trends(this, TrendForm.form, routes.Application.search(), property, smiles, smiColMap, trendData.flatten.toMap, splineCurve))
  }
}