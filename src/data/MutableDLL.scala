package data

import scala.collection.mutable

class MutableDLL[A] extends mutable.Buffer[A] {
  
  private var default: A = _
  private class Node(var data: A, var prev: Node, var next: Node)
  private val end = new Node(default, null, null) // sentinel
  // Make sentinel point to self
  end.prev = end 
  end.next = end
  private var numElems = 0
  
  def +=(elem: A): MutableDLL.this.type = { // Append method (adds to tail)
    val newNode = new Node(elem, end.prev, end)
    end.prev.next = newNode
    end.prev = newNode
    numElems += 1
    this
  }
  
  def +=:(elem: A): MutableDLL.this.type = { // Prepend method (adds to head)
    val newNode = new Node(elem, end, end.next)
    end.next.prev = newNode
    end.next = newNode
    numElems += 1
    this
  }
  
  def apply(n: Int): A = {
    require(n >= 0 && n < numElems)
    var rover = end.next
    for(i <- 1 to n) rover = rover.next
    rover.data
  }
  
  def update(n: Int, data: A): Unit = {
    require(n >= 0 && n < numElems)
    var rover = end.next
    for(i <- 1 to n) rover = rover.next
    rover.data = data
  }
  
  def clear(): Unit = {
    end.next = end
    end.prev = end
    numElems = 0
  }
  
  def insertAll(n: Int, elems: collection.Traversable[A]): Unit = {
    require(n >= 0 && n < numElems+1) // +1 bc user is allowed to insert 1 after end. Just appends to end of list
    if(elems.nonEmpty) { // ensure that there are elems to insert
      var rover = end.next
      for(i <- 0 until n) rover = rover.next
      for(e <- elems) {
        val newNode = new Node(e, rover.prev, rover)
        rover.prev.next = newNode
        rover.prev = newNode
        numElems += 1
      }
    }
  }
  
  def iterator: Iterator[A] = new Iterator[A] {
    var rover = end.next
    def hasNext: Boolean = rover != end
    def next(): A = {
      val ret = rover.data
      rover = rover.next
      ret
    }
  }
  
  def length: Int = numElems
  
  def remove(n: Int): A = {
    require(n >= 0 && n < numElems)
    numElems -= 1
    var rover = end.next
    for(i <- 0 until n) rover = rover.next
    val ret = rover.data
    rover.prev.next = rover.next.next
    rover.next.prev = rover.prev
    ret
    }
}
