class Exit(val north: String, val south: String, val east: String, val west: String, val up: String, val down: String) {
  // no methods
  // class exists to simplify naming conventions by assigning exits to conventional directions
}

object Exit {
  // apply method for room creation
  def apply(e: Array[String]): Exit = new Exit(e(0),e(1),e(2),e(3),e(4),e(5))
}