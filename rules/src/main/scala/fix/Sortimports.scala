package fix

import scala.meta._
import scala.meta.tokens.Token.{Comment, LF}

import fix.SortImportsConfig.Trade
import metaconfig.generic.Surface
import metaconfig.{ConfDecoder, ConfEncoder, Configured, generic}
import scalafix.v1._

final case class SortImportsConfig(
  blocks: List[String] = List(SortImportsConfig.Blocks.Asterisk),
  asciiSort: Boolean = true
)

object SortImportsConfig {

  object Blocks {
    val Asterisk: String = "*"
  }

  val default: SortImportsConfig = SortImportsConfig()
  implicit val surface: Surface[SortImportsConfig] = generic.deriveSurface[SortImportsConfig]
  implicit val decoder: ConfDecoder[SortImportsConfig] = generic.deriveDecoder[SortImportsConfig](default)
  implicit val encoder: ConfEncoder[SortImportsConfig] = generic.deriveEncoder[SortImportsConfig]

  final class Trade(val value: (Import, String)) extends AnyVal {
    def from: Import = value._1
    def to: String = value._2
  }
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

  private val sortWith = if (config.asciiSort) new DefaultSort else new WildcardAndGroupFirstSort

  override def fix(implicit doc: SyntacticDocument): Patch = {

    // Traverse full code tree. Stop when import branches are found and add them to last list in buf
    // If an empty line is found add an empty list to buf
    val importGroupsWithEmptyLines: List[ImportGroup] = ImportGroupTraverser.retrieveImportGroups(doc.tree)

    // Contains groups of imports
    val importGroups: List[ImportGroup] = importGroupsWithEmptyLines.filter(_.nonEmpty)

    // Trailing comments
    val comments: Map[Import, Comment] = ImportGroup(importGroups.flatten).trailingComment(doc.comments)

    // Remove all newlines within import groups
    val removeLinesPatch: List[Patch] = importGroups.flatMap { importGroup =>
      doc.tokens.collect {
        case token: LF if importGroup.containPosition(token.pos) => token
      }
    }.map(Patch.removeToken)

    // Remove comments and whitespace between imports and comments
    val removeCommentsPatch: Iterable[Patch] = comments.values.map(Patch.removeToken)
    val removeCommentSpacesPatch: Iterable[Patch] = comments.flatMap {
      case (imp, comment) => (0 to comment.pos.start - imp.pos.end).map { diff =>
        new Token.Space(Input.None, comment.dialect, imp.pos.end + diff)
      }
    }.map(Patch.removeToken)

    // Sort each group of imports
    val sorted: Seq[Seq[String]] = importGroups.map { importGroup =>
      val configBlocksByLengthDesc = config.blocks.sortBy(-_.length)

      // Sort all imports then group based on SortImports rule
      // In case of import list, the first element in the list is significant
      val importsGrouped: Map[String, ImportGroup] =
        importGroup
          .sortWith(sortWith.perform)
          .groupByBlock(configBlocksByLengthDesc, SortImportsConfig.Blocks.Asterisk)

      // If a start is not found in the SortImports rule, add it to the end
      val configBlocks: List[String] =
        config.blocks
          .find(_ == SortImportsConfig.Blocks.Asterisk)
          .fold(config.blocks :+ SortImportsConfig.Blocks.Asterisk)(_ => config.blocks)

      // Sort grouped imports and convert to strings
      val strImportsSorted = configBlocks.foldLeft(Seq[Seq[String]]()) { (acc, configBlock) =>
          importsGrouped.find {
            case (block, _) => block == configBlock
          }.fold(acc) {
            case (_, imports) =>
              val strImports = imports.map { imp =>
                comments.get(imp).fold(imp.syntax)(comment => s"${imp.syntax} ${comment.syntax}")
              }.toSeq

              acc :+ (strImports.init :+ (strImports.last + '\n'))
          }
        }.flatten

      // Remove extra newline on end of imports
      strImportsSorted.init :+ strImportsSorted.last.dropRight(1)
    }

    val combined: List[List[Trade]] =
      importGroups
        .zip(sorted)
        .map {
          case (importGroup, strImportGroupSorted) => importGroup.value.zip(strImportGroupSorted).map(new Trade(_))
        }

    // Create patches using sorted - unsorted pairs
    // Essentially imports are playing musical chairs
    val patches: List[Patch] =
      combined.flatMap(importTrades =>
        importTrades.init.map(trade =>
          Patch.replaceTree(trade.from, s"${trade.to}\n")
        ) :+ Patch.replaceTree(importTrades.last.from, importTrades.last.to)
      )

    List(patches, removeLinesPatch, removeCommentsPatch, removeCommentSpacesPatch).flatten.asPatch
  }
}
