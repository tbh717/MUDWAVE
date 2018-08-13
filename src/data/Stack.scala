package data

trait Stack[A] {
  
  // Adds element to top of stack.
  def push(a: A): Unit
  
  // Removes most recently added element from stack.
  def pop(): A
  
  // Gives back next item that would be popped.
  // Didn't put () because it doesn't actually altar the stack.
  def peek: A
  
  // Tells if there are no items on the stack to pop.
  def isEmpty: Boolean
}