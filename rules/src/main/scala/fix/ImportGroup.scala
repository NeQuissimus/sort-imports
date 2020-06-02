package fix

import scala.collection.mutable.ListBuffer
import scala.meta.contrib.AssociatedComments
import scala.meta.inputs.Position
import scala.meta.tokens.Token
import scala.meta.{ Import, Traverser, Tree }

object ImportGroupTraverser {
  def retrieveImportGroups(tree: Tree): List[ImportGroup] = {
    val importGroupsBuffer = ListBuffer[ListBuffer[Import]](ListBuffer.empty)
    val importTraverser    = new ImportGroupTraverser(importGroupsBuffer)
    importTraverser(tree)
    importGroupsBuffer.map(importGroupBuffer => ImportGroup(importGroupBuffer.toList)).toList
  }
}

private class ImportGroupTraverser(listBuffer: ListBuffer[ListBuffer[Import]]) extends Traverser {
  override def apply(tree: Tree): Unit =
    tree match {
      case x: Import => listBuffer.last.append(x)
      case node =>
        listBuffer.append(ListBuffer.empty)
        super.apply(node)
    }
}

case class ImportGroup(value: List[Import]) extends Iterable[Import] {

  def sortWith(ordering: Ordering[Import]): ImportGroup = ImportGroup(value.sortWith(ordering.lt))

  def groupByBlock(blocks: List[Block]): Map[Block, ImportGroup] =
    value.groupBy { imp =>
      blocks
        .find(_.matches(imp.children.head.syntax))
        .getOrElse(Block.Default)
    }.mapValues(ImportGroup(_)).toMap

  def containPosition(pos: Position): Boolean =
    pos.start > value.head.pos.start && pos.end < value.last.pos.end

  def trailingComment(comments: AssociatedComments): Map[Import, Token.Comment] =
    value
      .map(currentImport => currentImport -> comments.trailing(currentImport).headOption)
      .collect {
        case (imp, comment) if comment.nonEmpty => (imp, comment.get)
      }
      .toMap

  override def isEmpty: Boolean = value.isEmpty

  override def foreach[U](f: Import => U): Unit = value.foreach(f)

  override def iterator: Iterator[scala.meta.Import] = value.iterator
}
