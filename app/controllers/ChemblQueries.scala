package controllers

import javax.inject.Inject

import play.api.db.Database

/**
  * A one line summary.
  *
  * @author Rajarshi Guha
  */
class ChemblQueries @Inject()(db: Database) extends EntityHelper {

  def medianFsp3(smi: String): Map[Int, Double] = {
    if (smi.isEmpty) None
    db.withConnection { conn =>
      val pst = conn.prepareStatement("select year, median(fsp3) as val " +
        "from  ste_moldoc stm, ste_descriptors sted, rdk.mols " +
        "where m@>'" + smi + "' " +
        "and mols.molregno = stm.molregno " +
        "and mols.molregno = sted.molregno " +
        "group by year " +
        "order by year")
      val riter = results(pst.executeQuery())(rs => Map(rs.getInt(1) -> rs.getDouble(2)))
      riter.toList.flatten.toMap
    }
  }

  def medianSA(smi: String): Map[Int, Double] = {
    if (smi.isEmpty) None
    db.withConnection { conn =>
      val pst = conn.prepareStatement("select year, median(sa) as val " +
        "from  ste_moldoc stm, ste_descriptors sted, rdk.mols " +
        "where m@>'" + smi + "' " +
        "and mols.molregno = stm.molregno " +
        "and mols.molregno = sted.molregno " +
        "group by year " +
        "order by year")
      val riter = results(pst.executeQuery())(rs => Map(rs.getInt(1) -> rs.getDouble(2)))
      riter.toList.flatten.toMap
    }
  }

  def medianSolubility(smi: String): Map[Int, Double] = {
    if (smi.isEmpty) None
    db.withConnection { conn =>
      val pst = conn.prepareStatement("select year, median(logS) as val " +
        "from  ste_moldoc stm, ste_descriptors sted, rdk.mols " +
        "where m@>'" + smi + "' " +
        "and mols.molregno = stm.molregno " +
        "and mols.molregno = sted.molregno " +
        "group by year " +
        "order by year")
      val riter = results(pst.executeQuery())(rs => Map(rs.getInt(1) -> rs.getDouble(2)))
      riter.toList.flatten.toMap
    }
  }

  def medianQED(smi: String): Map[Int, Double] = {
    if (smi.isEmpty) None
    db.withConnection { conn =>
      val pst = conn.prepareStatement("select year, median(qed) as val " +
              "from  ste_moldoc stm, ste_descriptors sted, rdk.mols " +
              "where m@>'" + smi + "' " +
              "and mols.molregno = stm.molregno " +
              "and mols.molregno = sted.molregno " +
              "group by year " +
              "order by year")
      val riter = results(pst.executeQuery())(rs => Map(rs.getInt(1) -> rs.getDouble(2)))
      riter.toList.flatten.toMap
    }
  }

  def compoundCounts(smi: String): Map[Int, Double] = {
    if (smi.isEmpty) None
    db.withConnection { conn =>
      val pst = conn.prepareStatement("select year, count(stm.molregno) as val " +
              "from  ste_moldoc stm, rdk.mols " +
              "where m@>'" + smi + "' " +
              "and mols.molregno = stm.molregno " +
              "group by year " +
              "order by year")
      val riter = results(pst.executeQuery())(rs => Map(rs.getInt(1) -> rs.getDouble(2)))
      riter.toList.flatten.toMap
    }
  }

  def assayCounts(smi: String): Map[Int, Double] = {
    if (smi.isEmpty) None

    db.withConnection { conn =>
      val pst = conn.prepareStatement("select docs.year, count(distinct act.assay_id) as val " +
        "from rdk.mols, activities_robustz act, docs " +
        "where " +
        "m@>'" + smi + "' " +
        "and mols.molregno = act.molregno " +
        "and act.doc_id = docs.doc_id " +
        "and docs.year is not null " +
        "group by year " +
        "order by year;")
      val riter = results(pst.executeQuery())(rs => Map(rs.getInt(1) -> rs.getDouble(2)))
      riter.toList.flatten.toMap
    }
  }

  def assayActivity(smi: String): Map[Int, Double] = {
    if (smi.isEmpty) None

    db.withConnection { conn =>
      val pst = conn.prepareStatement("select docs.year, median(distinct act.standard_zscore) as val " +
        "from rdk.mols, activities_robustz act, docs " +
        "where " +
        "m@>'" + smi + "' " +
        "and mols.molregno = act.molregno " +
        "and act.doc_id = docs.doc_id " +
        "and docs.year is not null " +
        "group by year " +
        "order by year;")
      val riter = results(pst.executeQuery())(rs => Map(rs.getInt(1) -> rs.getDouble(2)))
      riter.toList.flatten.toMap
    }
  }

}
