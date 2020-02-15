package fix

import scala.collection.mutable.ListBuffer
import scala.meta._
import scala.meta.tokens.Token.Comment

import metaconfig.Configured
import metaconfig.generic

import scalafix.v1._

final case class SortImportsConfig(blocks: List[String] = List("*"))

object SortImportsConfig {
  val default          = SortImportsConfig()
  implicit val surface = generic.deriveSurface[SortImportsConfig]
  implicit val decoder = generic.deriveDecoder[SortImportsConfig](default)
  implicit val encoder = generic.deriveEncoder[SortImportsConfig]
}

class SortImports(config: SortImportsConfig) extends SyntacticRule("SortImports") {
  def this() = this(SortImportsConfig.default)

  override def description: String =
    "Sorts imports and groups them based on configuration"
  override def isRewrite: Boolean = true

  override def withConfiguration(config: Configuration): Configured[Rule] =
    config.conf
      .getOrElse("SortImports")(this.config)
      .map(new SortImports(_))

  override def fix(implicit doc: SyntacticDocument): Patch = {

    // Traverse full code tree. Stop when import branches are found and add them to last list in buf
    // If an empty line is found add an empty list to buf
    val buf: ListBuffer[ListBuffer[Import]] = ListBuffer(ListBuffer.empty)
    val traverser: Traverser = new Traverser {
      override def apply(tree: Tree): Unit = tree match {
        case x: Import =>
          buf.last.append(x)
        case node =>
          buf.append(ListBuffer.empty)
          super.apply(node)
      }
    }

    traverser(doc.tree)

    // Contains groups of imports
    val unsorted: ListBuffer[ListBuffer[Import]] = buf
      .filter(_.length > 0)

    // Trailing comments
    val comments: Map[Import, Option[Comment]] =
      unsorted.flatten.map(x => (x -> doc.comments.trailing(x).headOption)).filterNot(_._2.isEmpty).toMap

    // Remove all newlines within import groups
    val removeLinesPatch: ListBuffer[Patch] = unsorted.map { i =>
      doc.tokens.collect {
        case e
            if e.productPrefix == "LF"
              && e.pos.start > i.head.pos.start
              && e.pos.end < i.last.pos.end =>
          e
      }
    }.flatten
      .map(Patch.removeToken(_))

    // Remove comments and whitespace between imports and comments
    val removeCommentsPatch: Iterable[Patch] = comments.values.flatten.map(Patch.removeToken _)
    val removeCommentSpacesPatch: Iterable[Patch] = comments.flatMap {
      case (k, v) =>
        v.map { v =>
          val num = v.pos.start - k.pos.end
          ((0 to num).map { diff => new Token.Space(Input.None, v.dialect, k.pos.end + diff) }).toList
        }.getOrElse(List.empty)
    }.map(Patch.removeToken _)

    // Sort each group of imports
    val sorted: ListBuffer[ListBuffer[String]] = unsorted.map { importLines =>
      val configBlocksByLengthDesc = config.blocks.sortBy(-_.length)

      // Sort all imports then group based on SortImports rule
      // In case of import list, the first element in the list is significant
      val importsGrouped = importLines.sortWith { (line1, line2) =>
        line1.children.head.toString.compareTo(line2.children.head.toString) < 0
      }.groupBy(line => configBlocksByLengthDesc.find(block => line.children.head.toString.startsWith(block)))

      // If a start is not found in the SortImports rule, add it to the end
      val fixedList: List[String] = config.blocks
        .find(_ == "*")
        .fold(config.blocks :+ "*")(_ => config.blocks)

      // Sort grouped imports and convert to strings
      val importsSorted = fixedList
        .foldLeft(ListBuffer[ListBuffer[String]]()) { (acc, i) =>
          importsGrouped
            .find(_._1.getOrElse("*") == i) // If key is None, make key *
            .fold(acc) { found =>
              val commentOrNot = comments.get(found._2.last).map(" " + _.mkString)
              acc += (found._2.map(_.toString).init += (found._2.last.toString + commentOrNot.getOrElse("") + "\n"))
            }
        }
        .flatten

      // Remove extra newline on end of imports
      importsSorted.init :+ importsSorted.last.dropRight(1)
    }

    val combined: ListBuffer[ListBuffer[(Import, String)]] = unsorted
      .zip(sorted)
      .map(i => i._1.zip(i._2))

    // Create patches using sorted - unsorted pairs
    // Essentially imports are playing musical chairs
    val patches: ListBuffer[Patch] = combined.map { el =>
      el.init.map { i =>
        Patch.replaceTree(i._1, i._2 + "\n")
      } :+ Patch.replaceTree(el.last._1, el.last._2)
    }.flatten

    List(patches, removeLinesPatch, removeCommentsPatch, removeCommentSpacesPatch).flatten.asPatch
  }
}
