package it.unibo.agar.protocol

import akka.actor.typed.ActorRef
import it.unibo.agar.model.{Player, World, Food}

sealed trait WorldManagerMessage
case class RegisterPlayer(playerId: String, ref: ActorRef[PlayerRegistered]) extends WorldManagerMessage
case class UnregisterPlayer(playerId: String) extends WorldManagerMessage
case class GetWorldState(replyTo: ActorRef[WorldState]) extends WorldManagerMessage
case class UpdatePlayerPosition(playerId: String, x: Double, y: Double) extends WorldManagerMessage
case class AteFood(playerId: String, foodId: String) extends WorldManagerMessage
case class AtePlayer(killerId: String, victimId: String) extends WorldManagerMessage
case object Tick extends WorldManagerMessage

case class WorldState(world: World) extends WorldManagerMessage
case class PlayerRegistered(player: Player) extends WorldManagerMessage
case class UpdateFood(foods: Seq[Food]) extends WorldManagerMessage
case object NoOp extends WorldManagerMessage
case object InitializeFoodManager extends WorldManagerMessage
case class PlayersUpdate(player: Set[ActorRef[PlayerActorMessage]]) extends WorldManagerMessage
case object ShuttingDown extends WorldManagerMessage
