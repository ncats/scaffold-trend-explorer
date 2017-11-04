package controllers

/**
  * SMILES/trend property entry form.
  *
  * Taken from https://github.com/playframework/play-scala-forms-example/blob/2.6.x/app/controllers/WidgetForm.scala
  *
  * @author Rajarshi Guha
  */
object TrendForm {

  import play.api.data.Form
  import play.api.data.Forms._

  case class Data(smiles: String, property: String)

  val form = Form(
    mapping(
      "smiles" -> nonEmptyText,
      "property" -> text)(Data.apply)(Data.unapply)
  )

}
