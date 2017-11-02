package controllers

import javax.inject._

import play.api.db._
import play.api.libs.json._
import play.api.mvc._

class Assay @Inject()(db: Database, cc: ControllerComponents)
  extends AbstractController(cc) with EntityHelper {


  def raw(smi: String) = TODO

  def counts(smi: String) = Action {
    if (smi.isEmpty) BadRequest("Must specify a SMILES string")

    db.withConnection { conn =>
      val pst = conn.prepareStatement("select docs.year, count(distinct act.assay_id) as nassay " +
        "from compound_structures cs, activities_robustz act, docs " +
        "where " +
        "rdmol_smiles@>'" + smi + "' " +
        "and cs.molregno = act.molregno " +
        "and act.doc_id = docs.doc_id " +
        "and docs.year is not null " +
        "group by year " +
        "order by year;")
      val riter = results(pst.executeQuery())(rs => Map("year" -> rs.getInt(1), "count" -> rs.getInt(2)))
      Ok(Json.toJson(riter.toList))
    }

  }

}