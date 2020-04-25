package effekt

import effekt.context.Context
import effekt.core.{ JavaScript, Transformer }
import effekt.namer.Namer
import effekt.source.ModuleDecl
import effekt.symbols.Module
import effekt.typer.Typer
import effekt.util.{ SourceTask, Task, VirtualSource }
import effekt.util.messages.FatalPhaseError
import org.bitbucket.inkytonik.kiama
import kiama.output.PrettyPrinterTypes.Document
import kiama.parsing.{ NoSuccess, Success }
import kiama.util.{ Positions, Source }

import scala.collection.mutable

/**
 * "Pure" compiler without reading or writing to files
 *
 * All methods return Option, the errors are reported in the given context
 */
trait Compiler {

  val positions: Positions

  // Frontend phases
  // ===============
  object parser extends Parser(positions)
  object namer extends Namer
  object typer extends Typer

  // Backend phases
  // ==============
  object transformer extends Transformer
  object generator extends JavaScript

  // Tasks
  // =====

  object getAST extends SourceTask[ModuleDecl]("ast") {
    def run(source: Source)(implicit C: Context): Option[ModuleDecl] = source match {
      case VirtualSource(decl, _) => Some(decl)
      case _ =>
        println("Running parser on " + source.name)
        parser(source)
    }
  }

  object frontend extends SourceTask[Module]("frontend") {
    def run(source: Source)(implicit C: Context): Option[Module] = for {
      ast <- getAST(source)
      _ = println("Running frontend on " + source.name)
      mod = Module(ast, source)
      _ <- C.using(module = mod, focus = ast) {
        for {
          _ <- namer(mod)
          _ <- typer(mod)
        } yield ()
      }
    } yield mod
  }

  object computeCore extends SourceTask[core.ModuleDecl]("core") {
    def run(source: Source)(implicit C: Context): Option[core.ModuleDecl] = for {
      mod <- frontend(source)
      core <- transformer(mod)
    } yield core
  }

  object generateJS extends SourceTask[Document]("generator") {
    def run(source: Source)(implicit C: Context): Option[Document] = for {
      core <- computeCore(source)
      _ = println("generating JS for " + source.name)
      doc <- generator(core)
    } yield doc
  }

  // TODO change result from Unit to File
  object compile extends SourceTask[Unit]("compile") {
    def run(source: Source)(implicit C: Context): Option[Unit] = for {
      mod <- frontend(source)
      js <- generateJS(source)
      _ = println("writing output for " + source.name)
      _ = saveOutput(js, mod)
    } yield ()
  }

  /**
   * Output writer: Document -> IO
   *
   * TODO convert into task?
   */
  def saveOutput(js: Document, unit: Module)(implicit C: Context): Unit

}
