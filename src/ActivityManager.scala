import akka.actor.Actor
import data.MUDPQHeap
import Cases._
import scala.concurrent.duration._

class ActivityManager extends Actor {
  
  // Queue
  var pq = new MUDPQHeap[MessageQueue](_.time<_.time)
  // Loop
  implicit val ec = context.dispatcher
  val system = context.system
  // Time loop
  private var time = 0 // 1 time = 0.1 seconds
  val countLoop = system.scheduler.schedule(0.seconds, 100.millis)(time+=1)(ec)
  // CheckQueue Loop
  val queueLoop = system.scheduler.schedule(0.seconds, 100.millis)(checkQueue())(ec)
  
  def receive = {
    case MessageQueue(message, mtime, sender, recipient) => {
      pq.enqueue(new MessageQueue(message,time+mtime,sender,recipient)) // Queues new message
      printMessage(new MessageQueue(message,time+mtime,sender,recipient))
    }
    case _ => println("Unknown message sent to Activity Manager.")
  }
  
  def checkQueue(): Unit = {
    if(!pq.isEmpty) {
      val check = pq.peek
      var message: MessageQueue = null
      if(check.time<=time) {
        message = pq.dequeue
        message.sender ! MessageFinal(message.message, message.recipient)
      }
    }
  }
  
  // For testing
  def printMessage(m: MessageQueue) = {
    println(s"${m.message} sent by ${m.sender} to ${m.recipient}")
    println(s"It is $time and the message will be dequeued at ${m.time}")
  }
  
}