import scala.io.StdIn.readLine
import akka.actor.Actor
import akka.actor.ActorSystem
import akka.actor.Props
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.mutable.ListBuffer
import akka.actor.ActorRef
import Cases._
import java.net.ServerSocket
import scala.concurrent.duration._
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintStream
import data.MutableDLL


object Main extends App { 
    
  // System creation
  val system = ActorSystem("MainSystem")
  
  // Supervisor creation
  val rs = system.actorOf(Props[RoomSupervisor], "RoomSupervisor")
  val ps = system.actorOf(Props[PlayerSupervisor], "PlayerSupervisor")
  val am = system.actorOf(Props[ActivityManager], "ActivityManager")
  
  // Room creation
  rs ! CreateRooms
  
  // Player creation
  ps ! PlayerStart
  
  // NPCCreation
  Thread.sleep(1000) // wait for PlayerStart to initialize default room
  ps ! NPCStart
  
  // Inform admin that game is ready
  Thread.sleep(3000) // 3 seconds is safe duration to wait for
  println("Game ready.")
  
  // Networking
  Future { checkConnections() }
  
  def checkConnections(): Unit = {
    val port = 4000
    val ss = new ServerSocket(port)
    while (true) {
      val sock = ss.accept()
      val in = new BufferedReader(new InputStreamReader(sock.getInputStream))
      val out = new PrintStream(sock.getOutputStream)
      Future {  // Get unique name
        var nameV = false
        out.println(splashText+"\n")
        out.println("Welcome to MUDWAVE.\n")
        while(nameV == false) {
          out.println("What is your name?")
          val name = in.readLine()
          if(nameVerification(name)) {
            usedNames += name
            ps ! AddPlayer(name, in, out, sock)
            nameV = true
          }
          else out.println("Please pick a different name.")
        }
      }
    }
  }
  
  // Name verficiation system 
  val usedNames = new MutableDLL[String]()

  def nameVerification(name:String): Boolean = {
    if(name.contains(" ")) return false
    else if(usedNames.contains(name)) return false
    else true
  }
  
  def splashText = 
"""
███╗   ███╗██╗   ██╗██████╗ ██╗    ██╗ █████╗ ██╗   ██╗███████╗
████╗ ████║██║   ██║██╔══██╗██║    ██║██╔══██╗██║   ██║██╔════╝
██╔████╔██║██║   ██║██║  ██║██║ █╗ ██║███████║██║   ██║█████╗  
██║╚██╔╝██║██║   ██║██║  ██║██║███╗██║██╔══██║╚██╗ ██╔╝██╔══╝  
██║ ╚═╝ ██║╚██████╔╝██████╔╝╚███╔███╔╝██║  ██║ ╚████╔╝ ███████╗
╚═╝     ╚═╝ ╚═════╝ ╚═════╝  ╚══╝╚══╝ ╚═╝  ╚═╝  ╚═══╝  ╚══════╝
"""
}