import akka.actor.ActorRef
import java.io.PrintStream
import java.io.BufferedReader
import java.net.Socket

object Cases {
  
  // Player
  case object CheckInput // Checks input from timer set in ps
  case class Output(message: String) // Prints out information to player
  case class AddItem(item: Item) // Sent by room when player gets item
  case class PrintMessage(msg: String) // can send room description to player, to be printed for the player
  case class ChangeRoom(roomName: String, room: ActorRef) // Changes room for player
  case class PlayerSays(message: String, name: String)
  case class CombatInitiated(combatActor: ActorRef, combatName: String, started: Boolean) // start combat
  case object CombatVictory // Success in combat
  case object CombatLoss // Failure in combat. Death
  case class Strike(damage: Double, weapon: String) // Damage from being hit
  case class GiveItem(name: String, item: Item)
  case object End // done as message so that all other messages can be cleared before the actor stops
  
  // NPCs
  case object MoveAttempt // NPC
  
  // Room
  case object GetDescrip // Sends player sender description of room
  case object PlayerList // Sends player sender list of current players in room
  case class PlayerGet(itemName: String, pname: String) // Sent to room when player attempts to take something
  case class AddToRoom(itemName: Item, pname: String) // Adds item to room when dropped by player, send playername
  case class PlayerMove(dest: String, name: String) // Sent to room when player attempts to move
  case class PlayerFlee(dir: String, name: String) // Sent to room when player attempts to flee
  case class NPCMove(direction: String, name: String) // Sent to room when NPC attempts to move
  case class AddPlayerToRoom(name: String, player: ActorRef) // Adds player to room. Sent by rs
  case class AddNPCToRoom(name: String, player: ActorRef) // Adds NPC to room. Sent by rs
  case class RemovePlayerFromRoom(name: String) // Removes player from room. Sent by room to itself
  case class RemoveNPCFromRoom(name: String) // Removes NPC from room
  case class PlayerSlain(name: String, killer: String)
  case class NPCSlain(name: String, killer: String)
  case class TellPlayer(message: String, sender: String, recipient: String) // Delivers player's message
  case class AttemptKill(attackerName: String, victimName: String)
  case object FetchExits
  
  // rs
  case object CreateRooms // initializes the room creation method at start of game
  case class PlayerChangesRoom(roomName: String, pname: String, player: ActorRef) // fetches room from String, notifies that room and player
  case class NPCChangesRoom(roomName: String, pname: String, player: ActorRef)
  case class RoomFetch(roomName: String) // sent so that rs can fetch room from String name
  case class RoomReturn(room: ActorRef) // returns Room as ActorRef
  case class ShortestPath(current: String, destination: String)
  case class AddExit(room: String, direction: String, destination: String)
  
  // ps
  case class AddPlayer(name: String, in: BufferedReader, out: PrintStream, sock: Socket) // Sent from main to create new player
  case object PlayerStart // Sent by main
  case object NPCStart // Sent by main
  case class OutputAll(message: String) // Messages all players
  case class RemovePlayer(name: String)
  case class RemoveNPC(name: String)
  
  // am
  /* Case class for messages that ActivityManager will handle.
   * Message is the case class that will be sent to the recipient, and all data it will deliver.
   * Time is recorded in Int form, where 1 Int represents 100 milliseconds (or 0.1 seconds) */
  case class MessageQueue(message: Any, time: Int, sender: ActorRef, recipient: ActorRef)
  case class MessageFinal(message: Any, recipient: ActorRef)

}