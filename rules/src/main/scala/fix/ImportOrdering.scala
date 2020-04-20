package fix

import scala.meta.Import
import WildcardAndGroupFirstSort._

sealed trait ImportOrdering extends Ordering[Import] {

  protected def strFirstImport(imp: Import): String =
    imp.children.head.syntax
}

class DefaultSort extends ImportOrdering {

  override def compare(x: Import, y: Import): Int =
    strFirstImport(x).compareTo(strFirstImport(y))
}

object WildcardAndGroupFirstSort {

  private val wildcardRegex = "_".r
  private val groupRegex    = "\\{.+\\}".r
}

class WildcardAndGroupFirstSort extends ImportOrdering {

  private def transformForSorting(imp: Import): (String, String) = {
    val strImp = strFirstImport(imp)
    (strImp, groupRegex.replaceAllIn(wildcardRegex.replaceAllIn(strImp, "\0"), "\1"))
  }

  override def compare(x: Import, y: Import): Int =
    (transformForSorting(x), transformForSorting(y)) match {
      case ((strImp1, tranformedStrImp1), (strImp2, tranformedStrImp2)) =>
        val transformComparison = tranformedStrImp1.compareTo(tranformedStrImp2)
        if (transformComparison != 0) transformComparison else strImp1.compareTo(strImp2)
    }
}
