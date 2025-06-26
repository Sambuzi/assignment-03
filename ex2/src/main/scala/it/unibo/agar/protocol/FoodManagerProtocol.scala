package it.unibo.agar.protocol

import akka.actor.typed.ActorRef
import it.unibo.agar.model.Food

sealed trait FoodManagerMessage
case class GenerateFood(count: Int) extends FoodManagerMessage
case class RemoveFood(foodId: String) extends FoodManagerMessage
case class GetAllFood(replyTo: ActorRef[FoodList]) extends FoodManagerMessage
case class FoodList(foods: Seq[Food])

