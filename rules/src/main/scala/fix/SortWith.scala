package fix

import scala.meta.Import

sealed trait SortWith {
  def perform(imp1: Import, imp2: Import): Boolean

  protected def strFirstImport(imp: Import): String =
    imp.children.head.syntax
}

class DefaultSort extends SortWith {

  override def perform(imp1: Import, imp2: Import): Boolean =
    strFirstImport(imp1).compareTo(strFirstImport(imp2)) < 0
}