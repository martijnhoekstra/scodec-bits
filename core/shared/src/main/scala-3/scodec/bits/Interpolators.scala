/*
 * Copyright (c) 2013, Scodec
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package scodec.bits

import scala.quoted._

/**
  * Provides the `hex` string interpolator, which returns `ByteVector` instances from hexadecimal strings.
  * 
  * @example {{{
  * scala> val b = hex"deadbeef"
  * val b: scodec.bits.ByteVector = ByteVector(4 bytes, 0xdeadbeef)
  * }}}
  */
inline def (inline ctx: StringContext).hex (inline args: Any*): ByteVector =
  ${Literals.validate(Literals.Hex, 'ctx, 'args)}

/**
  * Provides the `bin` string interpolator, which returns `BitVector` instances from binary strings.
  *
  * @example {{{
  * scala> val b = bin"1010101010"
  * val b: scodec.bits.BitVector = BitVector(10 bits, 0xaa8)
  * }}}
  */
inline def (inline ctx: StringContext).bin (inline args: Any*): BitVector =
  ${Literals.validate(Literals.Bin, 'ctx, 'args)}

object Literals {

  trait Validator[A] {
    def validate(s: String): Option[String]
    def build(s: String)(using QuoteContext): Expr[A]
  }

  def validate[A](validator: Validator[A], strCtxExpr: Expr[StringContext], argsExpr: Expr[Seq[Any]])(using QuoteContext): Expr[A] = {
    strCtxExpr.unlift match {
      case Some(sc) => validate(validator, sc.parts, argsExpr)
      case None =>
        report.error("StringContext args must be statically known")
        ???
    }
  }

  private def validate[A](validator: Validator[A], parts: Seq[String], argsExpr: Expr[Seq[Any]])(using QuoteContext): Expr[A] = {
    if (parts.size == 1) {
      val literal = parts.head
      validator.validate(literal) match {
        case Some(err) =>
          report.error(err)
          ???
        case None =>
          validator.build(literal)
      }
    } else {
      report.error("interpolation not supported", argsExpr)
      ???
    }
  }

  object Hex extends Validator[ByteVector] {
    def validate(s: String): Option[String] =
      ByteVector.fromHex(s).fold(Some("hexadecimal string literal may only contain characters [0-9a-fA-f]"))(_ => None)
    def build(s: String)(using QuoteContext): Expr[ByteVector] =
      '{ByteVector.fromValidHex(${Expr(s)})},
  }    

  object Bin extends Validator[BitVector] {
    def validate(s: String): Option[String] =
      ByteVector.fromBin(s).fold(Some("binary string literal may only contain characters [0, 1]"))(_ => None)
    def build(s: String)(using QuoteContext): Expr[BitVector] =
      '{BitVector.fromValidBin(${Expr(s)})},
  }
}
