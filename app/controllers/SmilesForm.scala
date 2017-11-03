package controllers

/**
  * SMILES entry form.
  *
  * Taken from https://github.com/playframework/play-scala-forms-example/blob/2.6.x/app/controllers/WidgetForm.scala
  *
  * @author Rajarshi Guha
  */
object SmilesForm {

  import play.api.data.Form
  import play.api.data.Forms._

  case class Data(smiles: String)

  val form = Form(
    mapping("smiles" -> nonEmptyText)(Data.apply)(Data.unapply)
  )

}
