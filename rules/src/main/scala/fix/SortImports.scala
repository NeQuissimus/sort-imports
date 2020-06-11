package fix

import scala.meta._
import scala.meta.tokens.Token.{ Comment, LF }

import fix.SortImportsConfig.Swap
import metaconfig.generic.Surface
import metaconfig.{ generic, ConfDecoder, ConfEncoder, Configured }
import scalafix.v1._

final case class SortImportsConfig(
  blocks: List[String],
  asciiSort: Boolean
)

object SortImportsConfig {

  val default: SortImportsConfig                       = SortImportsConfig(Nil, asciiSort = true)
  implicit val surface: Surface[SortImportsConfig]     = generic.deriveSurface[SortImportsConfig]
  implicit val decoder: ConfDecoder[SortImportsConfig] = generic.deriveDecoder[SortImportsConfig](default)
  implicit val encoder: ConfEncoder[SortImportsConfig] = generic.deriveEncoder[SortImportsConfig]

  final class Swap(val value: (Import, String)) extends AnyVal {
    def from: Import = value._1
    def to: String   = value._2
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

  private val importOrdering: ImportOrdering =
    if (config.asciiSort) new DefaultSort else new WildcardAndGroupFirstSort

  override def fix(implicit doc: SyntacticDocument): Patch = {

    // Traverse full code tree. Stop when import branches are found and add them to last list in buf
    // If an empty line is found add an empty list to buf
    val importGroupsWithEmptyLines: List[ImportGroup] = ImportGroupTraverser.retrieveImportGroups(doc.tree)

    // Contains groups of imports
    val importGroups: List[ImportGroup] = importGroupsWithEmptyLines.filter(_.nonEmpty)

    // Trailing comments
    val comments: Map[Import, Comment] = ImportGroup(importGroups.flatten).trailingComment(doc.tokens, doc.comments)

    // Remove all newlines within import groups
    val removeLinesPatch: List[Patch] = importGroups.flatMap { importGroup =>
      doc.tokens.collect {
        case token: LF if importGroup.containPosition(token.pos) => token
      }
    }.map(Patch.removeToken)

    val removeSemicolonsPatch: List[Patch] = importGroups.flatMap { importGroup =>
      doc.tokens.collect {
        case token: Token.Semicolon if importGroup.containPosition(token.pos) || importGroup.trailedBy(token.pos) =>
          token
      }
    }.map(Patch.removeToken)

    // Remove comments and whitespace between imports and comments
    val removeCommentsPatch: Iterable[Patch] = comments.values.map(Patch.removeToken)
    val removeCommentSpacesPatch: Iterable[Patch] = comments.flatMap {
      case (imp, comment) =>
        (0 to comment.pos.start - imp.pos.end).map { diff =>
          new Token.Space(Input.None, comment.dialect, imp.pos.end + diff)
        }
    }.map(Patch.removeToken)

    val configBlocks = {
      val originalBlocks = config.blocks.map {
        case Block.Default.string => Block.Default
        case block if block.startsWith(Block.RegexPrefix.Prefix) =>
          new Block.RegexPrefix(block.substring(Block.RegexPrefix.Prefix.length))
        case block => new Block.StaticPrefix(block)
      }

      // If DefaultBlock is not found in the SortImports rule, prepend it
      if (originalBlocks.contains(Block.Default)) {
        originalBlocks
      } else {
        originalBlocks :+ Block.Default
      }
    }

    // Sort config blocks by length desc so groupByBlock give precedence to more specific group
    val sortedConfigBlocks = configBlocks.sortBy(-_.string.length)

    // Sort each group of imports
    val sorted: Seq[Seq[String]] = importGroups.map { importGroup =>
      // Group based on SortImports rule
      // In case of import list, the first element in the list is significant
      val groupedImports: Map[Block, ImportGroup] = importGroup.groupByBlock(sortedConfigBlocks)

      // Sort grouped imports and convert to strings
      val strImportsSorted: Seq[String] = configBlocks
        .foldLeft(Seq[Seq[String]]()) { (acc, configBlock) =>
          groupedImports.get(configBlock) match {
            case Some(blockImports) =>
              val semiColons = blockImports.trailingSemicolon(doc.tokens)
              val strImports = blockImports
                .sortWith(importOrdering)
                .map { imp =>
                  val semi = if (semiColons.contains(imp)) ";" else ""
                  comments.get(imp).fold(s"$imp$semi")(comment => s"$imp$semi $comment")
                }
                .toSeq
              acc :+ (strImports.init :+ (strImports.last + '\n'))
            case _ => acc
          }
        }
        .flatten

      // Remove extra newline on end of imports
      strImportsSorted.init :+ strImportsSorted.last.dropRight(1)
    }

    val combined: List[List[Swap]] =
      importGroups
        .zip(sorted)
        .map {
          case (importGroup, strImportGroupSorted) => importGroup.value.zip(strImportGroupSorted).map(new Swap(_))
        }

    // Create patches using sorted - unsorted pairs
    // Essentially imports are playing musical chairs
    val patches: List[Patch] =
      combined.flatMap(importSwaps =>
        importSwaps.init.map(trade => Patch.replaceTree(trade.from, s"${trade.to}\n")) :+ Patch
          .replaceTree(importSwaps.last.from, importSwaps.last.to)
      )

    List(
      patches,
      removeLinesPatch,
      removeSemicolonsPatch,
      removeCommentsPatch,
      removeCommentSpacesPatch
    ).flatten.asPatch
  }
}
