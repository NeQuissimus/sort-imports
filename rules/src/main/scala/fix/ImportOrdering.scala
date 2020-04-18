package fix

import scala.meta.Import

sealed trait ImportOrdering extends Ordering[Import] {

  protected def strFirstImport(imp: Import): String =
    imp.children.head.syntax
}

class DefaultSort extends ImportOrdering {

  override def compare(x: Import, y: Import): Int =
    strFirstImport(x).compareTo(strFirstImport(y))
}
