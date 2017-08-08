package forms
import models._
import play.api.data.Form
import play.api.data.Forms._

object RbsBotsForms {

  val RbsBotsorm = Form(
    mapping(
      "yourName" -> text(minLength = 2),
      "opponentName" -> text(minLength = 2),
      "pointsToWin" -> number,
      "maxRounds" -> number,
      "dynamiteCount" -> number
    )(RbsBots.apply)(RbsBots.unapply)
  )

}