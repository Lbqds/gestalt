import scala.gestalt._
import scala.gestalt.options.unsafe

object plusObject {
  def apply(a: Int, b: Int): Int = meta {
    q"$a + $b"
  }

  def defaultArgs(a:Int, b: Int = 1): Int = meta {
    q"$a + $b"
  }
  def curried(a:Int)(b: Int): Int = meta {
    q"$a + $b"
  }
  def poly(a: Any, b: Int): Int = meta {
    a match {
      case tpd.Lit(i:Int) => q"$a + $b"
      case tpd.Lit(s:String) => q"${a.wrap}.toInt + $b"
      case other =>
        error(s"expected String or Interger constants", a.pos)
        untpd.Term.Lit(null)
    }
  }

  def varargs(items: Int*): Int = meta {
    items match {
      case tpd.SeqLiteral(items: Seq[tpd.Tree]) =>
        items.map(item => item.wrap).reduceLeft[untpd.TermTree]((a, b) => q"$a + $b")
      case tpd.Ascribe(list, _) =>
        q"$list.reduce((a:Int,b:Int)=> a + b)"
    }
  }

  def deconstructApply(items: Any): Int = meta {
    items match {
      case tpd.ApplySeq(prefix, items :: Nil) =>
        items.map(item => item.wrap).reduceLeft[untpd.TermTree]((a, b) => q"$a + $b")
      case _ =>
        error("expected application of Ints", items.pos)
        untpd.Term.Lit(null)
    }
  }
}


class plus {
  def apply(a: Int, b: Int): Int = meta {
    q"$a + $b"
  }
}

object plusOne {
  def apply(a: Int): Int =  meta {
    q"$a + 1"
  }
}

class plus2(val a: Int) {
  def apply(b: Int): Int = meta {
    q"$this.a + $b"
  }
}

object ImplicitsForNumbers {
  implicit class PlusFor(val a: Int) {
    def plus(b: Int): Int = meta {
      q"$this.a + $b"
    }
  }
}

object ImplicitBigInt {
  implicit def string2BigInt(s: String): BigInt = meta {
    val tpd.Lit(str: String) = s
    val bigInt = BigInt(str)
    val radix = Character.MAX_RADIX
    val compressedString = bigInt.toString(radix)
    q"BigInt(${untpd.Term.Lit(compressedString)},${untpd.Term.Lit(radix)})"
  }
}

object scope {
  def is[T](a: Any): Boolean = meta {
    q"$a.isInstanceOf[$T]"
  }

  def both[S, T](a: Any): Boolean = meta {
    q"$a.isInstanceOf[$S] && $a.isInstanceOf[$T]"
  }

  // test nested method inside macro def -- used to be a problem with @static implementation
  def mapTest(): Int = meta {
    val sum = (1 to 5).map(_ * 2).sum
    untpd.Term.Lit(sum)
  }
}

object trees {
  def some3(): Option[Int] = meta {
    q"Some(3)"
  }
  def some3Explicit(): Option[Int] = meta {
    q"new Some(3)"
  }
  def some3ExplicitTyped(): Option[Int] = meta {
    q"new Some[Int](3)"
  }
  def five(): Int = meta {
    q"5"
  }
  def pi(): Double = meta {
    q"Math.PI"
  }
  def ident(a: Any): Any = meta {
    q"$a"
  }

  def iterator(): Iterator[Nothing] = meta {
    q"""new Iterator[Nothing]{
         def hasNext = false
         def next() = ???
       }"""
  }

  def typedIterator[T](): Iterator[T] = meta {
    q"""new Iterator[$T]{
         def hasNext = false
         def next(): $T = ???
       }"""
  }

  def abcdObject(): AnyRef = meta {
    q"""new Object {
        override def toString = "abcd"
      }"""
  }
  def abcdObject2(): AnyRef = meta {
    q"""new java.lang.Object {
        override def toString = "abcd"
      }"""
  }

  def pfCollect(): Option[String] = meta {
    q"""Some(3).collect{
         case 2 => "two"
         case 3 => "three"
         }"""
  }
}

