import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.actorRef2Scala
import Cases._
import scala.collection.mutable.ListBuffer
import java.io.BufferedReader
import java.io.PrintStream
import java.net.Socket
import scala.concurrent.duration._
import scala.concurrent.Future
import Colors._

class Player(
  val name:String, 
  private var inv: ListBuffer[Item], 
  private var room: ActorRef,
  private var HP: Double,
  val in: BufferedReader,
  val out: PrintStream,
  sock: Socket
  ) extends Actor {
  
  // Non-constructor vars
  var roomName: String = "cafe" // Starting room

  // Enable supervisor access
  def ps = Main.ps // Player supervisor
  def rs = Main.rs // Room supervisor
  def am = Main.am // Activity manager
  
  // Combat variables
  var inCombat = false
  var combatant: ActorRef = null
  var combatantName: String = null
  // Default weapon
  val hands = new Item("hands", "Can be used for fighting.",5.0,30)
  var activeWeapon = hands
  // Combat loop
  implicit val ec = context.dispatcher
  val system = context.system
  var combatLoop:akka.actor.Cancellable = _
  
  // Actor message interpreter
  def receive = {
    case Output(message) => {
      out.print(Console.RESET+message+"\n")
      out.print(Console.GREEN) // makes input green
    }
    case ChangeRoom(newRoomName, newRoom) => {
      room = newRoom // change room
      roomName = newRoomName
      room ! GetDescrip // get description of new room
    }
    case AddItem(item: Item) => addToInventory(item)
    case CheckInput => if(in.ready()) processCommand(in.readLine())
    case MessageFinal(message, recipient) => recipient ! message // For ActivityManager messages
    // Combat
    case CombatInitiated(combatActor, combatName, started) => {
      if(started) self ! Output(s"You attack ${combatName}!") else {
        self ! Output(s"You are attacked by ${combatName}!")
        Thread.sleep(activeWeapon.speed*100) // Delay. Attacker gets the jump
      }
      inCombat = true
      combatant = combatActor
      combatantName = combatName
      self ! Output("Combat starts!")
      combat // Start combat method
    }
    case CombatVictory => {
      self ! Output(s"You defeated ${combatantName}!")
      endCombat
    }
    case CombatLoss => {
      self ! Output(s"You were defeated by ${combatantName}!")
      combatant ! Output(s"You have slain ${name}!")
      death
    }
    case GiveItem(name, item) => self ! Output(s"${name} gives you ${item.name}.")
    case Strike(damage, weapon) => {
      if(inCombat) {
        HP -= damage // subtract damage
        if(HP < 0) HP = 0 // no negative HP
        self ! Output(s"${combatantName} hits you for ${damage} damage with their ${weapon}! HP: ${HP}") // inform self of hit
        combatant ! Output(s"You have struck ${name} for ${damage}! Their HP: ${HP}") // inform combatant of hit
        if(HP == 0) { // DEATH
          self ! CombatLoss
          combatant ! CombatVictory
        }
      }
    }
    case MessageFinal(message, recipient) => recipient ! message // Sent my activity manager
    case End => {
      context.stop(self)
      sock.close()
    }
    case _ => println(s"Unknown message sent to player ${name}.")
  }
  
  /* COMMAND PROCESSING */
  
  def processCommand(c: String): Unit = {
    
    // String parsing
    var command = c.toLowerCase() // assigns command input to mutable value
    var mod = "" // initializes empty modifier following command
    if(c.indexOf(" ") > -1) { // checks to see if there is a space in the command
      val commandSplit = c.split(" ", 2) // splits command on space separator into array of 2: 0 = command, 1 = modifier
      command = commandSplit(0) // Assigns original command back to command variable
      mod = commandSplit(1) } // assigns modifier to mod variable
    
    /* Command-handling */
    
    // In combat
    if(inCombat) command match {
      case "flee" => attemptFlee
      case "equip" => equip(mod)
      case "unequip" => {
        out.println(s"You unequip your ${activeWeapon.name}")
        activeWeapon = hands
      }
      case _ => self ! Output("You can't do that in combat.")  
    }
    
    // Peaceful
    else command match { 
      case "north"|"south"|"east"|"west"|"up"|"down" => move(command)
      case "shortestPath"|"sp" => {
        if(mod == roomName) self ! Output("You are already in that room!")
        else rs ! ShortestPath(roomName, mod)
      }
      case "look" => room ! GetDescrip
      case "inv"|"inventory" => self ! Output(printInventory)
      case "get"|"take" => room ! PlayerGet(mod, name)
      case "drop" => self ! dropItem(mod)
      case "players" => room ! PlayerList
      case "tell"|"message" => {
        val modSplit = mod.split(" ", 2) // 0 = name, 1 = message
        room ! TellPlayer(modSplit(1),name,modSplit(0))
        println(s"Message: ${modSplit(1)}, Sender: $name, Recipient: ${modSplit(0)}")
      }
      case "say" => room ! PlayerSays(mod, name)
      case "equip" => equip(mod)
      case "unequip" => {
        if(activeWeapon != hands) {
          inv += activeWeapon
          self ! Output(s"You unequip your ${activeWeapon.name}")
          activeWeapon = hands
        }
        else self ! Output(s"You have nothing to unequip.")
      }
      case "weapon" => self ! Output(s"You currently have ${activeWeapon.name} equipped.\n")
      case "health"|"HP" => self ! Output(s"You currently have ${HP} health points.")
      case "stats" => self ! Output(s"Your weapon does ${activeWeapon.damage} damage every ${activeWeapon.speed/10.0} seconds.")
      case "kill"|"attack" => room ! AttemptKill(name, mod)
      case "exit" => exit
      case "help" => self ! Output(help)
      case _ => self ! Output("Please enter a valid command.") // invalid command
    }
  }
  
  /* INVENTORY MANIPULATION */
  def fetchItem(itemName: String): Option[Item] = return inv.find {_.name.toLowerCase == itemName.toLowerCase}
  
  def dropItem(itemName: String): Unit = {
  fetchItem(itemName) match {
      case Some(item) => {
        inv -= item
        self ! Output(s"${item.name} dropped.")
        room ! AddToRoom(item, name)
      }
      case None => self ! Output("You do not have that item in your inventory.")
    }
  }
  
  def addToInventory(item: Item): Unit = {
    inv += item
  }

  def printInventory(): String = {
    var pi = ""
    pi += s"You currently have ${activeWeapon.name} equipped.\n" // Equipped weapon
    if(inv.length>0) { // If inv contains items, iterate through inv and print each one
      pi += "INVENTORY: \n"
      inv.foreach(i => pi+=s"${i.name}: ${i.description}. Damage: ${i.damage} Speed: ${i.speed}\n")
    }
    else pi=("You have nothing to your name. Get yourself together.") // Inv empty
    pi
  }
  
  def move(dir: String) = room ! PlayerMove(dir, name)
  
  /* COMBAT METHODS */
  
  def combat: Unit = {
    val random = scala.util.Random
    combatLoop = system.scheduler.schedule(0.seconds, (activeWeapon.speed*100).millis)(
      am ! MessageQueue(Strike(
      math.floor((activeWeapon.damage+((random.nextDouble-0.5)*(activeWeapon.damage/3)))*10 max 0)/10, // formula for damage variance
      // Variates around damage number. Variance is greater the higher the damage. Rounds to tens place
      activeWeapon.name), activeWeapon.speed, self, combatant))(ec)
  }
  
  def equip(itemName: String): Unit = {
    fetchItem(itemName) match {
      case Some(i) => { 
        activeWeapon = i
        inv -= i
        self ! Output(s"You equip the ${i.name}.")
      }
      case None => self ! Output("You don't have anything like that.")
    }
  }
  
  def attemptFlee(): Unit = {
    val directions = List("north","south","east","west","up","down")
    val random = scala.util.Random
    var randomDir = directions(random.nextInt(5))
    self ! Output(s"You attempt to flee to the ${randomDir}!")
    room ! PlayerFlee(randomDir, name)
  }
  
  def endCombat: Unit = {
    combatant = null
    inCombat = false
    combatLoop.cancel()
  }
  
  def death: Unit = {
    combatLoop.cancel()
    inv += activeWeapon
    inv.foreach { i => combatant ! GiveItem(name, i) }
    inv.foreach { i => inv -= i }
    room ! PlayerSlain(name, combatantName) // inform room that player has been killed
    self ! Output("You have died!")
    exit
  }
  
  def exit: Unit = {
    out.print("Goodbye!")
    out.print(" "+Console.RESET) // Reset console color
    ps ! RemovePlayer(name) // inform player manager that player has been killed
    self ! End
  }
  
  // Print vals
  def help =
"""
Here is what you can do, with its corresponding (command) and, if it has one, [modifier]:
1.) Move in a direction: (north|south|east|west|up|down)
2.) Find the shortest path to a location: (shortestPath|sp) [location]
3.) Examine a room, its contents, and its exits: (look)
4.) Look inside your inventory: (inv | inventory)
5.) Pick up an object in the room: (get | take) [item name]
6.) Drop an item from your inventory in the room: (drop) [item name]
7.) Get a list of players: (players)
8.) Tell another player something: (tell | message) [message]
9.) Say something to every player in the room: (say) [message]
10.) Check current weapon: (weapon)
11.) Equip weapon: (equip) [weapon]
12.) Unequip weapon to default fists: (unequip)
13.) Check your current health: (health | HP)
13.) Check your current weapon damage: (stats)
13.) Initiate combat with a player: (kill | attack) [player name]
14.) Leave the game: (exit)
"""

}