package org.scalajs.testsuite.niobuffer

import scala.language.implicitConversions

import scala.reflect._

import java.nio._

import scala.scalajs.js

sealed abstract class BufferFactory {
  type BufferType <: Buffer with Comparable[BufferType]
  type ElementType

  implicit val elemClassTag: ClassTag[ElementType]

  implicit def elemFromInt(value: Int): ElementType

  implicit def elemToJSAny(elem: ElementType): js.Any

  implicit def bufferAdapter(
      buffer: BufferType): BufferAdapter[BufferType, ElementType]

  val createsReadOnly: Boolean = false

  def allocBuffer(capacity: Int): BufferType

  def allocBuffer(pos: Int, limit: Int, capacity: Int): BufferType = {
    val buf = allocBuffer(capacity)
    buf.limit(limit).position(pos)
    buf
  }

  def elemRange(start: Int, end: Int): Array[ElementType] =
    (start until end).map(elemFromInt).toArray

  def withContent(capacity: Int, content: ElementType*): BufferType =
    withContent(0, capacity, capacity, content: _*)

  def withContent(pos: Int, limit: Int, capacity: Int,
      content: ElementType*): BufferType = {
    val buf = allocBuffer(pos, limit, capacity)
    buf.put(content.toArray)
    buf.position(pos)
    buf
  }
}

object BufferFactory {
  abstract class ByteBufferFactory extends BufferFactory {
    type BufferType = ByteBuffer
    type ElementType = Byte

    implicit val elemClassTag: ClassTag[ElementType] = ClassTag.Byte

    implicit def elemFromInt(value: Int): ElementType = value.toByte

    implicit def elemToJSAny(elem: ElementType): js.Any =
      js.Any.fromByte(elem)

    implicit def bufferAdapter(
        buffer: BufferType): BufferAdapter[BufferType, ElementType] =
      new BufferAdapter.ByteBufferAdapater(buffer)
  }

  abstract class CharBufferFactory extends BufferFactory {
    type BufferType = CharBuffer
    type ElementType = Char

    implicit val elemClassTag: ClassTag[ElementType] = ClassTag.Char

    implicit def elemFromInt(value: Int): ElementType = value.toChar

    implicit def elemToJSAny(elem: ElementType): js.Any =
      js.Any.fromInt(elem.toInt)

    implicit def bufferAdapter(
        buffer: BufferType): BufferAdapter[BufferType, ElementType] =
      new BufferAdapter.CharBufferAdapater(buffer)
  }

  abstract class ShortBufferFactory extends BufferFactory {
    type BufferType = ShortBuffer
    type ElementType = Short

    implicit val elemClassTag: ClassTag[ElementType] = ClassTag.Short

    implicit def elemFromInt(value: Int): ElementType = value.toShort

    implicit def elemToJSAny(elem: ElementType): js.Any =
      js.Any.fromShort(elem)

    implicit def bufferAdapter(
        buffer: BufferType): BufferAdapter[BufferType, ElementType] =
      new BufferAdapter.ShortBufferAdapater(buffer)
  }

  abstract class IntBufferFactory extends BufferFactory {
    type BufferType = IntBuffer
    type ElementType = Int

    implicit val elemClassTag: ClassTag[ElementType] = ClassTag.Int

    implicit def elemFromInt(value: Int): ElementType = value.toInt

    implicit def elemToJSAny(elem: ElementType): js.Any =
      js.Any.fromInt(elem)

    implicit def bufferAdapter(
        buffer: BufferType): BufferAdapter[BufferType, ElementType] =
      new BufferAdapter.IntBufferAdapater(buffer)
  }

  abstract class LongBufferFactory extends BufferFactory {
    type BufferType = LongBuffer
    type ElementType = Long

    implicit val elemClassTag: ClassTag[ElementType] = ClassTag.Long

    implicit def elemFromInt(value: Int): ElementType = value.toLong

    implicit def elemToJSAny(elem: ElementType): js.Any =
      js.Any.fromDouble(elem.toDouble)

    implicit def bufferAdapter(
        buffer: BufferType): BufferAdapter[BufferType, ElementType] =
      new BufferAdapter.LongBufferAdapater(buffer)
  }

  abstract class FloatBufferFactory extends BufferFactory {
    type BufferType = FloatBuffer
    type ElementType = Float

    implicit val elemClassTag: ClassTag[ElementType] = ClassTag.Float

    implicit def elemFromInt(value: Int): ElementType = value.toFloat

    implicit def elemToJSAny(elem: ElementType): js.Any =
      js.Any.fromFloat(elem)

    implicit def bufferAdapter(
        buffer: BufferType): BufferAdapter[BufferType, ElementType] =
      new BufferAdapter.FloatBufferAdapater(buffer)
  }

  abstract class DoubleBufferFactory extends BufferFactory {
    type BufferType = DoubleBuffer
    type ElementType = Double

    implicit val elemClassTag: ClassTag[ElementType] = ClassTag.Double

    implicit def elemFromInt(value: Int): ElementType = value.toDouble

    implicit def elemToJSAny(elem: ElementType): js.Any =
      js.Any.fromDouble(elem)

    implicit def bufferAdapter(
        buffer: BufferType): BufferAdapter[BufferType, ElementType] =
      new BufferAdapter.DoubleBufferAdapater(buffer)
  }

  trait WrappedBufferFactory extends BufferFactory {
    protected def baseWrap(array: Array[ElementType]): BufferType

    protected def baseWrap(array: Array[ElementType],
        offset: Int, length: Int): BufferType

    def allocBuffer(capacity: Int): BufferType =
      baseWrap(new Array[ElementType](capacity))

    override def allocBuffer(pos: Int, limit: Int, capacity: Int): BufferType =
      baseWrap(new Array[ElementType](capacity), pos, limit-pos)

    override def withContent(pos: Int, limit: Int, capacity: Int,
        content: ElementType*): BufferType = {
      val after = capacity - (pos + content.size)
      val fullContent =
        (Seq.fill(pos)(elemFromInt(0)) ++
            content ++
            Seq.fill(after)(elemFromInt(0))).toArray
      baseWrap(fullContent, pos, limit - pos)
    }
  }

  trait ReadOnlyBufferFactory extends BufferFactory {
    override val createsReadOnly = true

    abstract override def allocBuffer(capacity: Int): BufferType =
      super.allocBuffer(capacity).asReadOnlyBuffer()

    override def allocBuffer(pos: Int, limit: Int, capacity: Int): BufferType =
      super.allocBuffer(pos, limit, capacity).asReadOnlyBuffer()

    override def withContent(pos: Int, limit: Int, capacity: Int,
        content: ElementType*): BufferType =
      super.withContent(pos, limit, capacity, content: _*).asReadOnlyBuffer()
  }

  trait SlicedBufferFactory extends BufferFactory {
    abstract override def allocBuffer(capacity: Int): BufferType = {
      if (capacity < 0)
        throw new IllegalArgumentException
      val buf = super.allocBuffer(capacity+25)
      buf.position(17)
      buf.limit(17+capacity)
      buf.slice()
    }

    override def withContent(pos: Int, limit: Int, capacity: Int,
        content: ElementType*): BufferType = {
      if (!(0 <= pos && pos <= limit && limit <= capacity))
        throw new IllegalArgumentException
      val buf = super.allocBuffer(capacity+25)
      buf.position(9+pos)
      buf.put(content.toArray)
      buf.position(9)
      buf.limit(9+capacity)
      val buf2 = buf.slice()
      buf2.position(pos)
      buf2.limit(limit)
      buf2
    }
  }
}
