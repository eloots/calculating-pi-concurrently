# Calculating π concurrently - Akka Streams based version

This program calculates the number π to a given precision using
the [Bailey-Borwein-Plouffe formula](https://en.wikipedia.org/wiki/Bailey–Borwein–Plouffe_formula for details)

In this implementation, we use [Akka Streams](https://doc.akka.io/docs/akka/current/stream/)
to implement a parallelised version of the formula. More specific, we use the `mapAsync`
combinator on `akka.stream.scaladsl.`

Usage: `run ITERATION_COUNT PRECISION`
Parameters:
   - `ITERATION_COUNT`: the number `n` in the formula
   - `PRECISION`: the calculations are done using Java's
                `BigDecimal` with the given precision

There's is an optimal precision setting for a given iteration count;
if the result is represented in base 16, every step in the calculation
adds a single digit. Because we perform the calculation in base 10, each step
adds ± 1.2 decimal digits (`log 16` ~ 1.204). So, suppose we calculate the
formula with 10.000 terms, the optimal precision is about 12.000.