package it.unibo.agar.view

import it.unibo.agar.model.World
import it.unibo.agar.GameConfig._

import scala.swing._
import java.awt.Graphics2D

class GlobalViewFrame extends MainFrame:
  private var world: World = World(WorldWidth, WorldHeight, Seq.empty, Seq.empty)

  title = "Agar.io Global View"
  preferredSize = new Dimension(WindowSize, WindowSize)

  contents = new Panel:
    override def paintComponent(g: Graphics2D): Unit =
      AgarViewUtils.drawWorld(g, world)

  open()

  def updateWorld(newWorld: World): Unit =
    world = newWorld
    repaint()