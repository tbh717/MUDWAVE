import akka.actor.Props
import akka.actor.Actor
import akka.actor.ActorRef
import scala.collection.mutable.ListBuffer
import Cases._

class Room
    (val name: String, 
    val ref: String, 
    val description: String, 
    private var items: ListBuffer[Item], 
    val exits: Exit) 
    extends Actor {
  
  // Players & NPCs
  private var players = Map[String, ActorRef]()
  private var NPCs = Map[String, ActorRef]()
  
  // Enable supervisor access
  def ps = Main.ps
  def rs = Main.rs
  
  def receive = {
    // INFO
    case FetchExits => getExits // send room supervisor exit info
    case GetDescrip => sender! Output(printDescription())
    case TellPlayer(message, senderName, recipient) => {
      try players(recipient) ! Output(s"$senderName: $message") // Atempts to send message
      // Catches map exception if that player is not in the room
      catch { case nse: java.util.NoSuchElementException => sender ! Output("That player isn't in the room.") }
    }
    case PlayerSays(message, senderName) => printToAll(s"$senderName: $message")
    case PlayerList => sender ! Output(playerList)
    // MOVEMENT
    case PlayerMove(dir, name) => playerMove(dir, name, sender)
    case NPCMove(dir, name) => NPCMove(dir, name, sender)
    // ITEMS
    case PlayerGet(itemName, pname) => removeItem(itemName, pname, sender)
    case AddToRoom(item, pname) => addItem(item, pname)
    // COMBAT
    case AttemptKill(attackerName: String, victimName: String) => {
      players.get(victimName) match { // First checks player list for victim
        case Some(p) => startCombat(p) // Player found. Starts combat
        case None => { // No player found
          NPCs.get(victimName) match { // Checks NPCs for victim
            case Some(npc) => startCombat(npc) // NPC found with matching name
            case None => sender ! Output("That person isn't in the room.") // Neither player nor NPC found with name
          }
        }
      }
      def startCombat(victim: ActorRef): Unit = { // Method to start combat
        if(victim == sender) sender ! Output("You can't attck yourself!") // Checks if attacker is trying to attack themself
        else {
          // False = combat not initiated, true = combat initiated
          victim ! CombatInitiated(sender, attackerName, false) // Sends to victim
          sender ! CombatInitiated(victim, victimName, true) // Informs attacker
        }
      }
    }
    // PLAYER MANAGEMENT
    case AddPlayerToRoom(name,player) => {
      printToAll(s"$name enters the room.") // Inform players in room
      players += (name -> player) // Add player to room
    }
    case RemovePlayerFromRoom(player) => {
      printToAll(s"$player leaves the room.")
      players -= player
    }
    case PlayerSlain(player, killer) => {
      printToAll(s"$player has been slain by $killer.")
      players -= player
    }
    // NPC MANAGEMENT
    case AddNPCToRoom(name,player) => {
      players.foreach(p => p._2 ! Output(s"NPC $name enters the room.")) // Inform players in room
      NPCs += (name -> player) // Add NPC to room
    }
    case RemoveNPCFromRoom(npc) => {
      printToAll(s"NPC $npc leaves the room.")
      NPCs -= npc
    }
    case NPCSlain(npc, killer) => {
      printToAll(s"$npc has been slain by $killer.")
      NPCs -= name
    }
    case _ => println(s"Unknown message sent to room ${name}.")
  }
  
  def playerMove(dir: String, pname: String, player: ActorRef): Unit = {
    val exit = getDestination(dir)
    exit match {
      case "0" => player ! Output("There is nothing in that direction.")
      case _ => {
        rs ! PlayerChangesRoom(exit, pname, player)
        self ! RemovePlayerFromRoom(pname)
      }
    }
  }
  
  def playerFlee(dir: String, npcname: String, NPC: ActorRef): Unit = {
    val exit = getDestination(dir)
    exit match {
      case "0" => Nil // No need to send output to NPC
      case _ => {
        rs ! NPCChangesRoom(exit, npcname, NPC)
        self ! RemoveNPCFromRoom(npcname)
      }
    }
  }
  
  def NPCMove(dir: String, npcname: String, NPC: ActorRef): Unit = {
    val exit = getDestination(dir)
    exit match {
      case "0" => Nil // No need to send output to NPC
      case _ => {
        rs ! NPCChangesRoom(exit, npcname, NPC)
        self ! RemoveNPCFromRoom(npcname)
      }
    }
  }
  
  def getDestination(dest: String): String = {
    dest match {
      case "north" => exits.north
      case "south" => exits.south
      case "east" => exits.east
      case "west" => exits.west
      case "up" => exits.up
      case "down" => exits.down
    }
  }
  
  def getExits {
    val directions = List("north","south","east","west","up","down")
    for (dir <- directions) rs ! AddExit(ref, dir, getDestination(dir))
  }
  
  /* INVENTORY MANIPULATION */
  
  def fetchItem(itemName: String): Option[Item] = {
    return items.find {_.name.toLowerCase == itemName.toLowerCase}
  }
  
  def removeItem(itemName: String, pname: String, player: ActorRef): Unit = {
    val itemSearch = fetchItem(itemName)
    itemSearch match {
      case Some(i) => {
        items -= i
        player ! AddItem(i)
        // Inform room
        player ! Output(s"You have taken ${i.name}.") // Inform player
        (players-pname).foreach { p => p._2 ! Output(s"$pname has taken ${i.name}.") } // Inform room
      }
      case None => player ! Output("That item is not in this room.")
    }
  }
  
  def addItem(item: Item, pname: String): Unit = {
    items += item
    players.foreach { p => p._2 ! Output(s"$pname has dropped ${item.name}.") } // Inform room
  }
  
  /* INFO FETCH */
  
  // Parent method
  def printDescription(): String = "\n" + roomDescription + "\n\n" + itemDescription + "\n" + exitDescription
  
  // Children methods
  def roomDescription: String = s"You are in $name." + "\n" + description
  
  def itemDescription: String = {
    var id = ""
    if(items.length>0) items.foreach { i => id += s"${i.name}: ${i.description}. Damage: ${i.damage} Speed: ${i.speed}\n" }
    else id+="There's nothing of value here.\n"
    id
  }
  
  def exitDescription: String = {
    var ed = ""
    if (exits.north != "0") ed+=exits.north +" is to the north.\n"
    if (exits.south != "0") ed+=exits.south +" is to the south.\n"
    if (exits.east != "0") ed+=exits.east +" is to the east.\n"
    if (exits.west != "0") ed+=exits.west +" is to the west.\n"
    if (exits.up != "0") ed+=exits.up +" is up.\n"
    if (exits.down != "0") ed+=exits.down +" is down.\n"
    ed
  }
  
  def playerList:String = {
    var pl = "PLAYERS:\n"
    var npcl = ""
    players.foreach { p => pl+=(p._1+"\n") }
    if(NPCs.size != 0) {
      npcl += "NPCs:\n"
      NPCs.foreach { npc => npcl+=(npc._1+"\n") }
    } 
    npcl+pl
  }
  
  def printToAll(message: String) = players.foreach(p => p._2 ! Output(message))
  
}