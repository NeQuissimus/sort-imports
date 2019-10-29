/*
rule = SortImports
 SortImports.blocks = [
 "java",
 "scala",
 "com\\.sun"
 ]
 */
import scala.util._
import scala.collection._
import java.util.Map
import com.oracle.net._

object NoStarInImportOrder {
  import java.math.BigInteger
  import com.sun.awt._
  // Add code that needs fixing here.
}
