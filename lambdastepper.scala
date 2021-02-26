object lambdastepper {

  sealed trait Expr {
    def show: String = this match {
      case Var(x) => x
      case Lam(x,b) => "\\" + x + "." + b.show
      case App(a,b) => a.showL + " " + b.showR
    }
    def showL: String = this match {
      case Lam(_,_) => "(" + show + ")"
      case _ => show
    }
    def showR: String = this match {
      case Var(x) => x
      case _ => "(" + show + ")"
    }
  }
  case class Var(name: String) extends Expr
  case class App(left: Expr, right: Expr) extends Expr
  case class Lam(name: String, body: Expr) extends Expr

  sealed trait Command
  case class Def(name: String, body: Expr) extends Command
  case class Reduce(expr: Expr) extends Command

  case class Program(commands: List[Command])

  def fail(msg: String): Nothing = {
    println("!!! Error: " + msg)
    scala.sys.exit()
  }

  def tokenize(text: Array[String]): Array[String] = {
    val cleanText =
      text.filter(line => line.trim.nonEmpty && line.head != '#').flatten

    val special = Set('\\','.','(',')','=','!','>') // ,';','=','~')
    val builder = new StringBuilder()
    for (c <- cleanText) {
      if (c.isWhitespace) builder += ' '
      else if (special(c)) builder ++= s" $c "
      else if (c.isLetter || c.isDigit) builder += c
      else fail("Illegal character '"+c+"'")
    }
    builder.toString.trim.split(" +")
  }

  def parse(toks: Array[String]): Program = {
    var i = 0
    def tok = if (i < toks.length) toks(i) else ";"

    def force(s: String) = {
      if (tok == s) i += 1
      else fail("Expected '" + s + "' but found '" + tok +"'")
    }
    def pupper: String = {
      val t = pname
      if (t.matches("[A-Z][a-zA-Z]*|[0-9]+")) {
        return t
      }
      fail("Illegal name '"+t+"': must be all letters (beginning with uppercase letter) or all digits")
    }
    def plower: String = {
      val t = pname
      if (t.matches("[a-z][a-zA-Z]*")) {
        return t
      }
      fail("Illegal name '"+t+"': must be all letters, beginning with lowercase letter")
    }
    def pname: String = {
      val t = tok
      if (t.matches("[a-zA-Z]+|[0-9]+")) {
        i += 1
        return t
      }
      fail("Illegal name '"+tok+"': must be made of only letters or only digits")
    }
    def patom: Expr = {
      val t = tok
      if (t.head.isLetterOrDigit) return Var(pname)
      if (tok == "(") {
        force("(")
        val e = pexpr
        force(")")
        return e
      }
      fail("Expected name or parens, found '" + tok +"'")
    }
    def papp: Expr = {
      var list = List.empty[Expr]
      val set = Set(";",")","!",">")
      while (!set(tok)) list +:= patom
      list = list.reverse
      if (list.nonEmpty) list.tail.foldLeft(list.head)(App(_,_))
      else fail("Unexpected '"+tok+"'")
    }
    def pexpr: Expr = {
      if (tok == "\\") {
        force("\\")
        val name = plower
        force(".")
        Lam(name, pexpr)
      }
      else papp
    }

    def pcommand: Command = tok match {
      case "!" =>
        force("!")
        val name = pupper
        force("=")
        val expr = pexpr
        Def(name,expr)
      case ">" =>
        force(">")
        val expr = pexpr
        Reduce(expr)
      case t =>
        fail("Illegal token '"+t+"': expected ! or >")
    }

    var commands = List.empty[Command]
    while (tok != ";") {
      commands = pcommand :: commands
    }
    Program(commands.reverse)
  }

  def freeVars(expr: Expr): Set[String] = expr match {
    case Var(x) => Set(x)
    case App(e1,e2) => freeVars(e1) union freeVars(e2)
    case Lam(x,e) => freeVars(e) - x
  }

  def redex(expr: Expr, build: Expr=>Expr): Option[(Expr => Expr, Expr)] = expr match {
    case Var(x) => defs.get(x).map{ e => (build, e) }
    case Lam(x,body) =>
      redex(body,b => build(Lam(x,b)))
    case App(Lam(x,body),arg) =>
      Some(( build, reduce(x,body,arg,freeVars(arg)) ))
    case App(e1,e2) =>
      redex(e1,e => build(App(e,e2))) orElse redex(e2,e => build(App(e1,e)))
  }

  var genSymCount = 0
  def genSym(name: String): String = {
    genSymCount += 1
    name.takeWhile(_.isLetter) + genSymCount.toString
  }

  def reduce(x: String, body: Expr, arg: Expr, free: Set[String]): Expr = body match {
    case Var(y) =>
      if (y == x) arg
      else body
    case Lam(y,ey) =>
      val fey = freeVars(ey)
      if (y == x || !fey(x)) body
      else if (free(y)) {
        val ynew = genSym(y)
        reduce(x,Lam(ynew,reduce(y,ey,Var(ynew),Set.empty)),arg,free)
      }
      else Lam(y,reduce(x,ey,arg,free))
    case App(e1,e2) =>
      App(reduce(x,e1,arg,free), reduce(x,e2,arg,free))
  }

  var defs = scala.collection.mutable.Map.empty[String,Expr]
  var free = Set.empty[String]

  def trace(lines: Array[String]): Unit = {
    defs = scala.collection.mutable.Map.empty[String,Expr]
    free = Set.empty[String]

    val tokens = tokenize(lines)
    val Program(commands) = parse(tokens)
    for (cmd <- commands) cmd match {
      case Def(name,body) =>
        val fbody = freeVars(body)
        println("! " + name + " = " + body.show)

        if (fbody(name)) fail("Function '"+name+"' cannot be recursive.")
        if (free(name)) fail("Function '"+name+"' cannot be called in previous definition.")
        val leftovers = fbody diff defs.keySet
        if (leftovers.nonEmpty)
          fail("Function '"+name+"' cannot refer to free variables such as '" +
            leftovers.head + "'.")

        free = free union freeVars(body)
        defs += (name -> body)

      case Reduce(expr) =>
        var e = expr
        var estr = e.show
        println("> " + estr)
        var count = 0
        genSymCount = 0
        import util.control.Breaks._
        breakable { while (true) {
          val r = redex(e, e1 => e1)
          if (r.isEmpty) {
            val num = if (count == 1) "1 reduction" else s"$count reductions"
            println(": Done (" + num + ").\n----------------------------------------")
            break()
          }
          count += 1
          if (count > 1000) {
            println("!!! HALTED AFTER 1000 REDUCTIONS !!!")
            break()
          }

          val Some((build,enew)) = r

          val tmp = build(Var("?")).show
          val before = tmp.indexOf('?')
          val after  = tmp.length - before - 1
          val mid1   = estr.length - before - after
          println(": " + (" "*before) + ("|"*mid1))

          e = build(enew)
          estr = e.show
          val mid2   = estr.length - before - after
          println(": " + (" "*before) + ("|"*mid2))
          println(": " + estr)
        }}
    }
  }

  def run(text: String): Unit = trace(text.linesIterator.toArray)

  def main(args: Array[String]): Unit = {
    if (args.isEmpty) println("missing arg: name of file")
    else {
      val filename = args(0)
      val text = scala.io.Source.fromFile(filename).mkString
      run (text)
    }
  }
}
