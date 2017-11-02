package controllers

import java.sql.ResultSet

import play.api.libs.json._

trait EntityHelper {

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

}
