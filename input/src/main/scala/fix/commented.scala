/*
rule = SortImports
 SortImports.blocks = [
 "java",
 "scala",
 "*",
 "com.sun"
 ]
 */
import scala.util._
import scala.collection._
import java.util.Map // foo1
import com.oracle.net._
import com.sun.awt._ // foo2
import java.math.BigInteger

/**
 *  Bla
 */
object Bla {
  val foo = "Hello" // World
}
