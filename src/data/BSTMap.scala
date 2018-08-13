package data

import scala.collection.mutable

class BSTMap[K,V](comp:(K,K) => Int) extends mutable.Map[K,V] {
  
  class Node(var key: K, var value: V) {
    var left: Node = null
    var right: Node = null
  }
  
  private var root: Node = null
  
  def +=(kv: (K,V)) = {
    val (key, value) = kv
    def recur(n: Node): Node = {
      if(n == null) new Node(key,value)
      else {
        val c = comp(key, n.key)
        if(c == 0) {
          n.value = value
          n.key = key
        } else if (c<0) {
          n.left = recur(n.left)
        } else {
          n.right = recur(n.right)
        }
        n
      }
    }
    root = recur(root)
    this
  }
  
  def -=(key: K) = {
    def recur(n: Node): Node = {
      if(n == null) null
      else {
        val c = comp(key, n.key)
        if(c == 0) {
          if(n.left == null) n.right
          else if(n.right == null) n.left
          else {
            val (k,v,node) = removeMax(n.left)
            n.left = node
            n.key = k
            n.value = v
          }
        } else if (c<0) {
          n.left = recur(n.left)
        } else {
          n.right = recur(n.right)
        }
        n
      }
    }
    
    def removeMax(n: Node): (K,V, Node) = {
      if(n.right == null) {
        (n.key, n.value, n.left)
      }
      else {
        val (k,v,node) = removeMax(n.right)
        n.right = node
        (k,v,n)
      }
    }
    
    root = recur(root)
    this
  }
  
  def get(key: K): Option[V] = {
    var rover = root
    var c = if(rover != null) comp(key, rover.key) else 0
    while(rover != null && c!=0) {
      if(c<0) rover = rover.left else rover = rover.right
      c = if(rover != null) comp(key, rover.key) else 0
    }
    if(rover==null) None else Some(rover.value)
  }
  
  def iterator = new Iterator[(K,V)] {
    val stack = new ArrayStack[Node]
    pushAllLeft(root)
    
    def next:(K,V) = {
      val ret = stack.pop()
      pushAllLeft(ret.right)
      ret.key -> ret.value
    }
    
    def hasNext = !stack.isEmpty
    
    def pushAllLeft(n: Node) {
      if(n != null) {
        stack.push(n)
        pushAllLeft(n.left)
      }
    }
  }
  
}