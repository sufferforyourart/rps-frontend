package models
import play.api.libs.json.Json

case class RbsBots(yourName: String, opponentName: String, pointsToWin: Int, maxRounds:Int, dynamiteCount:Int)

object RbsBots {
  implicit val formats = Json.format[RbsBots]
}

case class bot(opponentName: String, pointsToWin: Int, maxRounds:Int, dynamiteCount:Int)

object bot {
  implicit val formats = Json.format[bot]
}

case class lastMove(lastOpponentMove:String, lastSystemMove : String)

object lastMove{
  implicit val formats=Json.format[lastMove]
}