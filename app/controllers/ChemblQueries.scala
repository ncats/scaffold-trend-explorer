package controllers

import javax.inject.Inject

import play.api.db.Database

/**
  * A one line summary.
  *
  * @author Rajarshi Guha
  */
class ChemblQueries @Inject()(db: Database) extends EntityHelper {

  def compoundCounts(smi: String): Map[Int, Int] = {
    if (smi.isEmpty) None
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
      val riter = results(pst.executeQuery())(rs => Map(rs.getInt(1) -> rs.getInt(2)))
      riter.toList.flatten.toMap
    }
  }

  def assayCounts(smi: String): Map[Int, Int] = {
    if (smi.isEmpty) None

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
      val riter = results(pst.executeQuery())(rs => Map(rs.getInt(1) -> rs.getInt(2)))
      riter.toList.flatten.toMap
    }

  }
}
