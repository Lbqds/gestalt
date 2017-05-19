package scala.gestalt.core

case class Location(fileName: String, line: Int, column: Int)

trait Toolbox extends Trees with Types with Denotations with Symbols with TypeTags {

  /** get the location where the def macro is used */
  def location: Location

  /** diagnostics */
  def error(message: String, pos: Pos): Unit

  /** stop macro transform */
  def abort(message: String, pos: Pos): Nothing

  /** generate fresh unique name */
  def fresh(prefix: String = "$local"): String
}