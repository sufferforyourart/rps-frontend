package controllers

import java.util
import javax.inject._

import play.Routes
import play.api._
import play.api.i18n.MessagesApi
import play.api.mvc._
import forms.RbsBotsForms._
import models.{bot, lastMove, RbsBots}
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.mvc.Controller
import play.api.routing.JavaScriptReverseRouter
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.{MongoConnection, MongoDriver, DefaultDB}

import scala.concurrent.Future
import scala.concurrent.duration._

import play.api.mvc._
import play.api.libs.ws._
import play.api.http.HttpEntity


import javax.inject.Inject

import play.api._
import play.api.mvc._
import routes.javascript._

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}


@Singleton
class HomeController @Inject() (ws: WSClient, config : Configuration)(implicit val messagesApi: MessagesApi, context: ExecutionContext) extends Controller with i18n.I18nSupport {


  lazy val player1 = config.getString(s"external-url.player1-service.host").getOrElse("")
  lazy val player2 = config.getString(s"external-url.player2-service.host").getOrElse("")


  def index = Action { implicit request =>
    Ok(views.html.index(RbsBotsorm))
  }

  def submit = Action { implicit request =>

    RbsBotsorm.bindFromRequest.fold(
              formWithErrors => {
                BadRequest(views.html.index(formWithErrors))
              },
              successSub => {
                sendData(successSub)
                Ok(views.html.rps_bots(
                    successSub.yourName,
                    successSub.opponentName,
                    successSub.pointsToWin,
                    successSub.maxRounds,
                    successSub.dynamiteCount))
                }
    )

  }

  def showRpsBot(yourName: String, opponentName: String, pointsToWin: Int, maxRounds:Int, dynamiteCount:Int) = Action { implicit request =>
    Ok(views.html.rps_bots(yourName, opponentName, pointsToWin, maxRounds, dynamiteCount))
  }


  def sendData(person : RbsBots): Future[WSResponse] ={
    val url1=s"$player1/start"
    val url2=s"$player2/start"
    ws.url(url1).post(Json.toJson(bot(person.yourName, person.pointsToWin, person.maxRounds, person.dynamiteCount))).flatMap { _ =>
       ws.url(url2).post(Json.toJson(bot(person.opponentName, person.pointsToWin, person.maxRounds, person.dynamiteCount))).map {
         response  => (response)
       }
    }
  }


  def getOpponentMove = Action.async { implicit request =>


    val url= s"$player1/move "

    val futureResult: Future[String] = ws.url(url).get().map {
      response =>
        (response.json).as[String]
    }
    futureResult.map{ res =>
      Ok(res)
    }
  }

  def dynamicMove(dynaCount : Int) = /*Action { implicit request =>

    Ok(if(dynaCount!=0) scala.util.Random.shuffle(List("ROCK", "PAPER", "SCISSORS", "DYNAMITE"/*, "WATERBOMB"*/)).head else scala.util.Random.shuffle(List("ROCK", "PAPER", "SCISSORS")).head)*/
    Action.async { implicit request =>

      val url="http://localhost:7500/move "

      val futureResult: Future[String] = ws.url(url).get().map {
        response =>
          (response.json).as[String]
      }
      futureResult.map{ res =>
        Ok(res)
      }
    }


  def lastOpponentMove = Action.async { implicit request =>
    Try(request.body.asJson) match {
      case Success(payload) =>

        val url1=s"$player1/move"
        val url2=s"$player2/move"

        val result = payload.map{ y =>
          val player1 = (y \ "player1").as[String]
          val player2 = (y \ "player2").as[String]

          for {
            res1 <- ws.url(url1).post(Json.toJson(Map("lastOpponentMove" -> player1)))
            res2 <- ws.url(url2).post(Json.toJson(Map("lastOpponentMove" -> player2)))
          } yield(res1,res2)

          player1 match {
            case x if (player2 == x) => "TIE"
            case "ROCK" if (player2 != "DYNAMITE" && player2 != "PAPER") => "WIN"
            case "PAPER" if (player2 != "DYNAMITE" && player2 != "SCISSORS") => "WIN"
            case "SCISSORS" if (player2 != "DYNAMITE" && player2 != "ROCK") => "WIN"
            case "DYNAMITE" if (player2 != "WATERBOMB") => "WIN"
            case "WATERBOMB" if (player2 != "ROCK" && player2 != "PAPER" && player2 != "SCISSORS") => "WIN"
            case _ => "LOST"
          }
        }

        Future.successful(Ok(result.getOrElse("")))

      case Failure(e) =>
        Future.successful(BadRequest(s"Invalid payload: $e"))
    }

  }

  def javascriptRoutes = Action { implicit request =>

    Ok(
      JavaScriptReverseRouter("jsRoutes")(
        routes.javascript.HomeController.getOpponentMove,
        routes.javascript.HomeController.dynamicMove,
        routes.javascript.HomeController.lastOpponentMove
      )
    ).as("text/javascript")
  }
}


