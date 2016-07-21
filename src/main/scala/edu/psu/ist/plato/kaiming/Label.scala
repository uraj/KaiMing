package edu.psu.ist.plato.kaiming

case class Label(name: String, addr: Long) {

  override def equals(l: Any) = l match {
    case label: Label => label.name == name && label.addr == addr
    case _ => false
  }
  
}
