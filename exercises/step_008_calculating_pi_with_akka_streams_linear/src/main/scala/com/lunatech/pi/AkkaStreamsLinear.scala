package com.lunatech.pi

import java.math.{MathContext as MC}

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Sink, Source}

import scala.math.{BigDecimal as ScalaBigDecimal}
import scala.util.{Failure, Success}

object AkkaStreamsLinear {
  def main(args: Array[String]): Unit =

    given system: ActorSystem = akka.actor.ActorSystem("pi-system")
    import system.dispatcher

    val RunParams(iterationCount, precision) = Helpers.getRunParams(args)

    Helpers.printMsg(s"Iteration count = $iterationCount - Precision = $precision")

    given MC = new MC(precision)

    object BigDecimal:
      def apply(d: Int)(using mc: MC): BigDecimal = ScalaBigDecimal(d, mc)

    def piBBPdeaTermI(i: Int): BigDecimal =
      BigDecimal(1) / BigDecimal(16).pow(i) * (
        BigDecimal(4) / (8 * i + 1) -
          BigDecimal(2) / (8 * i + 4) -
          BigDecimal(1) / (8 * i + 5) -
          BigDecimal(1) / (8 * i + 6)
        )

    val indexes = Source(iterationCount to 0 by -1)

    object TermProcessor:
      def apply(select: Int, par: Int): Flow[(Int, BigDecimal), (Int, BigDecimal), NotUsed] = 
        Flow.fromFunction[(Int, BigDecimal), (Int, BigDecimal)] {
          case (n, partialValue) if n % par == select =>
            (n, piBBPdeaTermI(n))
          case passthrough =>
            passthrough
        }

    Helpers.printMsg(s"Calculating π with $iterationCount terms")
    Helpers.printMsg(s"BigDecimal precision settings: ${summon[MC]}")
    println

    val startTime = System.currentTimeMillis

    /**
     * As an experiment, run the calculations asynchronously on 12 term-processors
     * and a custom dispatcher
     * Note that this is not a very practical approach as this is hard-coding the
     * level of parallelism
     */
    val piF = indexes.map { n => (n, BigDecimal(0))}
      .via(TermProcessor(0, 10).async("pi-dispatcher"))
      .via(TermProcessor(1, 10).async("pi-dispatcher"))
      .via(TermProcessor(2, 10).async("pi-dispatcher"))
      .via(TermProcessor(3, 10).async("pi-dispatcher"))
      .via(TermProcessor(4, 10).async("pi-dispatcher"))
      .via(TermProcessor(5, 10).async("pi-dispatcher"))
      .via(TermProcessor(6, 10).async("pi-dispatcher"))
      .via(TermProcessor(7, 10).async("pi-dispatcher"))
      .via(TermProcessor(8, 10).async("pi-dispatcher"))
      .via(TermProcessor(9, 10).async("pi-dispatcher"))
      .via(TermProcessor(10, 11).async("pi-dispatcher"))
      .runWith(Sink.fold(BigDecimal(0)) { case (acc, (_, term)) =>
        acc + term}
      )

    piF.onComplete {
      case Success(pi) =>
        val stopTime = System.currentTimeMillis
        println(s"Pi:      ${pi}")
        val delta = pi - Helpers.piReference
        Helpers.printMsg(s"|Delta|: ${delta(new MC(8)).abs}")
        Helpers.printCalculationTime(startTime, stopTime)
        system.terminate()
      case Failure(e) =>
        println(s"An error occurred: ${e}")
        system.terminate()
    }
}