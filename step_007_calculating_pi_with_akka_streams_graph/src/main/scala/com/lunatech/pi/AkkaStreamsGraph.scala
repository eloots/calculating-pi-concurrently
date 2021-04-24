package com.lunatech.pi

import java.math.{MathContext as MC}

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.FlowShape
import akka.stream.scaladsl.{Balance, Flow, GraphDSL, Merge, Sink, Source}

import scala.math.{BigDecimal as ScalaBigDecimal}
import scala.util.{Failure, Success}

object AkkaStreamsGraph {
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

    val termProcessor: Flow[Int, BigDecimal, NotUsed] =
        Flow.fromFunction[Int, BigDecimal](n => piBBPdeaTermI(n))

    /**
     * As an experiment, run the calculations asynchronously on 12 term-processors and
     * a custom dispatcher. We're using the Akka Stream graph DSL to 'balance' the
     * incoming stream of term indexes to 12 'sub' streams that are processed by a
     * term-processor after which the results are merged back into a single stream
     * Note that this is not a very practical approach as this is hard-coding the
     * level of parallelism
     */
    val parallelPiTermProcessor: Flow[Int, BigDecimal, NotUsed] = Flow.fromGraph(GraphDSL.create() { builder =>
      given akka.stream.scaladsl.GraphDSL.Builder[NotUsed] = builder
      import GraphDSL.Implicits.*

      val dispatcher = builder.add(Balance[Int](12))
      val merger = builder.add(Merge[BigDecimal](12))

      dispatcher.out(0) ~> termProcessor.async("pi-dispatcher") ~> merger.in(0)
      dispatcher.out(1) ~> termProcessor.async("pi-dispatcher") ~> merger.in(1)
      dispatcher.out(2) ~> termProcessor.async("pi-dispatcher") ~> merger.in(2)
      dispatcher.out(3) ~> termProcessor.async("pi-dispatcher") ~> merger.in(3)
      dispatcher.out(4) ~> termProcessor.async("pi-dispatcher") ~> merger.in(4)
      dispatcher.out(5) ~> termProcessor.async("pi-dispatcher") ~> merger.in(5)
      dispatcher.out(6) ~> termProcessor.async("pi-dispatcher") ~> merger.in(6)
      dispatcher.out(7) ~> termProcessor.async("pi-dispatcher") ~> merger.in(7)
      dispatcher.out(8) ~> termProcessor.async("pi-dispatcher") ~> merger.in(8)
      dispatcher.out(9) ~> termProcessor.async("pi-dispatcher") ~> merger.in(9)
      dispatcher.out(10) ~> termProcessor.async("pi-dispatcher") ~> merger.in(10)
      dispatcher.out(11) ~> termProcessor.async("pi-dispatcher") ~> merger.in(11)
      FlowShape(dispatcher.in, merger.out)
    })

    Helpers.printMsg(s"Calculating π with $iterationCount terms")
    Helpers.printMsg(s"BigDecimal precision settings: ${summon[MC]}")

    val startTime = System.currentTimeMillis

    val sumOfTerms = Sink.fold[BigDecimal, BigDecimal](BigDecimal(0)) { case (acc, term) =>
      acc + term}

    val piF = indexes
      .via(parallelPiTermProcessor).async
      .runWith(sumOfTerms)

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