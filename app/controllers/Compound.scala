package controllers

import java.sql.ResultSet
import javax.inject._

import play.api.db.Database
import play.api.mvc._
import play.api.libs.json._

class Compound @Inject()(db: Database, cc: ControllerComponents)
  extends AbstractController(cc) {

  // https://stackoverflow.com/a/29962806
  def results[T](resultSet: ResultSet)(f: ResultSet => T) = {
    new Iterator[T] {
      def hasNext = resultSet.next()

      def next() = f(resultSet)
    }
  }

  // https://stackoverflow.com/a/36982026
  implicit val writes: Writes[Map[String,Any]] = new Writes[Map[String,Any]]{
    override def writes(o: Map[String,Any]): JsValue = {
      JsObject(
        o.map{kvp =>
          kvp._1 -> (kvp._2 match {
            case x: String => JsString(x)
            case x: Int => JsNumber(x)
            case _ => JsNull // Do whatever you want here.
          })
        }
      )
    }
  }

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
      val riter = results(pst.executeQuery()) {
        case rs => Map(
          "year" -> rs.getInt(1),
          "molregno" -> rs.getInt(2),
          "pubmed_id" -> rs.getInt(3),
          "doi" -> rs.getString(4).replace("https://doi.org/",""),
          "journal" -> rs.getString(5))
      }
      Ok(Json.toJson(riter.toList))
    }
  }

  def counts(smi: String) = Action {
    if (smi.isEmpty) BadRequest("Must specify a SMILES string")

    db.withConnection { conn =>
      val pst = conn.prepareStatement("select docs.year, count(cs.molregno) as nmol " +
        "from compound_structures cs, " +
        "activities act, docs " +
        "where rdmol_smiles@>'" + smi + "' " +
        "and cs.molregno = act.molregno " +
        "and act.doc_id = docs.doc_id " +
        "and act.standard_relation = '=' " +
        "and docs.year is not null " +
        "group by year " +
        "order by year")
      val riter = results(pst.executeQuery())(rs => Map("year" -> rs.getInt(1), "count" -> rs.getInt(2)))
      Ok(Json.toJson(riter.toList))
    }

  }
}