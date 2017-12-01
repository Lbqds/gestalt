package scala.gestalt.core

trait Denotations {
  val toolbox: Toolbox
  import toolbox.types.Type
  import toolbox.symbols.Symbol

  type Denotation

  def name(denot: Denotation): String

  def info(denot: Denotation): Type

  def symbol(denot: Denotation): Symbol
}
