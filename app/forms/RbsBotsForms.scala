package forms
import models._
import play.api.data.Form
import play.api.data.Forms._

object RbsBotsForms {

  val RbsBotsorm = Form(
    mapping(
      "player1Name" -> text(minLength = 2),
      "player2Name" -> text(minLength = 2),
      "pointsToWin" -> number,
      "maxRounds" -> number,
      "dynamiteCount" -> number,
      "player1url" -> optional(text),
      "player2url" -> optional(text)
    )(RbsBots.apply)(RbsBots.unapply)
  )

}