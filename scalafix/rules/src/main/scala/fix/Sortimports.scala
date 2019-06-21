package fix

import metaconfig.Configured
import metaconfig.generic

import scalafix.v1._
import scala.meta._

final case class SortImportsConfig(blocks: List[String] = List.empty)

object SortImportsConfig {
  val default = SortImportsConfig()
  implicit val surface = generic.deriveSurface[SortImportsConfig]
  implicit val decoder = generic.deriveDecoder[SortImportsConfig](default)
  implicit val encoder = generic.deriveEncoder[SortImportsConfig]
}

class SortImports(config: SortImportsConfig) extends SemanticRule("SortImports") {
  def this() = this(SortImportsConfig.default)

  override def description: String =
    "Removes unused imports and terms that reported by the compiler under -Ywarn-unused"
  override def isRewrite: Boolean = true

  override def withConfiguration(config: Configuration): Configured[Rule] =
      config.conf
        .getOrElse("SortImports")(this.config)
        .map(new SortImports(_))

  override def fix(implicit doc: SemanticDocument): Patch = {
    val a: List[Importer] = doc.tree.collect {
        case i: Importer => Some(i)
        case _ => None
      }.filter(_.isDefined).map(_.get)

    val removal = a.map { importers =>
        importers.importees.collect {
          case importee: Importee => Patch.removeImportee(importee).atomic
        }.asPatch
      }.asPatch

    val importsGrouped = a
      .map(_.toString)
      .sorted
      .groupBy(s => config.blocks.find(p => s.startsWith(p)))

    val imports: List[String] = config.blocks.flatMap(b => importsGrouped.get(Some(b))
                                                        .fold(List.empty[String])(
                                                          (l: List[String]) => l ++ List("")
                                                        )) ++ importsGrouped.get(None).fold(List.empty[String])(identity)

    val importsWithKeyword = imports.map {
        case "" => "\n"
        case x => s"\nimport ${x}"
      }.dropRight {

        imports.lastOption match {
          case Some("") => 1
          case _ => 0
        }
      }

    val add = importsWithKeyword.map(s => Patch.addLeft(a.head.parent.get, s)).asPatch

    List(removal, add).asPatch
  }

}
