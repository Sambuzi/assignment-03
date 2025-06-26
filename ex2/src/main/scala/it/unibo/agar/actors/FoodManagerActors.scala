package it.unibo.agar.actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.*
import com.typesafe.config.Config
import scala.util.Random
import it.unibo.agar.model.Food
import it.unibo.agar.GameConfig._
import it.unibo.agar.protocol._

/**
 * Actor responsible for managing food in the game world.
 * - Periodically generates new food up to a maximum.
 * - Handles requests to remove food and to get the current food list.
 *
 * @param config Akka/Typesafe configuration (not used directly here, but available for future extensions)
 */
object FoodManagerActor:

  /**
   * Creates the FoodManagerActor behavior.
   * @param config Configuration object
   * @return Akka Behavior for FoodManagerMessage
   */
  def apply(config: Config): Behavior[FoodManagerMessage] = Behaviors.setup { context =>
    Behaviors.withTimers { timers =>
      // Initial food list
      var foodList = Vector.tabulate(MaxFood)(i => Food(s"f$i", Random.nextInt(WorldWidth), Random.nextInt(WorldHeight)))
      var foodCounter = MaxFood

      // Periodically generate new food every TwoSeconds
      timers.startTimerAtFixedRate(GenerateFood(10), TwoSeconds)

      Behaviors.receiveMessage {
        /**
         * Handles periodic or explicit food generation.
         * Adds up to 'count' new food items, but never exceeds MaxFood.
         */
        case GenerateFood(count) if foodList.size < MaxFood =>
          val toAdd = math.min(count, MaxFood - foodList.size)
          val newFoods = Vector.tabulate(toAdd) { _ =>
            foodCounter += 1
            Food(s"f$foodCounter", Random.nextInt(WorldWidth), Random.nextInt(WorldHeight))
          }
          foodList ++= newFoods
          Behaviors.same

        /**
         * Removes a food item by its id.
         */
        case RemoveFood(foodId) =>
          foodList = foodList.filterNot(_.id == foodId)
          Behaviors.same

        /**
         * Replies with the current list of food.
         */
        case GetAllFood(replyTo) =>
          replyTo ! FoodList(foodList)
          Behaviors.same

        case _ => Behaviors.same
      }
    }
  }