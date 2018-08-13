package data

import scala.collection.mutable.ListBuffer
import scala.collection.mutable.Map

object RoomGraph {
  val rooms = ListBuffer[Room]()
  
  case class Exit(direction: String, destination: Room)
  
  class Room(name: String) {
    def roomName = name
    var exits = ListBuffer[Exit]()
  }
  
  def addExit(room: String, direction: String, destination: String) {
    if(destination != "0") {
      roomFind(room) match {
        case Some(r) => { // Finds room
          roomFind(destination) match {
            case Some(d) => { // Finds room & destination
              r.exits += Exit(direction, d)
            }
            case None => { // Finds room but not destination
              rooms += new Room(destination)
              r.exits += Exit(direction, new Room(destination))
            }
          }
        }
        case None => { // Does not find room
          roomFind(destination) match {
            case Some(d) => { // Does not find room, finds destination
              val newRoom = new Room(room)
              newRoom.exits += Exit(direction, d)
              rooms += newRoom
            }
            case None => { // Does not find room or destination
              val newRoom = new Room(room)
              val newDestination = new Room(destination)
              newRoom.exits += Exit(direction,newDestination)
              rooms += newDestination
              rooms += newRoom
            }
          }
        }
      }
    }
  }
  
  def roomFind(room: String): Option[Room] = rooms.find(r => room == r.roomName)
  
  def printMap {
    rooms.foreach(r => {
      println(s"Room: ${r}.")
      r.exits.foreach(e => println(s"Exit: ${e.destination} is ${e.direction}."))
    })
  }
  
  def shortestPath(current: String, destination: String, visited: Set[String]): String = {
    val newVisited = visited+current
    val currentRoom = roomFind(current).get
    if(current == destination) "Found!"
    else {
      val paths = for(e <- currentRoom.exits; if(!visited(e.destination.roomName))) yield {
        s"Go ${e.direction} to ${e.destination.roomName}.:" + shortestPath(e.destination.roomName, destination, newVisited)
      }
      if(!paths.isEmpty) {
        paths.filter(s => s.contains("Found!")).map(d => d.split(":")).minBy(p => p.length).mkString(" ")
      } else ""
    }
  }
  
}