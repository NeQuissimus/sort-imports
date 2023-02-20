package fix

import scala.collection.mutable.ListBuffer
import scala.meta.contrib.{ AComments, AssociatedComments }
import scala.meta.inputs.Position
import scala.meta.tokens.Token
import scala.meta.{ Import, Tokens, Traverser, Tree }

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

  def trailedBy(pos: Position): Boolean =
    pos.start == value.last.pos.end

  def trailingComment(tokens: Tokens, comments: AssociatedComments): Map[Import, Token.Comment] = {
    val trailingMap = AComments.trailingMap(comments)

    value.flatMap { currentImport =>
      val sc                                  = ImportGroup.semicolons(tokens, currentImport)
      val cs: IndexedSeq[List[Token.Comment]] = sc.flatMap(s => trailingMap.get(s))

      (currentImport -> comments.trailing(currentImport).headOption) +: cs.map(c => currentImport -> c.headOption)
    }.collect { case (imp, Some(comment)) =>
      (imp, comment)
    }.toMap
  }

  def trailingSemicolon(tokens: Tokens): Set[Import] =
    value.filter(imp => ImportGroup.semicolons(tokens, imp).nonEmpty).toSet

  override def isEmpty: Boolean = value.isEmpty

  override def foreach[U](f: Import => U): Unit = value.foreach(f)

  override def iterator: Iterator[scala.meta.Import] = value.iterator
}

object ImportGroup {
  def semicolons(tokens: Tokens, imp: Import) =
    tokens.collect { case t: Token.Semicolon if imp.pos.end == t.start => t }
}
