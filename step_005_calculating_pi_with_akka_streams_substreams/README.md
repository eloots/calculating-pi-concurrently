# Calculating π concurrently - Akka Streams based version

This program calculates the number π to a given precision using
the [Bailey-Borwein-Plouffe formula](https://en.wikipedia.org/wiki/Bailey–Borwein–Plouffe_formula for details)

In this implementation, we use [Akka Streams](https://doc.akka.io/docs/akka/current/stream/)
to implement a parallelised version of the formula. More specific, we use 
so-called `Substreams` to split a single stream of elements into a number
of "internal" streams on which we can run our calculations. The calculation
is done by mapping the function that calculates a term over each term.
Note the presence of the `.async` method on the `map`. You may want
to run the program again without this `.async` and see what the effect
is on the calculation time. 

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