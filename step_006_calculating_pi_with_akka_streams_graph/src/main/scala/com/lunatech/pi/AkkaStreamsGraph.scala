package com.lunatech.pi

import java.math.{MathContext => MC}

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.FlowShape
import akka.stream.scaladsl.{Balance, Flow, GraphDSL, Merge, Sink, Source}

import scala.math.{BigDecimal => ScalaBigDecimal}
import scala.util.{Failure, Success}

object AkkaStreamsGraph {
  def main(args: Array[String]): Unit = {

    implicit val system: ActorSystem = akka.actor.ActorSystem("pi-system")
    import system.dispatcher

    val RunParams(iterationCount, precision) = Helpers.getRunParams(args)

    Helpers.printMsg(s"Iteration count = $iterationCount - Precision = $precision")

    implicit val prec: MC = new MC(precision)

    object BigDecimal {
      def apply(d: Int)(implicit mc: MC): BigDecimal = ScalaBigDecimal(d, mc)
    }

    def piBBPdeaTermI(i: Int): BigDecimal = {
      BigDecimal(1) / BigDecimal(16).pow(i) * (
        BigDecimal(4) / (8 * i + 1) -
        BigDecimal(2) / (8 * i + 4) -
        BigDecimal(1) / (8 * i + 5) -
        BigDecimal(1) / (8 * i + 6)
        )
    }

    val indexes = Source(iterationCount to 0 by -1)

    val termProcessor: Flow[Int, BigDecimal, NotUsed] =
        Flow.fromFunction[Int, BigDecimal](n => piBBPdeaTermI(n))

    val parallelPiTermProcessor: Flow[Int, BigDecimal, NotUsed] = Flow.fromGraph(GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

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
    Helpers.printMsg(s"BigDecimal precision settings: ${implicitly[MC]}")

    val startTime = System.currentTimeMillis

    val sumOfTerms = Sink.fold[BigDecimal, BigDecimal](BigDecimal(0)) { case (acc, term) =>
      acc + term}

    val piF = indexes
      .via(parallelPiTermProcessor)
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
}