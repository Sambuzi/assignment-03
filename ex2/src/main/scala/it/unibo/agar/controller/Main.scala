package it.unibo.agar.controller

import it.unibo.agar.distributed._

import scala.io.StdIn
import scala.swing._

object Main:
  def main(args: Array[String]): Unit =
    if args.isEmpty then
      println("Run in default mode or open help menu? (Default: D, Help: H) ,then press enter to continue")
      val choice = StdIn.readLine().trim.toUpperCase

      choice match
        case "H" | "h" =>
          printHelp()
          System.exit(0)
        case "D" | "d" =>
          runDefault()
        case _ =>
          println("Not valid input. ")
          System.exit(1)

    else args(0).toLowerCase match
      case "server" =>
        GameServer.main(Array.empty)

      case "client" =>
        val name = if args.length > 1 then args(1) else askPlayerName()
        GameClient.main(Array(name))

      case "ai" =>
        val count = if args.length > 1 then args(1) else "4"
        AIClient.main(Array(count))

      case "all" =>
        runAll()

      case "default" =>
        runDefault()

      case _ =>
        println(s"Unknown mode: ${args(0)}")
        printHelp()
        System.exit(1)

  private def printHelp() : Unit =
    println("\nUsage: DistributedMain <mode> [options]")
    println("Modes:")
    println("  (no args)           - Start server + wait for client")
    println("  default             - Start default game: server + 4 ai + 1 human player")
    println("  server              - Start the game server only")
    println("  client <name>       - Start a player client with given name")
    println("  ai <count>          - Start specified number of AI players")
    println("  all                 - Start server + 4 AI + wait for client input (press ENTER to start)")
    println("\nRun from sbt shell to use commands, or run in default mode.")

  private def askPlayerName(): String =
    var inputName = ""
    val dialog: Dialog = new Dialog {
      title = "Agar.io - Player Name"
      modal = true

      val nameField: TextField = new TextField(20) {
        text = "Player1"
        requestFocusInWindow()
        selectAll()
      }

      val titleLabel = new Label("Enter your player name") {
        font = new Font("Arial", java.awt.Font.BOLD, 18)
        horizontalAlignment = Alignment.Center
      }

      val startButton = new Button(Action("Start game") {
        inputName = nameField.text.trim
        dispose()
      })
      val cancelButton = new Button(Action("Cancel") {
        inputName = ""
        dispose()
        System.exit(0)
      })
 
      contents = new BoxPanel(Orientation.Vertical) {
        contents += Swing.VStrut(10)
        contents += titleLabel
        contents += Swing.VStrut(10)
        contents += nameField
        contents += Swing.VStrut(10)
        contents += new FlowPanel(FlowPanel.Alignment.Center)(startButton, cancelButton)
        border = Swing.EmptyBorder(15, 20, 15, 20)
      }

      centerOnScreen()
      pack()
      peer.setAlwaysOnTop(true)
      peer.setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE)
    }

    dialog.open()
    if inputName.isEmpty then "Player1" else inputName

  private def runDefault() : Unit =
    println("Starting with default settings...")

    val serverThread = new Thread(() => GameServer.main(Array.empty))
    serverThread.setDaemon(true)
    serverThread.start()

    Thread.sleep(2000) // Wait for server to initialize
    
    val aiThread = new Thread(() => AIClient.main(Array("4")))
    aiThread.setDaemon(true)
    aiThread.start()

    val playerName = askPlayerName()
    GameClient.main(Array(playerName))

  private def runAll() : Unit =
    val serverThread = new Thread(() => GameServer.main(Array.empty))
    serverThread.start()

    Thread.sleep(2000) // Wait for server to initialize

    val aiThread = new Thread(() => AIClient.main(Array("4")))
    aiThread.start()

    println("Server and AI players started. Press enter to start a human player...")
    StdIn.readLine()
    val playerName = askPlayerName()
    GameClient.main(Array(playerName))