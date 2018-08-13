package data

class MUDPQSLL[A: Manifest](lt:(A,A) => Boolean) extends PriorityQueue[A] {
  
  private class Node(val data: A, var prev: Node, var next: Node)
  private val end = new Node(new Array[A](1)(0), null, null)
  end.next = end
  end.prev = end
  
  def enqueue(o: A) {
    var rover = end.prev
    while(rover != end && lt(rover.data,o)) rover = rover.prev
    val n = new Node(o, rover, rover.next)
    rover.next.prev = n
    rover.next = n
  }
  
  def dequeue(): A = {
    val ret = end.next.data
    end.next = end.next.next
    end.next.prev = end
    ret
  }
  
  def isEmpty: Boolean = end.next == end
  
  def peek: A = end.next.data
  
}