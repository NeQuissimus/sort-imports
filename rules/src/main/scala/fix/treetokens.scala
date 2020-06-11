package scala
package meta
package contrib

import Token.Comment

object AComments {
  def trailingMap(ac: AssociatedComments): Map[Token, List[Comment]] = {
    val f = classOf[AssociatedComments].getDeclaredField("trailingMap")
    f.setAccessible(true)

    f.get(ac).asInstanceOf[Map[Token, List[Comment]]]
  }
}
