package spinal

import scala.collection.immutable.Range
import scala.collection.mutable.ArrayBuffer
import scala.language.experimental.macros

package object core extends BaseTypeFactory with BaseTypeCast{
  import languageFeature._
  implicit lazy val implicitConversions = scala.language.implicitConversions
  implicit lazy val reflectiveCalls = scala.language.reflectiveCalls
  implicit lazy val postfixOps = scala.language.postfixOps


  implicit def IntToBuilder(value: Int) = new IntBuilder(value)
  implicit def BigIntToBuilder(value: BigInt) = new BigIntBuilder(value)
  implicit def DoubleToBuilder(value: Double) = new DoubleBuilder(value)

  //def enum(param: Symbol*) = MacroTest.enum(param)
  def enum(param: Symbol*): Any = macro MacroTest.enum_impl


  //implicit def EnumElementToCraft[T <: SpinalEnum](element : SpinalEnumElement[T]) : SpinalEnumCraft[T] = element()
//  implicit def EnumElementToCraft[T <: SpinalEnum](enumDef : T) : SpinalEnumCraft[T] = enumDef.craft().asInstanceOf[SpinalEnumCraft[T]]
//  implicit def EnumElementToCraft2[T <: SpinalEnum](enumDef : SpinalEnumElement[T]) : SpinalEnumCraft[T] = enumDef.craft().asInstanceOf[SpinalEnumCraft[T]]

  class IntBuilder(val i: Int) extends AnyVal{
//    def x[T <: Data](dataType : T) : Vec[T] = Vec(dataType,i)

    def downto(start: Int): Range.Inclusive = Range.inclusive(start, i)

    def bit = new BitCount(i)
    def exp = new ExpCount(i)

    def hr = new STime(i * 3600)
    def min = new STime(i * 60)
    def sec = new STime(i * 1)
    def ms = new STime(i * 1e-3)
    def us = new STime(i * 1e-6)
    def ns = new STime(i * 1e-9)
    def ps = new STime(i * 1e-12)
    def fs = new STime(i * 1e-15)
  }

  case class BigIntBuilder(i: BigInt) {
    def bit = new BitCount(i.toInt)
    def exp = new ExpCount(i.toInt)
  }

  case class DoubleBuilder(d: Double) {
    def hr = new STime(d * 3600)
    def min = new STime(d * 60)
    def sec = new STime(d * 1)
    def ms = new STime(d * 1e-3)
    def us = new STime(d * 1e-6)
    def ns = new STime(d * 1e-9)
    def ps = new STime(d * 1e-12)
    def fs = new STime(d * 1e-15)
  }

  def True = Bool(true)
  def False = Bool(false)


 // implicit def RegRefToReg[T <: Data](that : RegRef[T]) : T = that.getReg


  implicit def IntToUInt(that: Int) = U(that)
  implicit def BigIntToUInt(that: BigInt) = U(that)

  implicit def IntToSInt(that: Int) = S(that)
  implicit def BigIntToSInt(that: BigInt) = S(that)

  implicit def IntToBits(that: Int) = B(that)
  implicit def BigIntToBits(that: BigInt) = B(that)


  implicit def StringToBits(that: String) = bitVectorStringParser(spinal.core.B, that)
  implicit def StringToUInt(that: String) = bitVectorStringParser(spinal.core.U, that)
  implicit def StringToSInt(that: String) = bitVectorStringParser(spinal.core.S, that)

  implicit class LiteralBuilder(private val sc: StringContext) extends AnyVal {
    def B(args: Any*): Bits = bitVectorStringParser(spinal.core.B, getString(args))
    def U(args: Any*): UInt = bitVectorStringParser(spinal.core.U, getString(args))
    def S(args: Any*): SInt = bitVectorStringParser(spinal.core.S, getString(args))
    def M(args: Any*): MaskedLiteral = MaskedLiteral(sc.parts(0))


    def Bits(args: Any*): Bits = B(args)
    def UInt(args: Any*): UInt = U(args)
    def SInt(args: Any*): SInt = S(args)

    private def getString(args: Any*): String = {
     // println(sc.parts.size + " " + args.size)
     // println(sc.parts.head + "-----" + args.head)
    //  sc.standardInterpolator(_.toString(), args)

      val pi = sc.parts.iterator
      val ai = args.iterator
      val bldr = new StringBuilder(pi.next().toString)
      while (ai.hasNext) {
        if(ai.hasNext && !ai.next.isInstanceOf[List[_]])bldr append ai.next
        if(pi.hasNext && !pi.next.isInstanceOf[List[_]])bldr append pi.next
      }
      //println(bldr.result)
      bldr.result.replace("_", "")
    }
  }


  private[core] def bitVectorStringParser[T <: BitVector](builder: BitVectorLiteralFactory[T], arg: String): T = {
    var last = 0;
    var idx = 0
    val cleanedArg = arg.replace("_", "")
    val cleanedArgSize = cleanedArg.size
    if (cleanedArg.charAt(0).isLetter) {
      val tail = cleanedArg.tail
      val radix = getRadix(cleanedArg.charAt(0))
      val value = BigInt(tail, radix)
      val minus = tail.charAt(0) == '-'
      val digitCount = tail.size - (if (minus) 1 else 0)
      radix match {
        case 16 => return builder(value, digitCount * 4 bit)
        case 10 => return builder(value)
        case 8 => return builder(value, digitCount * 3 bit)
        case 2 => return builder(value, digitCount bit)
      }
      return ???
    }else if(cleanedArg.contains(''')){
      val tildPos = cleanedArg.indexOf(''')
      val bitCount = cleanedArg.substring(0,tildPos).toInt
      val radix = getRadix(cleanedArg.charAt(tildPos+1))
      val value = BigInt(cleanedArg.substring(tildPos + 2,cleanedArgSize), radix)
      return builder(value, new BitCount(bitCount))
    }else if("01".contains(cleanedArg.charAt(0))){
      val value = if(cleanedArg != "") BigInt(cleanedArg,2) else BigInt(0)
      return builder(value,cleanedArgSize bit)
    }


    def getRadix(that: Char): Int = that match {
      case 'x' => 16
      case 'h' => 16
      case 'd' => 10
      case 'o' => 8
      case 'b' => 2
      case _ => SpinalError(s"$that is not a valid radix specification. x-d-o-b are allowed")
    }


    return SpinalError(s"$arg literal is not well formed [bitCount'][radix]value")
  }

  //implicit def UIntToLitBuilder(sc: StringContext) = new UIntLitBuilder(sc)

  //  implicit def IntToUInt(that : Int) = UInt(that lit)
  //  implicit def BigIntToUInt(that : BigInt) = UInt(that lit)
  //
  // implicit def BooleanToBool(that : Boolean) = Bool(that)
  implicit def DataPimped[T <: Data](that : T) = new DataPimper(that)
  implicit def BitVectorPimped[T <: BitVector](that : T) = new BitVectorPimper(that)

//    implicit def autoCast[T <: Data, T2 <: T](that: T): T2#SSelf = that.asInstanceOf[T2#SSelf]
//  implicit def autoCast[T <: Data](that: T): T#SSelf = that.asInstanceOf[T#SSelf]



  implicit class SIntPimper(pimped: SInt) {
    def toSFix: SFix = {
      val width = pimped.getWidth
      val fix = SFix(width - 1 exp, width bit)
      fix.raw := pimped
      fix
    }
  }

  implicit class UIntPimper(pimped: UInt) {
    def toUFix : UFix = {
      val width = pimped.getWidth
      val fix = UFix(width exp, width bit)
      fix.raw := pimped
      fix
    }
  }

}