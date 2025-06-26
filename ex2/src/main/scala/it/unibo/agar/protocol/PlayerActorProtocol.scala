package it.unibo.agar.protocol

import it.unibo.agar.model.World

sealed trait PlayerActorMessage
case class MoveDirection(dx: Double, dy: Double) extends PlayerActorMessage
case class UpdateView(world: World) extends PlayerActorMessage
case class PlayerDied(playerId: String) extends PlayerActorMessage
case object StartAI extends PlayerActorMessage
case object Respawn extends PlayerActorMessage

// AGGIUNGI QUI i messaggi interni:
case class InitializeComplete(player: it.unibo.agar.model.Player) extends PlayerActorMessage
case object RegistrationFailed extends PlayerActorMessage
case class ViewUpdateFailed(error: Throwable) extends PlayerActorMessage