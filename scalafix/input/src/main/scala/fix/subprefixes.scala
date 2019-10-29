/*
 // this test tries to ensure that if there's a longer prefix
 // later in the configuration it still takes precedence and the result
 // is NOT following due to the first `scala.` rule:
 // import scala.annotation._
 // import scala.collection.IndexedSeq
 // import scala.collection.Seq
 // import scala.io.Source
 //
 // import java.io.File

 rule = SortImports
 SortImports.blocks = [
 "scala\\."
 "scala\\.collection\\."
 "scala\\.annotation\\."
 ]
 */

package fix

import java.io.File
import scala.annotation._
import scala.io.Source
import scala.collection.Seq
import scala.collection.IndexedSeq

object SubPrefixes {}
