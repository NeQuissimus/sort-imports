package fix

import java.util.regex.Pattern

sealed trait Block {
  def string: String
  def matches(s: String): Boolean
}
object Block {
  final class StaticPrefix(val string: String) extends Block {
    override def matches(s: String): Boolean = s.startsWith(string)
  }
  object RegexPrefix {
    val Prefix: String = "re:"
  }
  final class RegexPrefix(val string: String) extends Block {
    private val pattern                      = Pattern.compile(string)
    override def matches(s: String): Boolean = pattern.matcher(s).lookingAt
  }
  object Default extends Block {
    val string                               = "*"
    override def matches(s: String): Boolean = true
  }
}
