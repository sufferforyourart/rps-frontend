package forms
import models._
import play.api.data.Form
import play.api.data.Forms._

object RbsBotsForms {

  val RbsBotsorm = Form(
    mapping(
      "opponentName" -> text(minLength = 5),
      "pointsToWin" -> number,
      "maxRounds" -> number,
      "dynamiteCount" -> number
    )(RbsBots.apply)(RbsBots.unapply)
  )

}