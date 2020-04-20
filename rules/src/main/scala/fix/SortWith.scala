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

class WildcardAndGroupFirstSort extends SortWith {

  private def transformForSorting(imp: Import): (String, String) = {
    val strImp = strFirstImport(imp)
    (strImp, strImp.replaceAll("_", "\0").replaceAll( "\\{.+\\}", "\1"))
  }

  override def perform(imp1: Import, imp2: Import): Boolean = {
    (transformForSorting(imp1), transformForSorting(imp2)) match {
      case ((strImp1, tranformedStrImp1), (strImp2, tranformedStrImp2)) => {
        val transformComparison = tranformedStrImp1.compareTo(tranformedStrImp2)
        if (transformComparison != 0) transformComparison < 0  else strImp1.compareTo(strImp2) < 0
      }
    }
  }
}