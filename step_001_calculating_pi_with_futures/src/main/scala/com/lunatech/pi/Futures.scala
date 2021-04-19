package com.lunatech.pi

import java.math.MathContext
import java.util.concurrent.ForkJoinPool

import scala.concurrent.*
import scala.math.{BigDecimal as ScalaBigDecimal}
import scala.util.{Failure, Success}

object Main:
  def main(args: Array[String]): Unit =

    val RunParams(iterationCount, precision) = Helpers.getRunParams(args)

    Helpers.printMsg(s"Iteration count = $iterationCount - Precision = $precision")

    given MathContext = new MathContext(precision)

    object BigDecimal:
      def apply(d: Int)(using mc: MathContext): BigDecimal = ScalaBigDecimal(d, mc)

    def piBBPdeaPart(offset: Int, n: Int): BigDecimal =
      def piBBPdeaTermI(i: Int): BigDecimal =
        BigDecimal(1) / BigDecimal(16).pow(i) * (
          BigDecimal(4) / (8 * i + 1) -
          BigDecimal(2) / (8 * i + 4) -
          BigDecimal(1) / (8 * i + 5) -
          BigDecimal(1) / (8 * i + 6)
        )
      println(s"Started @ offset: $offset ")
      (offset until offset + n).foldLeft((BigDecimal(0))) {
        case (acc, i) => acc + piBBPdeaTermI(i)
      }

    val fjPool = new ForkJoinPool(Settings.parallelism)

    given ExecutionContextExecutor = ExecutionContext.fromExecutor(fjPool)

    val N = iterationCount
    val nChunks = 64
    val chunkSize = (N + nChunks - 1) / nChunks
    val offsets = 0 to N by chunkSize
    Helpers.printMsg(s"Calculating π with ${nChunks * chunkSize} terms in $nChunks chunks of $chunkSize terms each")
    Helpers.printMsg(s"Threadpool size: ${Settings.parallelism}")
    Helpers.printMsg(s"BigDecimal precision settings: ${summon[MathContext]}")

    val startTime = System.currentTimeMillis

    val piChunks: Future[Seq[BigDecimal]] =
      Future.sequence(
        for  offset <- offsets 
          yield Future(piBBPdeaPart(offset, chunkSize))
      )

    val piF: Future[BigDecimal] = piChunks.map(_.sum)

    piF.onComplete {
      case Success(pi) =>
        val stopTime = System.currentTimeMillis
        println(s"Pi:      ${pi}")
        val delta = pi - Helpers.piReference
        Helpers.printMsg(s"|Delta|: ${delta(new MathContext(8)).abs}")
        Helpers.printCalculationTime(startTime, stopTime)
        fjPool.shutdown()
      case Failure(e) =>
        println(s"An error occurred: ${e}")
        fjPool.shutdown()
    }
