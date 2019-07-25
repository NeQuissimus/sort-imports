package fix

import metaconfig.Configured
import metaconfig.generic

import scalafix.v1._
import scala.meta._

final case class SortImportsConfig(blocks: List[String] = List("*"))

object SortImportsConfig {
  val default          = SortImportsConfig()
  implicit val surface = generic.deriveSurface[SortImportsConfig]
  implicit val decoder = generic.deriveDecoder[SortImportsConfig](default)
  implicit val encoder = generic.deriveEncoder[SortImportsConfig]
}

class SortImports(config: SortImportsConfig) extends SemanticRule("SortImports") {
  def this() = this(SortImportsConfig.default)

  override def description: String =
    "Sorts imports and groups them based on configuration"
  override def isRewrite: Boolean = true

  override def withConfiguration(config: Configuration): Configured[Rule] =
    config.conf
      .getOrElse("SortImports")(this.config)
      .map(new SortImports(_))

  override def fix(implicit doc: SemanticDocument): Patch = {

    val a: List[Importer] = doc.tree.collect {
      case i: Importer =>
        val grandparent = i.parent.flatMap(_.parent)
        grandparent match {
          case Some(_: Pkg)    => Some(i)
          case Some(_: Source) => Some(i)
          case _               => None
        }
      case _ => None
    }.filter(_.isDefined).map(_.get)

    val is = doc.tree.collect {
      case i: Import => i
    }

    val lines = is.map(_.pos.endLine)
    val range = lines match {
      case Nil => Range(0, 0, 1) // effectively empty
      case _   => Range(lines.min, lines.max)
    }
    val empties = range.diff(lines)

    val tokens = doc.tokens.collect {
      case t if empties.contains(t.pos.endLine) => t
    }

    val removal = a.map { importers =>
      importers.importees.collect {
        case importee: Importee => Patch.removeImportee(importee).atomic
      }.asPatch
    }.asPatch

    val emptyRemoval = tokens.map(Patch.removeToken).asPatch

    val importsGrouped = a
      .map(_.toString)
      .sorted
      .groupBy(s => config.blocks.find(p => s.startsWith(p)))

    val noneValues = importsGrouped.get(None)

    val importsReplacedNone = importsGrouped - None ++ noneValues.fold(
      Map.empty[Option[String], List[String]]
    )(v => Map(Some("*") -> v))

    val imports: List[String] = config.blocks.flatMap { b =>
      importsReplacedNone
        .get(Some(b))
        .fold(List.empty[String])((l: List[String]) => l ++ List(""))
    }

    val importsWithKeyword = imports.map {
      case "" => "\n"
      case x  => s"\nimport ${x}"
    }.dropRight {
      imports.lastOption match {
        case Some("") => 1
        case _        => 0
      }
    }

    val add = importsWithKeyword
      .map(s => Patch.addLeft(a.head.parent.get, s))
      .asPatch

    List(emptyRemoval, removal, add).asPatch
  }
}
