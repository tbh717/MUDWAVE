import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.actorRef2Scala
import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._
import Cases._

class NPC(
  val name:String, 
  private var inv: ListBuffer[Item], 
  private var room: ActorRef,
  private var HP: Double,
  private var weapon: Item
  ) extends Actor {
  
  // Enable supervisor access
  def ps = Main.ps // Player supervisor
  def rs = Main.rs // Room supervisor
  def am = Main.am // Activity manager
  
  val directions = List("north","south","east","west","up","down")
  val random = scala.util.Random
  
  // Combat variables
  var inCombat = false
  var combatant: ActorRef = _
  var combatantName: String = null
  // Combat loop
  implicit val ec = context.dispatcher
  val system = context.system
  var combatLoop:akka.actor.Cancellable = _
  
  def receive = {
    case MoveAttempt => if(!inCombat) room ! NPCMove(directions(random.nextInt(5)), name)
    case ChangeRoom(newRoomName, newRoom) => {
      room = newRoom // change room
    }
    case CombatInitiated(combatActor, combatName, started) => {
      if(!started) Thread.sleep(weapon.speed*100) // started should always be false
      inCombat = true
      combatant = combatActor
      combatantName = combatName
      self ! Output("Combat starts!")
      combat // Start combat method
    }
    case Strike(damage, weapon) => {
      if(inCombat) {
        HP -= damage // subtract damage
        if(HP < 0) HP = 0 // no negative HP
        combatant ! Output(s"You have struck ${name} for ${damage}! Their HP: ${HP}") // inform combatant of hit
        if(HP == 0) { // DEATH
          self ! CombatLoss
          combatant ! CombatVictory
        }
      }
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
    case End => context.stop(self)
    case MessageFinal(message, recipient) => recipient ! message
    case _ => println(s"Unknown message sent to NPC ${name}.")
  }
  
  def combat: Unit = {
    val random = scala.util.Random
    combatLoop = system.scheduler.schedule(0.seconds, (weapon.speed*100).millis)(
      am ! MessageQueue(Strike(
      math.floor((weapon.damage+((random.nextDouble-0.5)*(weapon.damage/3)))*10 max 0)/10, // formula for damage variance, rounds to tenths place
      // Variates around damage number. Variance is greater the higher the damage. Rounds to tens place
      weapon.name), weapon.speed, self, combatant))(ec)
  }
  
  def endCombat: Unit = {
    combatant = null
    inCombat = false
    combatLoop.cancel()
  }
  
  def death: Unit = {
    combatLoop.cancel()
    inv.foreach { i => combatant ! GiveItem(name, i) }
    inv.foreach { i => inv -= i }
    ps ! RemoveNPC(name)
    room ! NPCSlain(name, combatantName)
    self ! End
  }
  
}