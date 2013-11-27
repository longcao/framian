package pellucid.pframe

import scala.reflect.runtime.universe.{ TypeTag, typeTag }
import scala.reflect.ClassTag

import spire.algebra.Order
import spire.std.int._

import shapeless._

trait RowPopulator[A, Row, Col] {
  type State
  def init: State
  def populate(state: State, row: Row, data: A): State
  def frame(state: State): Frame[Row, Col]
}

object RowPopulator {
  implicit def HListRowPopulator[Row: Order: ClassTag, L <: HList](
      implicit pop: HListColPopulator[L]) = new HListRowPopulator[Row, L](pop)

  final class HListRowPopulator[Row: Order: ClassTag, L <: HList](val pop: HListColPopulator[L])
      extends RowPopulator[L, Row, Int] {

    type State = List[Row] :: pop.State

    def init: State = Nil :: pop.init

    def populate(state: State, row: Row, data: L): State =
      (row :: state.head) :: pop.populate(state.tail, data)

    def frame(state: State): Frame[Row, Int] = {
      val cols = pop.columns(state.tail).toArray
      val rowIndex = Index(state.head.reverse.toArray)
      val colIndex = Index(Array.range(0, cols.size))
      Frame.fromColumns(rowIndex, colIndex, Column.fromArray(cols))
    }
  }
}

trait HListColPopulator[L <: HList] {
  type State <: HList
  def init: State
  def populate(state: State, data: L): State
  def columns(state: State): List[UntypedColumn]
}

trait HListColPopulator0 {
  implicit object HNilColPopulator extends HListColPopulator[HNil] {
    type State = HNil
    def init: HNil = HNil
    def populate(u: HNil, data: HNil): HNil = HNil
    def columns(state: State): List[UntypedColumn] = Nil
  }
}

object HListColPopulator extends HListColPopulator0 {
  implicit def HConsColPopulator[H: ClassTag: TypeTag, T <: HList](implicit tail: HListColPopulator[T]) =
    new HConsColPopulator(tail)

  final class HConsColPopulator[H: ClassTag: TypeTag, T <: HList](val tail: HListColPopulator[T])
      extends HListColPopulator[H :: T] {

    type State = List[H] :: tail.State

    def init = Nil :: tail.init

    def populate(state: State, data: H :: T): State =
      (data.head :: state.head) :: tail.populate(state.tail, data.tail)

    def columns(state: State): List[UntypedColumn] = {
      val col = TypedColumn(Column.fromArray(state.head.reverse.toArray))
      col :: tail.columns(state.tail)
    }
  }
}