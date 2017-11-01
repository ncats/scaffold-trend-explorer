package controllers

import java.sql.ResultSet
import javax.inject._

import play.api.db.Database
import play.api.mvc._

class Compound @Inject()(db: Database, cc: ControllerComponents) extends AbstractController(cc) {

  def results[T](resultSet: ResultSet)(f: ResultSet => T) = {
    new Iterator[T] {
      def hasNext = resultSet.next()

      def next() = f(resultSet)
    }
  }

  def get(smi: String) = Action {
    db.withConnection { conn =>
      val pst = conn.prepareStatement("select docs.year, count(cs.molregno) as nmol from compound_structures cs, " +
        "activities act, docs where " +
        "rdmol_smiles@>'" + smi + "' " +
        "and cs.molregno = act.molregno " +
        "act.doc_id = docs.doc_id " +
        "and act.standard_relation = '=' " +
        "and docs.year is not null " +
        "group by year" +
        "order by year")
      val riter = results(pst.executeQuery()) {

      }


    }
    Ok("Evrythings OK from compound")
  }
}