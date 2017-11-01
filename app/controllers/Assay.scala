package controllers

import javax.inject._
import play.api.db._
import play.api.mvc._

class Assay @Inject()(db:Database, cc: ControllerComponents) extends AbstractController(cc) {

  def get = Action {
    db.withConnection{ conn =>

    }
    Ok("Evrythings OK")
  }

}