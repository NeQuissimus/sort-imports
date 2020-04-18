/*
rule = SortImports
 SortImports.blocks = [
 "java",
 "scala",
 "*",
 "com.sun"
 ]
 */
import scala.util._ // foo1
import scala.collection._ // foo2
import java.util.Map
import com.oracle.net._
import com.sun.awt._ // foo3
import java.math.BigInteger

/**
 *  Bla
 */
object Bla {
  val foo = "Hello" // World
}
