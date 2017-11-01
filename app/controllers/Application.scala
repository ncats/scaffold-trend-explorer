package controllers

import javax.inject._
import play.api.mvc._

class Application @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def about = Action {
    Ok(views.html.about())
  }
}