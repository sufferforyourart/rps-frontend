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
                  successSub.player1Name,
                  successSub.player2Name,
                  successSub.pointsToWin,
                  successSub.maxRounds,
                  successSub.dynamiteCount,
                  successSub.player1url,
                  successSub.player2url))
              }
    )

  }


  def sendData(person : RbsBots): Future[WSResponse] ={
    if(person.player1url.isDefined && person.player2url.isDefined){
      val url1=s"${person.player1url.get}/start"
      val url2=s"${person.player2url.get}/start"
      ws.url(url1).post(Json.toJson(bot(person.player1Name, person.pointsToWin, person.maxRounds, person.dynamiteCount))).flatMap { _ =>
        ws.url(url2).post(Json.toJson(bot(person.player2Name, person.pointsToWin, person.maxRounds, person.dynamiteCount))).map {
          response  => (response)
        }
      }
    } else{
      val url2=s"${person.player1url.getOrElse(person.player2url.get)}/start"
      ws.url(url2).post(Json.toJson(bot(if(person.player1url.isDefined){person.player1Name} else {person.player2Name}, person.pointsToWin, person.maxRounds, person.dynamiteCount))).map {
        response  => (response)
      }
    }
  }


  def getPlayer1Move(dynaCount : Int, player1url: String) = Action.async { implicit request =>


    if(player1url!="no") {
      val url = s"${player1url}/move"
      val futureResult: Future[String] = ws.url(url).get().map {
        response =>
          (response.json).as[String]
      }
      futureResult.map{ res =>
        if(dynaCount<=0 && res == "DYNAMITE"){
          Ok("WATERBOMB")
        } else {
          Ok(res)
        }
      }
    } else {
      Future.successful(
        Ok(if(dynaCount!=0) scala.util.Random.shuffle(List("ROCK", "PAPER", "SCISSORS", "DYNAMITE", "WATERBOMB")).head else scala.util.Random.shuffle(List("ROCK", "PAPER", "SCISSORS")).head)
      )
    }

  }

  def getPlayer2Move(dynaCount : Int, player2url: String) = Action.async { implicit request =>

    if(player2url!="no") {
      val url = s"${player2url}/move"
      val futureResult: Future[String] = ws.url(url).get().map {
        response =>
          (response.json).as[String]
      }
      futureResult.map{ res =>
        if(dynaCount<=0 && res == "DYNAMITE"){
          Ok("WATERBOMB")
        } else {
          Ok(res)
        }
      }
    } else {
      Future.successful(
        Ok(if(dynaCount!=0) scala.util.Random.shuffle(List("ROCK", "PAPER", "SCISSORS", "DYNAMITE", "WATERBOMB")).head else scala.util.Random.shuffle(List("ROCK", "PAPER", "SCISSORS")).head)
      )
    }
  }


  def lastOpponentMove = Action.async { implicit request =>
    Try(request.body.asJson) match {
      case Success(payload) =>

        val result = payload.map{ y =>
          val player1 = (y \ "player1").as[String]
          val player2 = (y \ "player2").as[String]
          val url1= (y \ "player1url").as[String]
          val url2= (y \ "player2url").as[String]

          for {
            res1 <- if(url1!="no"){ws.url(s"$url1/move").post(Json.toJson(Map("lastOpponentMove" -> player2)))} else Future(url1)
            res2 <- if(url2!="no"){ws.url(s"$url2/move").post(Json.toJson(Map("lastOpponentMove" -> player1)))} else Future(url2)
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
        routes.javascript.HomeController.getPlayer1Move,
        routes.javascript.HomeController.getPlayer2Move,
        routes.javascript.HomeController.lastOpponentMove
      )
    ).as("text/javascript")
  }
}


