import akka.actor.Props
import akka.actor.Actor
import akka.actor.ActorRef
import scala.collection.mutable.ListBuffer
import Cases._
import data.BSTMap

class RoomSupervisor extends Actor {
  
  // Enable supervisor access
  def ps = Main.ps
  
  var rooms = new BSTMap[String, ActorRef]((s1: String, s2: String) => s1.compareTo(s2))
  var exits = data.RoomGraph
  
  def receive = {
    case CreateRooms => roomCreation
    case RoomFetch(roomName) => sender ! RoomReturn(rooms(roomName))
    case PlayerChangesRoom(roomName, pname, player) => {
      val newRoom = rooms.get(roomName)
      newRoom match {
        case Some(r) => {
          player ! ChangeRoom(roomName, r)
          r ! AddPlayerToRoom(pname, player)
        }
        case None => player ! Output("What?")
      }
    }
    case NPCChangesRoom(roomName, npcname, npc) => {
      val newRoom = rooms.get(roomName)
      newRoom match {
        case Some(r) => {
          npc ! ChangeRoom(roomName, r)
          r ! AddNPCToRoom(npcname, npc)
        }
        case None => Nil
      }
    }
    // Shortest path
    case ShortestPath(location, destination) => {
      if(rooms.contains(destination)) sender ! Output(exits.shortestPath(location, destination, Set[String]()))
      else sender ! Output("That room does not exist!")
    }
    case AddExit(room, direction, destination) => exits.addExit(room, direction, destination)
    case _ => println("Unknown message sent to Room Supervisor.")
  }
  
  // Create all rooms
  def roomCreation: Unit = {
    val file = (xml.XML.loadFile("src/map.xml") \ "room")
    file.foreach(n=>createRoom(n)) // get rooms from map.xml. Only works when wrapped in Future (why???)

    // Room creation method
    def createRoom(n: xml.Node): Unit = {
      // Seperates xml doc into different elements
      val name = (n \ "@name").text
      val ref = (n \ "@ref").text
      val description = (n \ "description").text
      val items = (n \ "item").map { inode => Item(inode) }.to[ListBuffer]
      val exits = Exit((n \ "exits").text.split(","))
      // Tell RoomSupervisor to create room
      rooms += (ref -> context.actorOf(Props(new Room(name,ref,description,items,exits)), ref))
    }
    
    // Fetch exit info from each room
    for(r <- rooms) r._2 ! FetchExits
  }
}