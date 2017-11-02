package controllers

import java.sql.ResultSet
import javax.inject._

import play.api.db._
import play.api.libs.json._
import play.api.mvc._

class Assay @Inject()(db: Database, cc: ControllerComponents) extends AbstractController(cc) {

  // https://stackoverflow.com/a/29962806
  def results[T](resultSet: ResultSet)(f: ResultSet => T) = {
    new Iterator[T] {
      def hasNext = resultSet.next()

      def next() = f(resultSet)
    }
  }

  // https://stackoverflow.com/a/36982026
  implicit val writes: Writes[Map[String, Any]] = new Writes[Map[String, Any]] {
    override def writes(o: Map[String, Any]): JsValue = {
      JsObject(
        o.map { kvp =>
          kvp._1 -> (kvp._2 match {
            case x: String => JsString(x)
            case x: Int => JsNumber(x)
            case _ => JsNull // Do whatever you want here.
          })
        }
      )
    }
  }

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