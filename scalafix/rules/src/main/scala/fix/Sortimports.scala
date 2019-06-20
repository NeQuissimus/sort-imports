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

class Sortimports(config: SortImportsConfig) extends SemanticRule("Sortimports") {
  def this() = this(SortImportsConfig.default)

  override def description: String =
    "Removes unused imports and terms that reported by the compiler under -Ywarn-unused"
  override def isRewrite: Boolean = true

  override def withConfiguration(config: Configuration): Configured[Rule] =
      config.conf
        .getOrElse("SortImports")(this.config)
        .map(new Sortimports(_))

  override def fix(implicit doc: SemanticDocument): Patch = {
    val a: List[Importer] = doc.tree.collect {
        case i: Importer => Some(i)
        case _ => None
      }.filter(_.isDefined).map(_.get)

    println("Imports: " + a)

    val removal = a.map { importers =>
        importers.importees.collect {
          case importee: Importee => Patch.removeImportee(importee).atomic
        }.asPatch
      }.asPatch

    val importsGrouped = a
      .map(_.toString)
      .sorted
      .groupBy(s => config.blocks.find(p => s.startsWith(p)))

    println("config: " + config.blocks)
      println("grouped:" + importsGrouped)

    val imports: List[String] = config.blocks.flatMap(b => importsGrouped.get(Some(b))
                                                        .fold(List.empty[String])(
                                                          (l: List[String]) => l ++ List[String]("\n\n")
                                                        )) ++ importsGrouped.get(None).fold(List.empty[String])(identity)

    val add = imports.map(s => Patch.addLeft(a.head.parent.get, s"\nimport ${s}"))
      .asPatch

    List(removal, add).asPatch
  }

}