object Inheritance {
  trait PlusOne {
    def a: Int
    def plus1() = meta {
      q"$this.a + 1"
    }
  }
  class A(val a: Int) extends PlusOne
  object B extends PlusOne {
    val k = 1000
    def a = 8 * k + 1000
  }
  val a39 = new PlusOne {
    def a = 39
  }
}

object Materializer {
  implicit def defaultOpt[T]: Option[T] = meta { q"None" }
  implicit def defaultSome[T](implicit x: T): Some[T] = meta {
    q"Some($x)"
  }
}

object Locations {
  def currentLine(): Int = meta {
    val pos = location
    untpd.Term.Lit(pos.line)
  }
}

object CaseInfo {
  def fields[T]: List[String] = meta {
    val tp = T.tpe
    if (!tp.isCaseClass) {
      error("Not a case class", T.pos)
      q"scala.Nil"
    }
    else {
      val fieldTrees = tp.caseFields.map(m => untpd.Term.Lit(m.name))
      q"List(..$fieldTrees)"
    }
  }
}

object MultiParamBlocks {
  def f(a: Int)(b: Int): Int = meta {
    q"$a + $b"
  }

  def g(a: Int)(b: Int)(implicit c: Int): Int = meta {
    q"$a + $b - $c"
  }
}

object Whitebox {
  def gimmelist(x: Int): Seq[Int] = meta {
    q"List($x)"
  }
}

object Hygiene {
  def f(x: Int): Int = meta {
    q"""
    val x = 4
    $x
    """
  }
}

object Transform {
  def log(x: Int)(f: Int => Int): Int = meta {
    val newfun = f.transform {
      case call @ tpd.ApplySeq(tpd.Ident(fsym), args) if args.size > 0 =>
        val name = fsym.name
        val print = tpd.Ident(Type.termRef("scala.Predef")).select("println").appliedTo(tpd.Lit(s"calling $name\n") :: Nil)
        val vdef = tpd.ValDef(call)
        val res = tpd.Ident(vdef.symbol)
        tpd.Block(print :: vdef :: Nil, res)
    }

    newfun.appliedTo(x :: Nil)
  }
}

object TypedDef {
  def double(x: Int): Int = meta {
    val mt = Type.MethodType(List("n" -> Type.typeRef("scala.Int")), Type.typeRef("scala.Int"))
    val meth = tpd.DefDef("double", mt) { case (_, params :: Nil) =>
      tpd.Block(
        x :: Nil,     // test nested definition
        params(0).select("+").appliedTo(params(0) :: Nil)
      )
    }
    val v = tpd.ValDef(tpd.Lit(10))
    tpd.Block(meth :: v :: Nil, tpd.Ident(meth.symbol).appliedTo(tpd.Ident(v.symbol) :: Nil))
  }

  def annoyAdd(x: Int): Int => Int = meta {
    val scalaInt = Type.typeRef("scala.Int")
    val mt = Type.MethodType(List("v" -> scalaInt), scalaInt)

    val parent = Type.typeRef("scala.Function1").appliedTo(scalaInt, scalaInt)
    val meth = tpd.DefDef("apply", mt) {
      case (_, params :: Nil) =>
        params(0).select("+").appliedTo(x :: Nil)
    }
    tpd.NewAnonymClass(parent :: Nil, meth :: Nil)
  }
}

object Owners {
  def mywhile(cond: Boolean)(f: Unit): Int  = meta {
    val vdef = tpd.ValDef(tpd.Lit(5)) // owner now is the ctx
    val whileTree = tpd.While(cond, tpd.Block(vdef :: Nil, f))  // owner now is $while

    val scalaInt = Type.typeRef("scala.Int")
    val parent = Type.typeRef("scala.Function1").appliedTo(scalaInt, scalaInt)
    val mt = Type.MethodType(List("v" -> scalaInt), scalaInt)
    val meth = tpd.DefDef("apply", mt) { case (_, params :: Nil) =>
      params(0).select("+").appliedTo(params(0) :: Nil)
    }
    val anonTree = tpd.NewAnonymClass(parent :: Nil, meth :: whileTree :: Nil)

    tpd.Block(anonTree :: Nil, tpd.Lit(5))
  }
}