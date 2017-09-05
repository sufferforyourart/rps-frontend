package models
import play.api.libs.json.Json

case class RbsBots(numberOfBots:String,
                   mode: Option[String],
                   player1Name: String,
                   player2Name: String,
                   pointsToWin: Int,
                   maxRounds:Int,
                   dynamiteCount:Int,
                   player1url: Option[String],
                   player2url: Option[String])


case class bot(opponentName: String, pointsToWin: Int, maxRounds:Int, dynamiteCount:Int)

object bot {
  implicit val formats = Json.format[bot]
}

case class lastMove(lastOpponentMove:String, lastSystemMove : String)

object lastMove{
  implicit val formats=Json.format[lastMove]
}