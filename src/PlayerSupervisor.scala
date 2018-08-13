import akka.actor.Actor
import scala.collection.mutable.ListBuffer
import akka.actor.ActorRef
import akka.actor.Props
import scala.collection.mutable.Map
import java.io.BufferedReader
import java.io.PrintStream
import Cases._
import java.io.InputStreamReader
import scala.concurrent.duration._
import java.net.Socket

class PlayerSupervisor extends Actor {
  
  // Data for players and NPCs. name -> ActorRef
  private var players = Map[String, ActorRef]() 
  private var NPCs = Map[String, ActorRef]()
  
  // Program loop
  implicit val ec = context.dispatcher
  val system = context.system
  val pLoop = system.scheduler.schedule(0.seconds, 1000.millis)(players.foreach(p => p._2 ! CheckInput))(ec)
  // Tells NPC to attempt to move in a random direction every 5 seconds
  val npcLoop = system.scheduler.schedule(0.seconds, 5000.millis)(NPCs.foreach(npc => npc._2 ! MoveAttempt))(ec)
  
  // Enable supervisor access
  def rs = Main.rs
  def am = Main.am
  
  def receive = {
    case AddPlayer(name, in, out, sock) => playerCreation(name, in, out, sock)
    case RoomReturn(room) => {
      startRoom = room
    }
    case PlayerStart => rs ! RoomFetch("cafe") // Sets start room
    case NPCStart => NPCCreation
    case RemovePlayer(player) => players -= player
    case RemoveNPC(npc) => NPCs -= npc
    case _ => println("Unknown message sent to Player Supervisor.")
  }
  
  // NPC creation
  def NPCCreation: Unit = {
    val file = (xml.XML.loadFile("src/NPCs.xml") \ "NPC")
    file.foreach(n=>createNPC(n))
    def createNPC(n: xml.Node) {
      val name = (n \ "@name").text
      val inv = (n \ "item").map { inode => Item(inode) }.to[ListBuffer]
      val HP = (n \ "@HP").text.toDouble
      val weapon = new Item(
          (n \ "weapon" \ "@name").text, 
          (n \ "weapon").text, // description
          (n \ "weapon" \ "@damage").text.toDouble, // damage
          (n \ "weapon" \ "@speed").text.toInt) // speed
      NPCs += (name -> context.actorOf(Props(new NPC(name,inv,startRoom,HP,weapon)), name))
    }
  }
  
  // Player creation method
  def playerCreation(name:String, in: BufferedReader, out: PrintStream, sock: Socket): Unit = {
    val newPlayer = (name -> context.actorOf(Props(new Player(
        name,
        startInv,
        startRoom,
        startHP,
        in,
        out,
        sock
        )), name))
    players += newPlayer // Add newly created player to ps list
    startRoom ! AddPlayerToRoom(newPlayer._1, newPlayer._2) // Add newly created player to room
    newPlayer._2 ! Output("Welcome to the world.")
  }
  
  // Default player creation vals
  private var startRoom: ActorRef = _
  private var startHP = 100.0
  val startInv = ListBuffer[Item]( // initiates inv w/ test items
      new Item("Soylent blue","Unnaturally colored nutrition",0.0,0),
      new Item("Flashlight","Primitive light projecting device",0.0,0),
      new Item("Flashlight","Primitive light projecting device",0.0,0))
}