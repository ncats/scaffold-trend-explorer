package controllers

import javax.inject._

import play.api.db.Database
import play.api.libs.json._
import play.api.mvc._

class Compound @Inject()(db: Database, cc: ControllerComponents)
  extends AbstractController(cc) with EntityHelper {

  val chembl = new ChemblQueries(db)

  def raw(smi: String) = Action {
    if (smi.isEmpty) BadRequest("Must specify a SMILES string")

    db.withConnection { conn =>
      val pst = conn.prepareStatement("select distinct docs.year, cs.molregno, docs.pubmed_id, docs.doi, journal " +
        "from compound_structures cs, " +
        "activities act, docs " +
        "where rdmol_smiles@>'" + smi + "' " +
        "and cs.molregno = act.molregno " +
        "and act.doc_id = docs.doc_id " +
        "and act.standard_relation = '=' " +
        "and docs.year is not null " +
        "order by year, pubmed_id, molregno")
      val riter = results(pst.executeQuery())(
        rs => Map(
          "year" -> rs.getInt(1),
          "molregno" -> rs.getInt(2),
          "pubmed_id" -> rs.getInt(3),
          "doi" -> rs.getString(4).replace("https://doi.org/", ""),
          "journal" -> rs.getString(5))
      )
      Ok(Json.toJson(riter.toList))
    }
  }

  def counts(smi: String) = Action {
    if (smi.isEmpty) BadRequest("Must specify a SMILES string")
    val listOfMaps = chembl.compoundCounts(smi).map {
      case (k, v) => Map("year" -> k, "count" -> v)
    }
    Ok(Json.toJson(listOfMaps))
  }
}