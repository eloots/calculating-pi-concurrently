# Calculating π concurrently - Akka Streams based version

This program calculates the number π to a given precision using
the [Bailey-Borwein-Plouffe formula](https://en.wikipedia.org/wiki/Bailey–Borwein–Plouffe_formula for details)

For reference only...
Yet another implementation is also based on Akka Streams... One can
consider it as an example of `a bad implementation` of the calculation
of π. 

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