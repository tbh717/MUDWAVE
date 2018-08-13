class Item(
    val name: String, 
    val description: String,
    val damage: Double,
    val speed: Int)
    {
    /* Speed is measured in 0.1s
     * 
     */
}


object Item {
  // Apply method for room creation
  def apply(n: xml.Node): Item = {
    new Item((n \ "@name").text, n.text, (n \ "@damage").text.toDouble, (n \ "@speed").text.toInt)
  }
}