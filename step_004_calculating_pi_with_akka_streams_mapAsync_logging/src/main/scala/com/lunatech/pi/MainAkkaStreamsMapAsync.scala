package com.lunatech.pi

import java.math.{MathContext as MC}

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Sink, Source}

import scala.concurrent.Future
import scala.math.{BigDecimal as ScalaBigDecimal}
import scala.util.{Failure, Success}

object MainAkkaStreamsMapAsync:
  import Helpers._

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

    Helpers.printMsg(s"Calculating π with $iterationCount terms")
    Helpers.printMsg(s"BigDecimal precision settings: ${summon[MC]}")
    Helpers.printMsg(s"Memory size to encode BigDecimal at precision=${precision} = ${36 + math.ceil(precision * math.log(10)/8.0)} bytes")

    val startTime = System.currentTimeMillis

    val sumOfTerms = Sink.fold[BigDecimal, BigDecimal](BigDecimal(0)) {
      case (acc, term) => acc + term
    }

    val piF: Future[BigDecimal] = indexes
      .logAtInfo("pi-index")
      .mapAsync(Settings.parallelism)(i => Future(piBBPdeaTermI(i)))
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
