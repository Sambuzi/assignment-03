package it.unibo.agar.view

import akka.actor.typed.ActorRef

import java.awt.Graphics2D
import scala.swing._
import scala.swing.Dialog._
import javax.swing.SwingUtilities

import it.unibo.agar.protocol.{MoveDirection, PlayerActorMessage, Respawn}
import it.unibo.agar.model.World
import it.unibo.agar.GameConfig._

class DistributedLocalView(playerId: String) extends MainFrame:
  private var world: World = World(WorldWidth, WorldHeight, Seq.empty, Seq.empty)
  private var playerActor: Option[ActorRef[PlayerActorMessage]] = None
  private var isActive = true

  // --- TIMER ---
  private var startTime: Long = System.currentTimeMillis()
  private var endTime: Long = 0L

  title = s"Agar.io - Local View ($playerId)"
  preferredSize = new Dimension(WindowSize, WindowSize)

  contents = new Panel:
    listenTo(mouse.moves)
    focusable = true
    requestFocusInWindow()

    override def paintComponent(g: Graphics2D): Unit =
      if isActive then
        val playerOpt = world.players.find(_.id == playerId)
        val (offsetX, offsetY) = playerOpt
          .map(p => (p.x - size.width / 2.0, p.y - size.height / 2.0))
          .getOrElse((0.0, 0.0))
        AgarViewUtils.drawWorld(g, world, offsetX, offsetY)
        // --- DISEGNA LO SCORE CON STILE ---
        playerOpt.foreach { player =>
          val scoreText = s"Score: ${player.score}"
          val font = new java.awt.Font("Arial", java.awt.Font.BOLD, 22)
          g.setFont(font)
          val metrics = g.getFontMetrics(font)
          val textWidth = metrics.stringWidth(scoreText)
          val textHeight = metrics.getHeight
          val padding = 10
          val x = 15
          val y = 35

          // Sfondo semi-trasparente arrotondato
          val bgColor = new java.awt.Color(255, 255, 255, 180)
          g.setColor(bgColor)
          g.fillRoundRect(x - padding, y - textHeight + 5, textWidth + 2 * padding, textHeight + padding / 2, 18, 18)

          // Bordo scuro
          g.setColor(java.awt.Color.DARK_GRAY)
          g.drawRoundRect(x - padding, y - textHeight + 5, textWidth + 2 * padding, textHeight + padding / 2, 18, 18)

          // Testo in blu scuro
          g.setColor(new java.awt.Color(30, 60, 180))
          g.drawString(scoreText, x, y)
        }
      else
        g.setColor(java.awt.Color.DARK_GRAY)
        g.fillRect(0, 0, size.width, size.height)
        g.setColor(java.awt.Color.WHITE)
        g.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 24))
        g.drawString("Waiting for respawn...", size.width / 2 - 100, size.height / 2)

    reactions += { case e: event.MouseMoved =>
      if isActive then
        playerActor.foreach: actor =>
          val dx = (e.point.x - size.width / 2.0) * 0.01
          val dy = (e.point.y - size.height / 2.0) * 0.01
          actor ! MoveDirection(dx, dy)
    }
  open()

  def getPlayerId: String = playerId
  
  def updateWorld(world: World): Unit =
    this.world = world
    repaint()

  def setPlayerActor(actor: ActorRef[PlayerActorMessage]): Unit =
    playerActor = Some(actor)

  def setActive(active: Boolean): Unit =
    isActive = active
    repaint()

  // --- CHIAMA QUESTO QUANDO IL PLAYER INIZIA O RESPWNA ---
  def onPlayerStart(): Unit =
    startTime = System.currentTimeMillis()

  def showMatchResultsAndRespawn(): Unit =
    SwingUtilities.invokeLater(() => {
      isActive = false
      repaint()

      endTime = System.currentTimeMillis()
      val playerOpt = world.players.find(_.id == playerId)
      val score = playerOpt.map(_.score).getOrElse(0)
      val foodEaten = score
      val cellsEaten = 0 // migliora se vuoi

      // Calcolo tempo di gioco
      val timeAliveMillis = (endTime - startTime).max(0)
      val seconds = (timeAliveMillis / 1000) % 60
      val minutes = (timeAliveMillis / 1000) / 60
      val timeAlive = f"$minutes%02d:$seconds%02d"

      val titleLabel = new Label("Match Results") {
        font = new Font("Arial", java.awt.Font.BOLD, 20)
        horizontalAlignment = Alignment.Center
      }
      val foodLabel = new Label(s"Food eaten: $foodEaten") {
        font = new Font("Arial", java.awt.Font.PLAIN, 14)
        horizontalAlignment = Alignment.Center
      }
      val cellsLabel = new Label(s"Cells eaten: $cellsEaten") {
        font = new Font("Arial", java.awt.Font.PLAIN, 14)
        horizontalAlignment = Alignment.Center
      }
      val scoreLabel = new Label(s"Score: $score") {
        font = new Font("Arial", java.awt.Font.PLAIN, 14)
        horizontalAlignment = Alignment.Center
      }
      val timeLabel = new Label(s"Time alive: $timeAlive") {
        font = new Font("Arial", java.awt.Font.PLAIN, 14)
        horizontalAlignment = Alignment.Center
      }
      val respawnLabel = new Label("Do you want to respawn?") {
        font = new Font("Arial", java.awt.Font.BOLD, 13)
        horizontalAlignment = Alignment.Center
      }

      val resultPanel = new BoxPanel(Orientation.Vertical) {
        contents += Swing.VStrut(10)
        contents += titleLabel
        contents += Swing.VStrut(15)
        contents += foodLabel
        contents += cellsLabel
        contents += scoreLabel
        contents += timeLabel
        contents += Swing.VStrut(15)
        contents += respawnLabel
        contents += Swing.VStrut(10)
      }

      val yesButton = new Button("Respawn")
      val noButton = new Button("Exit")

      val buttonPanel = new FlowPanel(FlowPanel.Alignment.Center)(yesButton, noButton)

      val mainPanel = new BoxPanel(Orientation.Vertical) {
        contents += resultPanel
        contents += buttonPanel
        border = Swing.EmptyBorder(10, 20, 10, 20)
      }

      val dialog = new Dialog {
        title = "Match Results"
        contents = mainPanel
        size = new Dimension(320, 300)
        centerOnScreen()
        modal = true
        peer.setAlwaysOnTop(true)
        peer.setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE)
      }

      listenTo(yesButton, noButton)

      reactions += {
        case event.ButtonClicked(`yesButton`) =>
          dialog.close()
          onPlayerStart() // resetta il timer!
          playerActor.foreach(_ ! Respawn)
        case event.ButtonClicked(`noButton`) =>
          dialog.close()
          dispose()
          System.exit(0)
      }

      dialog.open()
    })

  override def dispose(): Unit =
    isActive = false
    playerActor = None
    super.dispose()