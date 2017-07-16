package controllers

import java.util
import javax.inject._

import play.Routes
import play.api._
import play.api.i18n.MessagesApi
import play.api.mvc._
import forms.RbsBotsForms._
import models.{lastMove, RbsBots}
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
class HomeController @Inject() (ws: WSClient)(implicit val messagesApi: MessagesApi, context: ExecutionContext) extends Controller with i18n.I18nSupport {



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
                    successSub.opponentName,
                    successSub.pointsToWin,
                    successSub.maxRounds,
                    successSub.dynamiteCount))
                }
    )

  }

  def showRpsBot(opponentName: String, pointsToWin: Int, maxRounds:Int, dynamiteCount:Int) = Action { implicit request =>
    Ok(views.html.rps_bots(opponentName, pointsToWin, maxRounds, dynamiteCount))
  }


  def sendData(person : RbsBots): Future[WSResponse] ={
    val url="http://localhost:7400/start "
    ws.url(url).post(Json.toJson(person)).map {
     response =>
       (response)
   }
  }


  def move = Action.async { implicit request =>

    val url="http://localhost:7400/move "

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

        val url="http://localhost:7400/move "
        ws.url(url).post(Json.toJson(payload)).map {
          response =>
            val x =payload.map{ x =>
              val lastOpponentMove = (x \ "lastOpponentMove").as[String]
              val lastSystemMove = (x \ "lastSystemMove").as[String]

              lastOpponentMove match {
                case  x if(lastSystemMove==x) => "TIE"
                case "ROCK" if(lastSystemMove!="DYNAMITE" && lastSystemMove!="PAPER") => "WIN"
                case "PAPER" if(lastSystemMove!="DYNAMITE" && lastSystemMove!="SCISSORS") => "WIN"
                case "SCISSORS" if(lastSystemMove!="DYNAMITE" && lastSystemMove!="ROCK") => "WIN"
                case "DYNAMITE" if(lastSystemMove!="WATERBOMB") => "WIN"
                case "WATERBOMB" if(lastSystemMove!="ROCK" && lastSystemMove!="PAPER" && lastSystemMove!="SCISSORS")=> "WIN"
                case _ => "LOST"
              }
            }
            Ok(x.getOrElse(""))
        }
      case Failure(e) =>
        Future.successful(BadRequest(s"Invalid payload: $e"))
    }

  }

  def javascriptRoutes = Action { implicit request =>

    Ok(
      JavaScriptReverseRouter("jsRoutes")(
        routes.javascript.HomeController.move,
        routes.javascript.HomeController.lastOpponentMove
      )
    ).as("text/javascript")
  }
}


