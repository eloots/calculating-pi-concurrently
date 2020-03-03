package com.lunatech.pi

import akka.stream.Attributes
import akka.stream.scaladsl.Source

import scala.util.control.NonFatal
import scala.io.{Source => IOSource}
import Console.{GREEN, RED, RESET}

case class RunParams(iterationCount: Int, precision: Int)

object Helpers {

  private def getInt(intString: String, errorMessage: String): Int = {
    try { intString.toInt}
    catch {
      case NonFatal(e) =>
        System.err.println(s"${errorMessage}: ${intString}")
        System.exit(0)
        0
    }
  }

  def getRunParams(args: Array[String]): RunParams = {
    if (args.length != 2) {
      System.err.print(s"${RED}Usage: run ITERATIONCOUNT PRECISION${RESET}")
      System.exit(0)
    }
    RunParams(
      getInt(args(0), "Invalid iteration count"),
      getInt(args(1), "Invalid precision")
    )
  }

  def printMsg(msg: => String): Unit =
    println(s"${GREEN}${msg}${RESET}")

  def printCalculationTime(startTime: Long, stopTime: Long): Unit = {
    println(f"${GREEN}Calculation time: ${1.0 / 1000 * (stopTime - startTime)}%.3f${RESET}")
  }

  val piReference: BigDecimal = {
    val piSource = IOSource.fromFile(Settings.piReferenceFile)
    val piReference: String = try {
      piSource.getLines().take(1).next
    }
    catch {
      case NonFatal(nf) =>
        System.err.print(s"${RED}Error reading π reference value${RESET}")
        System.exit(1)
        ""
    }
    finally piSource.close()
    BigDecimal(piReference)
  }

  implicit class LogExt[+A, +M](val flow: Source[A, M]) {
    def withMyLogger(name: String): Source[A, M] =
      flow
        .log(name, identity)
        .withAttributes(
          Attributes.logLevels(
            onElement = Attributes.LogLevels.Info,
            onFinish = Attributes.LogLevels.Info,
            onFailure = Attributes.LogLevels.Error)
        )
  }
}
