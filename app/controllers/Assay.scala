package controllers

import javax.inject._

import play.api.db._
import play.api.libs.json._
import play.api.mvc._

class Assay @Inject()(db: Database, cc: ControllerComponents)
  extends AbstractController(cc) with EntityHelper {

  val chembl = new ChemblQueries(db)

  def raw(smi: String) = TODO

  def counts(smi: String) = Action {
    if (smi.isEmpty) BadRequest("Must specify a SMILES string")
    val listOfMaps = chembl.assayCounts(smi).map {
      case (k, v) => Map("year" -> k, "count" -> v)
    }
    Ok(Json.toJson(listOfMaps))
  }

}