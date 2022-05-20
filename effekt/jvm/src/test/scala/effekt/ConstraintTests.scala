package effekt

import effekt.symbols._
import effekt.typer._
import effekt.substitutions._

import org.scalatest.funspec.AnyFunSpec

import scala.language.implicitConversions

class ConstraintTests extends AnyFunSpec {

  lazy val scope = new UnificationScope

  lazy val S = freshVariable("S")
  lazy val T = freshVariable("T")
  lazy val U = freshVariable("U")
  lazy val A = freshVariable("A")
  lazy val B = freshVariable("B")
  lazy val C = freshVariable("C")

  def freshVariable(name: String) =
    scope.fresh(UnificationVar.TypeVariableInstantiation(TypeVar(Name.local(name))))

  def freshGraph() = new ConstraintGraph

  describe("simple constraints") {
    it("should set two variables in subtype relation") {
      val graph = freshGraph()

      // S --> T
      graph.connect(S, T)

      assert(graph.isSubtypeOf(S, T))
      assert(!graph.isSubtypeOf(T, S))
    }

    it("mutually connecting two nodes should put them into one equivalence class") {
      val graph = freshGraph()

      // S <--> T
      graph.connect(S, T)
      graph.connect(T, S)

      assert(graph.isSubtypeOf(S, T))
      assert(graph.isSubtypeOf(T, S))
      assert(graph.isEqual(S, T))
    }

    it("transitively connecting nodes should put them into subtype relation (length 3)") {
      val graph = freshGraph()

      // S --> T --> U
      graph.connect(S, T)
      graph.connect(T, U)

      assert(graph.isSubtypeOf(S, U))
      assert(!graph.isSubtypeOf(U, S))
    }

    it("transitively connecting nodes should put them into subtype relation (length 6)") {
      val graph = freshGraph()

      // S --> T --> U
      graph.connect(S, T)
      graph.connect(T, U)

      // A --> B --> C
      graph.connect(A, B)
      graph.connect(B, C)

      // S --> T --> U --> A --> B --> C
      graph.connect(U, A)

      assert(graph.isSubtypeOf(S, B))
      assert(graph.isSubtypeOf(T, B))
      assert(graph.isSubtypeOf(S, C))
    }

    it("forming indirect cycles should create equivalence classes") {
      val graph = freshGraph()

      // S --> T --> U --> A
      graph.connect(S, T)
      graph.connect(T, U)
      graph.connect(U, A)

      // S --> T --> U --> A
      // ^                 |
      // +-----------------+
      graph.connect(A, S)

      assert(graph.isEqual(S, A))
      assert(graph.isEqual(T, U))
      assert(graph.isEqual(T, A))
    }
    it("forming indirect cycles should create equivalence classes (2)") {
      val graph = freshGraph()

      // S --> T --> U
      graph.connect(S, T)
      graph.connect(T, U)

      // A --> B --> C
      graph.connect(A, B)
      graph.connect(B, C)

      // S --> T --> {A, U} --> B --> C
      graph.connect(U, A)
      graph.connect(A, U)

      assert(graph.isEqual(U, A))
      assert(graph.isSubtypeOf(S, A))
      assert(graph.isSubtypeOf(T, A))
      assert(graph.isSubtypeOf(U, B))
      assert(!graph.isSubtypeOf(B, T))
    }
    it("connecting two classes forms one class") {
      val graph = freshGraph()

      // S <--> T
      graph.connect(S, T)
      graph.connect(T, S)

      // A <--> B
      graph.connect(A, B)
      graph.connect(B, A)

      // {S, T} --> {A, B}
      graph.connect(S, B)
      assert(graph.isSubtypeOf(T, A))

      // {S, T, A, B}
      graph.connect(A, T)
      assert(graph.isEqual(A, S))
      assert(graph.isEqual(A, T))
      assert(graph.isEqual(A, B))
    }
  }
}
