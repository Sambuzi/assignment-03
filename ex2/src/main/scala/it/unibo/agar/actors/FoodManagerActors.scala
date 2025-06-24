package it.unibo.agar.actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.*
import com.typesafe.config.Config

import scala.util.Random
import it.unibo.agar.model.Food
import it.unibo.agar.distributed.{FoodList, FoodManagerMessage, GenerateFood, GetAllFood, RemoveFood}
import it.unibo.agar.GameConfig._

object FoodManagerActor:
  def apply(config: Config) : Behavior[FoodManagerMessage] =
    Behaviors.setup: context =>
      Behaviors.withTimers: timers =>
        val width = WorldWidth
        val height = WorldHeight
        val maxFood = MaxFood
        var foodList = (1 to maxFood).map(i => Food(s"f$i", Random.nextInt(width), Random.nextInt(height)))

        var foodCounter = maxFood
        
        timers.startTimerAtFixedRate(GenerateFood(10), TwoSeconds)
        
        Behaviors.receiveMessage:
          case GenerateFood(count) =>
            if foodList.size < maxFood then
              val newFoods = (1 to math.min(count, maxFood - foodList.size)).map: _ =>
                foodCounter += 1
                Food(s"f$foodCounter", Random.nextInt(width), Random.nextInt(height))
              foodList = foodList ++ newFoods
            Behaviors.same

          case RemoveFood(foodId) =>
            foodList = foodList.filterNot(_.id == foodId)
            Behaviors.same

          case GetAllFood(replyTo) =>
            replyTo ! FoodList(foodList)
            Behaviors.same