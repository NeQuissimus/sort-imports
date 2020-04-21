/*
rule = SortImports
 SortImports.blocks = [
 "java",
 "scala",
 "*",
 "com.sun"
 ]
 */
//scalafix:off
import scala.util._
import scala.collection._
//scalafix:on
import java.util.Map
import com.oracle.net._
import com.sun.awt._
import java.math.BigInteger

object ScalafixOffOn {
  // Add code that needs fixing here.
}
