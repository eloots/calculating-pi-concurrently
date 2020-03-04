Introduction
============

With the prevalence of multi-CPU computers and multi-core CPUs, the opportunity
exists to speed-up the execution of applications in general and specifically
algorithms that lend themselves to parallelisation.

If we look at the latter, we may find that the algorithm can be decomposed in
subtasks which can be executed in parallel. Getting the final result then
consists of combining the sub-results of the subtasks. There we can distinguish
two cases. In the first case, the order in which the sub-results are composed
doesn't matter, whereas in the second case, it does.

An example of the first case is when the sub-results are numbers and the final
result is their sum. Obviously, the order in which the numbers are added doesn't
change the result (to be complete, effects of rounding due to finite precision
_can_ impact the final result, but that's an interesting but entirely different
topic). An example where order definitely matters is if we multiply a series of
matrices: if we split the multiplication into groups, calculate the product of
the matrices in each group, then the order in which we combine the sub-results
_does_ matter!

In Scala, we have different tools available to write applications that execute
code concurrently, and if we execute this code on a multi-CPU or multi-core
computer, we can achieve parallelism in the execution of this code.

A first tool we have is Akka: Akka allows us to create so-called Actors that can
execute code. This execution can be triggered by sending an Actor a message. In
the context of concurrent execution of code, we can imagine a system where we
create a pool of Actors that execute code concurrently. This execution model
allows us to implement concurrent execution of sub-tasks that fall in the first
category mentioned earlier: unless we take specific measures, we have no control
over the order in which sub-results are returned. Conclusion is that the Actor
model can be applied in a straightforward way to problems of the first type.

A second tool we have at our disposal is Scala's `Future`. This API allows
us to tackle both of the aforementioned cases.

Let's see how `Future` can be used to implement a parallelised computation of
the number π.

Calculating π
=============

Ok, this topic certainly has been beaten to death, but it's easy to understand,
so we may just as well use it for demonstration purposes...

First we need an algorithm to calculate π. I chose the [_Bailey–Borwein–Plouffe
formula_](https://en.wikipedia.org/wiki/Bailey–Borwein–Plouffe_formula) _(BBP)_
discovered in 1995 by _Simon Plouffe_ :

![BBP digit extraction algorithm](images/Pi-formula.png)

This algorithm has a very interesting property (that we won't exploit in our
case): if the calculation is performed in hexadecimal number base, the algorithm
can produce any individual digit of π without calculating all the digits
preceding it! In this way, it is often used as a mechanism to verify the
correctness of the calculated value of π using a different algorithm.

Let's have a look at how we implement the calculation of π using `Future`.

On precision
------------

Obviously, if we calculate an approximation of π by calculating the sum of the
`N` first terms of the _BBP_ formula shown earlier, we must calculate each term
with sufficient precision. If we would perform the calculation using `Double`,
we wouldn't be able to calculate many digits...

Let's use `BigDecimal` instead. `BigDecimal` allows us to create numbers of
practically arbitrary precision specified in a so-called MathContext. Let's
create some numbers with 100 digit precision and perform a calculation with
them:

```scala
scala> import java.math.{ MathContext => MC }
import java.math.{MathContext=>MC}

scala> val mc = new MC(100)
mc: java.math.MathContext = precision=100 roundingMode=HALF_UP

scala> val one = BigDecimal(1)(mc)
one: scala.math.BigDecimal = 1

scala> val seven = BigDecimal(7)(mc)
seven: scala.math.BigDecimal = 7

scala> println((one/seven).toString)
0.1428571428571428571428571428571428571428571428571428571428571428571428571428571428571428571428571429
```

Each time we create a `BigDecimal` number we need to specify the required
precision by supplying the corresponding MathContext. A bit clumsy, so let's
start by a 'workaround' utilizing implicits such that we use `BigDecimal` in a
more straightforward way:

```scala
scala> import java.math.{ MathContext => MC }
import java.math.{MathContext=>MC}

scala> import scala.math.{ BigDecimal => ScalaBigDecimal}
import scala.math.{BigDecimal=>ScalaBigDecimal}

scala> object BigDecimal {
     |   def apply(d: Double)(implicit mc: MC) = ScalaBigDecimal(d)(mc)
     | }
defined object BigDecimal

scala> implicit val mc = new MC(100)
mc: java.math.MathContext = precision=100 roundingMode=HALF_UP

scala> val one = BigDecimal(1)
one: scala.math.BigDecimal = 1.0

scala> val seven = BigDecimal(7)
seven: scala.math.BigDecimal = 7.0

scala> println((one/seven).toString)
0.1428571428571428571428571428571428571428571428571428571428571428571428571428571428571428571428571429
```

If you're unfamiliar with Scala's implicits, the above may look a bit strange.
In fact it works as follows: in object `BigDecimal`, we define an apply method
that has two argument lists. The purpose of the first one is obvious. The second
one takes a single argument, namely a `MathContext`. This argument (in fact,
this applies to the argument list as a whole) is marked implicit. What this will
do is that the compiler will, in the current execution context, look for a value
of type `MC` marked as implicit. The declaration `implicit val mc = new MC(100)`
meets that criterium. Hence, when we invoke a call to the apply method in our
`BigDecimal` object, value `mc` will automatically be passed as the second
argument.

The attentive user will also note that our apply method takes a `Double` as
argument. Later on we use our apply method with an `Int` as argument. This works
because there exists what is called an _implicit conversion_ from `Int` to
`Double`.

Cool, and now that we've got that out of the way, let's look at how we can
perform the calculation.

Implementing the BBP digit extraction algorithm
-----------------------------------------------

An obvious way to parallelize the calculation of the BPP formula shown above up
to N terms is to split the terms into `nChunks` chunks and to calculate the sum
of the terms in the chunks in parallel. When that's done, the final result (π)
is the sum of the partial sums.

For each chunk, we need to know its offset in the sequence of terms. Let's
generate a sequence that contains the offsets of the respective chunks for a
given `N` and `nChunks`. Note that the latter may not divide entirely into the
former, so we do a bit of rounding that may result in calculating extra terms:

```scala
scala> val N = 3000 N: Int = 3000

scala> val nChunks = 64
nChunks: Int = 64

scala> val chunkSize = (N + nChunks - 1) / nChunks
chunkSize: Int = 47

scala> val offsets = 0 until N by chunkSize
offsets: scala.collection.immutable.Range = Range(0, 47, 94, 141, 188, 235, 282, 329, 376, 423, 470, 517, 564, 611, 658, 705, 752, 799, 846, 893, 940, 987, 1034, 1081, 1128, 1175, 1222, 1269, 1316, 1363, 1410, 1457, 1504, 1551, 1598, 1645, 1692, 1739, 1786, 1833, 1880, 1927, 1974, 2021, 2068, 2115, 2162, 2209, 2256, 2303, 2350, 2397, 2444, 2491, 2538, 2585, 2632, 2679, 2726, 2773, 2820, 2867, 2914, 2961)

scala> println(s"Calculating π with ${nChunks*chunkSize} terms in $nChunks chunks of $chunkSize terms each")
Calculating π with 3008 terms in 64 chunks of 47 terms each
```

Next we define a method `piBBPdeaPart` that will calculate the sum of `n` terms
in the BBPDEA formula, starting at term `offset`.

```scala
def piBBPdeaPart(offset: Int, n: Int): BigDecimal = {

    def piBBPdeaTermI(i: Int): BigDecimal = {
      BigDecimal(1) / BigDecimal(16).pow(i) * (
      BigDecimal(4) / (8 * i + 1) -
      BigDecimal(2) / (8 * i + 4) -
      BigDecimal(1) / (8 * i + 5) -
      BigDecimal(1) / (8 * i + 6)
      )
    }
    println(s"Started @ offset: $offset ")
    (offset until offset + n).foldLeft((BigDecimal(0))) {
      case (acc, i) => acc + piBBPdeaTermI(i)
    }
  }
```

Relatively straightforward, and time to tie everything together. Note the
presence of a println statement that prints some text just before the
calculation of a partial sum starts.  Let's start by launching the calculation
of the sum of the chunks:

```scala
val piChunks: Future[Seq[BigDecimal]] = Future.sequence(offsets.map { offset =>
  Future(piBBPdeaPart(offset, chunkSize))
})
```

Two things are important to note. First we map each offset in `offsets` to a
Future[BigDecimal]; each instance will be scheduled for execution within an
execution context (that we haven't provided yet). What we end up with is a
sequence of Futures. Secondly, `Future.sequence` converts the
`Seq[Future[BigDecimal]]` into a `Future[Seq[BigDecimal]]`. Pretty awesome.

What remains to be done is to calculate the sum of the partial sums. We can do
this as follows:

```scala
val piF: Future[BigDecimal] = piChunks.map {
  case chunks =>
    chunks.foldLeft(BigDecimal(0)) { case (acc, chunk) => acc + chunk }
```

If the previous was awesome, this certainly is awesome++. Think about it: we're
performing a calculation on a Future, but it sure looks as if we're working on
the concrete thing: `piChunks` is a `Future[Seq[BigDecimal]]`.

When we apply map on this future, we can work with a lambda that works on a
`Seq[BigDecimal]`.

The relevant (simplified) part in the source code of `Future` is as follows:

```scala
trait Future[+T] extends Awaitable[T] {
  ...
  def map[S](f: T => S): Future[S] = {
  ...
  }
  ...
}
```

Variable `piF` is still a `Future[BigDecimal]`. So, if we want to have the final
result, we can in this case do nothing else but wait for the calculation to
complete. This is done as follows:

```scala
import scala.concurrent.duration.Duration.Inf
val pi: BigDecimal = Await.result(piF, Inf)
```

Execution context and thread pools
----------------------------------

The above code contains almost everything that is needed. However, if we compile
it, we get the following error:

```
Error:(54, 64) not enough arguments for method apply: (implicit executor: scala.concurrent.ExecutionContext)scala.concurrent.Future[scala.math.BigDecimal] in object Future.
Unspecified value parameter executor.
val piChunks = Future.sequence( offsets map {offset => Future(piBBPdeaPart(offset, chunkSize))})
                                                             ^
```

Looking at the (simplified) signature of `Future` we see the following:

```scala
object Future {
  ...
  def apply[T](body: =>T)(implicit executor: ExecutionContext): Future[T] = ...
  ...
}
```

So, we need to provide an ExecutionContext. An ExecutionContext will
provide the machinery (Threads) on which the Future code (body in the signature)
will be run.

We can provide an ExecutionContext in the following way:

```scala
val fjPool = new ForkJoinPool(8)

implicit val ec = ExecutionContext.fromExecutor(fjPool)
```

Here, we create a ForkJoinPool of 8 threads and create an ExecutionContext from
it. Since `ec` is declared as an implicit val, it will be picked-up by our calls
to Future.apply...

Wrap-up
-------

Following is the complete code, which contains the value of π with 20,000 digit
precision (copied from the Internet, so it must be correct ;-) ). Note that in
the code below I truncated this value as to make this article not too long).

```scala
package com.lunatech.pi

import java.math.{MathContext => MC}
import java.util.concurrent.ForkJoinPool
import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration.Inf
import scala.concurrent.forkjoin.ForkJoinPool
import scala.math.{BigDecimal => ScalaBigDecimal}

object Main extends App {
  implicit val mc4000 = new MC(4000)

  object BigDecimal {
    def apply(d: Double)(implicit mc: MC) = ScalaBigDecimal(d)(mc)
  }

  def getImplicitPrecision()(implicit mc: MC) = s"${mc}"

  def piBBPdeaPart(offset: Int, n: Int): BigDecimal = {

    def piBBPdeaTermI(i: Int): BigDecimal = {
      BigDecimal(1) / BigDecimal(16).pow(i) * (
        BigDecimal(4) / (8 * i + 1) -
        BigDecimal(2) / (8 * i + 4) -
        BigDecimal(1) / (8 * i + 5) -
        BigDecimal(1) / (8 * i + 6))
    }
    println(s"Started @ offset: $offset ")
    (offset until offset + n).foldLeft((BigDecimal(0))) {
      case (acc, i) => acc + piBBPdeaTermI(i)
    }
  }

  val pi20000 = ScalaBigDecimal(
    "3.1415926535897932384626433832795028841971693993751058209749445923..."
  )

  val fjPool = new ForkJoinPool(8)

  implicit val ec = ExecutionContext.fromExecutor(fjPool)

  val N = 3000
  val nChunks = 64
  val chunkSize = (N + nChunks - 1) / nChunks
  val offsets = 0 until N by chunkSize
  println(
    s"Calculating π with ${nChunks * chunkSize} terms in $nChunks chunks of $chunkSize terms each"
  )
  println(getImplicitPrecision())

  val startTime = System.currentTimeMillis

  val piChunks: Future[Seq[BigDecimal]] = Future.sequence(offsets.map {
    offset => Future(piBBPdeaPart(offset, chunkSize))
  })

  val piF: Future[BigDecimal] = piChunks.map {
    case chunks =>
      chunks.foldLeft(BigDecimal(0)) { case (acc, chunk) => acc + chunk }
  }

  val pi: BigDecimal = Await.result(piF, Inf)

  val stopTime = System.currentTimeMillis

  println(s"Pi:      ${pi}")
  println(s"Pi Ref:  ${pi20000}")
  val delta = pi - pi20000
  println(s"|Delta|: ${delta(new MC(8)).abs}")
  println(f"Calculation time: ${1.0 / 1000 * (stopTime - startTime)}%.2f")

  fjPool.shutdown()
}
```

When this program is executed on my laptop (a MacBook Pro with an Intel 4-core
i7 processor), it produces the following output (values of π truncated):

```
Calculating π with 3008 terms in 64 chunks of 47 terms each
precision=4000 roundingMode=HALF_UP
Started @ offset: 47
Started @ offset: 0
Started @ offset: 141
Started @ offset: 94
Started @ offset: 235
Started @ offset: 188
Started @ offset: 282
Started @ offset: 329
Started @ offset: 376
Started @ offset: 423
Started @ offset: 470
Started @ offset: 517
Started @ offset: 564
Started @ offset: 611
Started @ offset: 658
Started @ offset: 705
Started @ offset: 752
Started @ offset: 799
Started @ offset: 846
Started @ offset: 893
Started @ offset: 940
Started @ offset: 987
Started @ offset: 1034
Started @ offset: 1081
Started @ offset: 1128
Started @ offset: 1175
Started @ offset: 1222
Started @ offset: 1269
Started @ offset: 1316
Started @ offset: 1363
Started @ offset: 1410
Started @ offset: 1457
Started @ offset: 1504
Started @ offset: 1551
Started @ offset: 1598
Started @ offset: 1645
Started @ offset: 1692
Started @ offset: 1739
Started @ offset: 1786
Started @ offset: 1833
Started @ offset: 1880
Started @ offset: 1927
Started @ offset: 1974
Started @ offset: 2021
Started @ offset: 2068
Started @ offset: 2115
Started @ offset: 2162
Started @ offset: 2209
Started @ offset: 2256
Started @ offset: 2303
Started @ offset: 2350
Started @ offset: 2397
Started @ offset: 2444
Started @ offset: 2491
Started @ offset: 2538
Started @ offset: 2585
Started @ offset: 2632
Started @ offset: 2679
Started @ offset: 2726
Started @ offset: 2773
Started @ offset: 2820
Started @ offset: 2867
Started @ offset: 2914
Started @ offset: 2961
Pi: 3.141592653589793238462643383279502884197169399375105820974944592307816406286208998...
Pi Ref: 3.141592653589793238462643383279502884197169399375105820974944592307816406286208998...
|Delta|: 2.8076968E-3630
Calculation time: 4.02
```

What we can observe is that, with 3,003 terms, we have correctly calculated more
than 3,600 digits accurately.

If we set the size of the ForkJoinPool to 1, 2, 4, 8, 16, 32 we get the
following values for the calculation time (in seconds) respectively: 18.25,
9.23, 5.91, 4.02, 4.47, 5.86

So, we see a near linear speed-up by going from one thread to two threads. A
further increase of the thread-count doesn't yield a further linear speed-up:
this may be caused by different factors, not in the least by the fact that we
have a single chip processor with a shared on-chip cache. Of course, since it's
a four core CPU (with hyper-threads that don't yield the same performance as the
regular CPU threads), we don't get a speed-up beyond 8 threads in the
ForkJoinPool.

Conclusion
----------

Scala's `Future` API presents a very powerful way to perform asynchronous and
concurrent execution of code. Even though it may take some time to get one's
head around it, when you grasp it, it's pretty cool and very powerful.

Now, as for π, is this a realistic way to calculate this number to say
multi-million digit precision? Not really for multiple reasons.

First of all, consider that the current record holders have calculated
12,100,000,000,050 digits... Because the algorithm shown in this article runs in
memory, it wouldn't fit in memory on even the biggest SMP (_Symmetrical
Multiprocessing_) computer on the market (memory size limit is in the order of
Terra-bytes). Secondly, even if it _would_ fit in memory: as the execution time
of the calculation is proportional to the square of the precision used in the
calculation, it would take much longer than the time since the big bang occurred
to calculate π at this level of precision.

The record holders, Alexander J. Yee & Shigeru Kondo, calculated (and verified)
their result in about 94 days. So, they definitely used a different method. In
fact, it's really interesting to read about how they did it using PC type
hardware. If you're interested, have a look at the following website.

[12.1 Trillion Digits of Pi - And we're out of disk space... By Alexander J.
Yee & Shigeru Kondo](http://www.numberworld.org/misc_runs/pi-12t)

**PS1:** If the above code is run with `nChunks` set to 8 instead of 1000, the
execution time jumps from ~4 seconds to about 7 seconds. Why's that? Well, it
turns out that, with the lower number of chunks, the available CPU resources are
not used efficiently. In order to find the root cause, you may want to
investigate yourself. Run the two cases and observe the CPU utilization during a
run and compare these between the two runs. It has something to do with a
property of `BigDecimal`. You may wish to bump the precision used in the
calculations to a higher value (e.g. 10,000) to make things better visible.

**PS2:** For those of you who want to play around with the above code; here's π
with 20,000 digits:

```
3.141592653589793238462643383279502884197169399375105820974944592307816406286208998628034825342117067982148
08651328230664709384460955058223172535940812848111745028410270193852110555964462294895493038196442881097566
59334461284756482337867831652712019091456485669234603486104543266482133936072602491412737245870066063155881
74881520920962829254091715364367892590360011330530548820466521384146951941511609433057270365759591953092186
11738193261179310511854807446237996274956735188575272489122793818301194912983367336244065664308602139494639
52247371907021798609437027705392171762931767523846748184676694051320005681271452635608277857713427577896091
73637178721468440901224953430146549585371050792279689258923542019956112129021960864034418159813629774771309
96051870721134999999837297804995105973173281609631859502445945534690830264252230825334468503526193118817101
00031378387528865875332083814206171776691473035982534904287554687311595628638823537875937519577818577805321
71226806613001927876611195909216420198938095257201065485863278865936153381827968230301952035301852968995773
62259941389124972177528347913151557485724245415069595082953311686172785588907509838175463746493931925506040
09277016711390098488240128583616035637076601047101819429555961989467678374494482553797747268471040475346462
08046684259069491293313677028989152104752162056966024058038150193511253382430035587640247496473263914199272
60426992279678235478163600934172164121992458631503028618297455570674983850549458858692699569092721079750930
29553211653449872027559602364806654991198818347977535663698074265425278625518184175746728909777727938000816
47060016145249192173217214772350141441973568548161361157352552133475741849468438523323907394143334547762416
86251898356948556209921922218427255025425688767179049460165346680498862723279178608578438382796797668145410
09538837863609506800642251252051173929848960841284886269456042419652850222106611863067442786220391949450471
23713786960956364371917287467764657573962413890865832645995813390478027590099465764078951269468398352595709
82582262052248940772671947826848260147699090264013639443745530506820349625245174939965143142980919065925093
72216964615157098583874105978859597729754989301617539284681382686838689427741559918559252459539594310499725
24680845987273644695848653836736222626099124608051243884390451244136549762780797715691435997700129616089441
69486855584840635342207222582848864815845602850601684273945226746767889525213852254995466672782398645659611
63548862305774564980355936345681743241125150760694794510965960940252288797108931456691368672287489405601015
03308617928680920874760917824938589009714909675985261365549781893129784821682998948722658804857564014270477
55513237964145152374623436454285844479526586782105114135473573952311342716610213596953623144295248493718711
01457654035902799344037420073105785390621983874478084784896833214457138687519435064302184531910484810053706
14680674919278191197939952061419663428754440643745123718192179998391015919561814675142691239748940907186494
23196156794520809514655022523160388193014209376213785595663893778708303906979207734672218256259966150142150
30680384477345492026054146659252014974428507325186660021324340881907104863317346496514539057962685610055081
06658796998163574736384052571459102897064140110971206280439039759515677157700420337869936007230558763176359
42187312514712053292819182618612586732157919841484882916447060957527069572209175671167229109816909152801735
06712748583222871835209353965725121083579151369882091444210067510334671103141267111369908658516398315019701
65151168517143765761835155650884909989859982387345528331635507647918535893226185489632132933089857064204675
25907091548141654985946163718027098199430992448895757128289059232332609729971208443357326548938239119325974
63667305836041428138830320382490375898524374417029132765618093773444030707469211201913020330380197621101100
44929321516084244485963766983895228684783123552658213144957685726243344189303968642624341077322697802807318
91544110104468232527162010526522721116603966655730925471105578537634668206531098965269186205647693125705863
56620185581007293606598764861179104533488503461136576867532494416680396265797877185560845529654126654085306
14344431858676975145661406800700237877659134401712749470420562230538994561314071127000407854733269939081454
66464588079727082668306343285878569830523580893306575740679545716377525420211495576158140025012622859413021
64715509792592309907965473761255176567513575178296664547791745011299614890304639947132962107340437518957359
61458901938971311179042978285647503203198691514028708085990480109412147221317947647772622414254854540332157
18530614228813758504306332175182979866223717215916077166925474873898665494945011465406284336639379003976926
56721463853067360965712091807638327166416274888800786925602902284721040317211860820419000422966171196377921
33757511495950156604963186294726547364252308177036751590673502350728354056704038674351362222477158915049530
98444893330963408780769325993978054193414473774418426312986080998886874132604721569516239658645730216315981
93195167353812974167729478672422924654366800980676928238280689964004824354037014163149658979409243237896907
06977942236250822168895738379862300159377647165122893578601588161755782973523344604281512627203734314653197
77741603199066554187639792933441952154134189948544473456738316249934191318148092777710386387734317720754565
45322077709212019051660962804909263601975988281613323166636528619326686336062735676303544776280350450777235
54710585954870279081435624014517180624643626794561275318134078330336254232783944975382437205835311477119926
06381334677687969597030983391307710987040859133746414428227726346594704745878477872019277152807317679077071
57213444730605700733492436931138350493163128404251219256517980694113528013147013047816437885185290928545201
16583934196562134914341595625865865570552690496520985803385072242648293972858478316305777756068887644624824
68579260395352773480304802900587607582510474709164396136267604492562742042083208566119062545433721315359584
50687724602901618766795240616342522577195429162991930645537799140373404328752628889639958794757291746426357
45525407909145135711136941091193932519107602082520261879853188770584297259167781314969900901921169717372784
76847268608490033770242429165130050051683233643503895170298939223345172201381280696501178440874519601212285
99371623130171144484640903890644954440061986907548516026327505298349187407866808818338510228334508504860825
03930213321971551843063545500766828294930413776552793975175461395398468339363830474611996653858153842056853
38621867252334028308711232827892125077126294632295639898989358211674562701021835646220134967151881909730381
19800497340723961036854066431939509790190699639552453005450580685501956730229219139339185680344903982059551
00226353536192041994745538593810234395544959778377902374216172711172364343543947822181852862408514006660443
32588856986705431547069657474585503323233421073015459405165537906866273337995851156257843229882737231989875
71415957811196358330059408730681216028764962867446047746491599505497374256269010490377819868359381465741268
04925648798556145372347867330390468838343634655379498641927056387293174872332083760112302991136793862708943
87993620162951541337142489283072201269014754668476535761647737946752004907571555278196536213239264061601363
58155907422020203187277605277219005561484255518792530343513984425322341576233610642506390497500865627109535
91946589751413103482276930624743536325691607815478181152843667957061108615331504452127473924544945423682886
06134084148637767009612071512491404302725386076482363414334623518975766452164137679690314950191085759844239
19862916421939949072362346468441173940326591840443780513338945257423995082965912285085558215725031071257012
66830240292952522011872676756220415420516184163484756516999811614101002996078386909291603028840026910414079
28862150784245167090870006992821206604183718065355672525325675328612910424877618258297651579598470356222629
34860034158722980534989650226291748788202734209222245339856264766914905562842503912757710284027998066365825
48892648802545661017296702664076559042909945681506526530537182941270336931378517860904070866711496558343434
76933857817113864558736781230145876871266034891390956200993936103102916161528813843790990423174733639480457
59314931405297634757481193567091101377517210080315590248530906692037671922033229094334676851422144773793937
51703443661991040337511173547191855046449026365512816228824462575916333039107225383742182140883508657391771
50968288747826569959957449066175834413752239709683408005355984917541738188399944697486762655165827658483588
45314277568790029095170283529716344562129640435231176006651012412006597558512761785838292041974844236080071
93045761893234922927965019875187212726750798125547095890455635792122103334669749923563025494780249011419521
23828153091140790738602515227429958180724716259166854513331239480494707911915326734302824418604142636395480
00448002670496248201792896476697583183271314251702969234889627668440323260927524960357996469256504936818360
90032380929345958897069536534940603402166544375589004563288225054525564056448246515187547119621844396582533
75438856909411303150952617937800297412076651479394259029896959469955657612186561967337862362561252163208628
69222103274889218654364802296780705765615144632046927906821207388377814233562823608963208068222468012248261
17718589638140918390367367222088832151375560037279839400415297002878307667094447456013455641725437090697939
61225714298946715435784687886144458123145935719849225284716050492212424701412147805734551050080190869960330
27634787081081754501193071412233908663938339529425786905076431006383519834389341596131854347546495569781038
29309716465143840700707360411237359984345225161050702705623526601276484830840761183013052793205427462865403
60367453286510570658748822569815793678976697422057505968344086973502014102067235850200724522563265134105592
40190274216248439140359989535394590944070469120914093870012645600162374288021092764579310657922955249887275
84610126483699989225695968815920560010165525637567856672279661988578279484885583439751874454551296563443480
39664205579829368043522027709842942325330225763418070394769941597915945300697521482933665556615678736400536
66564165473217043903521329543529169414599041608753201868379370234888689479151071637852902345292440773659495
63051007421087142613497459561513849871375704710178795731042296906667021449863746459528082436944578977233004
87647652413390759204340196340391147320233807150952220106825634274716460243354400515212669324934196739770415
95683753555166730273900749729736354964533288869844061196496162773449518273695588220757355176651589855190986
66539354948106887320685990754079234240230092590070173196036225475647894064754834664776041146323390565134330
68449539790709030234604614709616968868850140834704054607429586991382966824681857103188790652870366508324319
74404771855678934823089431068287027228097362480939962706074726455399253994428081137369433887294063079261595
99546262462970706259484556903471197299640908941805953439325123623550813494900436427852713831591256898929519
64272875739469142725343669415323610045373048819855170659412173524625895487301676002988659257866285612496655
23533829428785425340483083307016537228563559152534784459818313411290019992059813522051173365856407826484942
76441137639386692480311836445369858917544264739988228462184490087776977631279572267265556259628254276531830
01340709223343657791601280931794017185985999338492354956400570995585611349802524990669842330173503580440811
68552653117099570899427328709258487894436460050410892266917835258707859512983441729535195378855345737426085
90290817651557803905946408735061232261120093731080485485263572282576820341605048466277504500312620080079980
49254853469414697751649327095049346393824322271885159740547021482897111777923761225788734771881968254629812
68685817050740272550263329044976277894423621674119186269439650671515779586756482399391760426017633870454990
17614364120469218237076488783419689686118155815873606293860381017121585527266830082383404656475880405138080
16336388742163714064354955618689641122821407533026551004241048967835285882902436709048871181909094945331442
18287661810310073547705498159680772009474696134360928614849417850171807793068108546900094458995279424398139
21350558642219648349151263901280383200109773868066287792397180146134324457264009737425700735921003154150893
67930081699805365202760072774967458400283624053460372634165542590276018348403068113818551059797056640075094
26087885735796037324514146786703688098806097164258497595138069309449401515422221943291302173912538355915031
00333032511174915696917450271494331515588540392216409722910112903552181576282328318234254832611191280092825
25619020526301639114772473314857391077758744253876117465786711694147764214411112635835538713610110232679877
56410246824032264834641766369806637857681349204530224081972785647198396308781543221166912246415911776732253
26433568614618654522268126887268445968442416107854016768142080885028005414361314623082102594173756238994207
57136275167457318918945628352570441335437585753426986994725470316566139919996826282472706413362221789239031
76085428943733935618891651250424404008952719837873864805847268954624388234375178852014395600571048119498842
39060613695734231559079670346149143447886360410318235073650277859089757827273130504889398900992391350337325
08559826558670892426124294736701939077271307068691709264625484232407485503660801360466895118400936686095463
25002145852930950000907151058236267293264537382104938724996699339424685516483261134146110680267446637334375
34076429402668297386522093570162638464852851490362932019919968828517183953669134522244470804592396602817156
55156566611135982311225062890585491450971575539002439315351909021071194573002438801766150352708626025378817
97519478061013715004489917210022201335013106016391541589578037117792775225978742891917915522417189585361680
59474123419339842021874564925644346239253195313510331147639491199507285843065836193536932969928983791494193
94060857248639688369032655643642166442576079147108699843157337496488352927693282207629472823815374099615455
98798259891093717126218283025848112389011968221429457667580718653806506487026133892822994972574530332838963
81843944770779402284359883410035838542389735424395647555684095224844554139239410001620769363684677641301781
96593799715574685419463348937484391297423914336593604100352343777065888677811394986164787471407932638587386
24732889645643598774667638479466504074111825658378878454858148962961273998413442726086061872455452360643153
71011274680977870446409475828034876975894832824123929296058294861919667091895808983320121031843034012849511
62035342801441276172858302435598300320420245120728725355811958401491809692533950757784000674655260314461670
50827682772223534191102634163157147406123850425845988419907611287258059113935689601431668283176323567325417
07342081733223046298799280490851409479036887868789493054695570307261900950207643349335910602454508645362893
54568629585313153371838682656178622736371697577418302398600659148161640494496501173213138957470620884748023
65371031150898427992754426853277974311395143574172219759799359685252285745263796289612691572357986620573408
37576687388426640599099350500081337543245463596750484423528487470144354541957625847356421619813407346854111
76688311865448937769795665172796623267148103386439137518659467300244345005449953997423723287124948347060440
63471606325830649829795510109541836235030309453097335834462839476304775645015008507578949548931393944899216
12552559770143685894358587752637962559708167764380012543650237141278346792610199558522471722017772370041780
84194239487254068015560359983905489857235467456423905858502167190313952629445543913166313453089390620467843
87785054239390524731362012947691874975191011472315289326772533918146607300089027768963114810902209724520759
16729700785058071718638105496797310016787085069420709223290807038326345345203802786099055690013413718236837
09919495164896007550493412678764367463849020639640197666855923356546391383631857456981471962108410809618846
05456039038455343729141446513474940784884423772175154334260306698831768331001133108690421939031080143784334
15137092435301367763108491351615642269847507430329716746964066653152703532546711266752246055119958183196376
37076179919192035795820075956053023462677579439363074630569010801149427141009391369138107258137813578940055
99500183542511841721360557275221035268037357265279224173736057511278872181908449006178013889710770822931002
79766593583875890939568814856026322439372656247277603789081445883785501970284377936240782505270487581647032
45812908783952324532378960298416692254896497156069811921865849267704039564812781021799132174163058105545988
01300484562997651121241536374515005635070127815926714241342103301566165356024733807843028655257222753049998
83701534879300806260180962381516136690334111138653851091936739383522934588832255088706450753947395204396807
90670868064450969865488016828743437861264538158342807530618454859037982179945996811544197425363443996029025
10015888272164745006820704193761584547123183460072629339550548239557137256840232268213012476794522644820910
23564775272308208106351889915269288910845557112660396503439789627825001611015323516051965590421184494990778
99920073294769058685778787209829013529566139788848605097860859570177312981553149516814671769597609942100361
83559138777817698458758104466283998806006162298486169353373865787735983361613384133853684211978938900185295
69196780455448285848370117096721253533875862158231013310387766827211572694951817958975469399264219791552338
57662316762754757035469941489290413018638611943919628388705436777432242768091323654494853667680000010652624
85473055861598999140170769838548318875014293890899506854530765116803337322265175662207526951791442252808165
17166776672793035485154204023817460892328391703275425750867655117859395002793389592057668278967764453184040
41855401043513483895312013263783692835808271937831265496174599705674507183320650345566440344904536275600112
50184335607361222765949278393706478426456763388188075656121689605041611390390639601620221536849410926053876
88714837989559999112099164646441191856827700457424343402167227644558933012778158686952506949936461017568506
01671453543158148010545886056455013320375864548584032402987170934809105562116715468484778039447569798042631
80991756422809873998766973237695737015808068229045992123661689025962730430679316531149401764737693873514093
36183321614280214976339918983548487562529875242387307755955595546519639440182184099841248982623673771467226
06163364329640633572810707887581640438148501884114318859882769449011932129682715888413386943468285900666408
06314077757725705630729400492940302420498416565479736705485580445865720227637840466823379852827105784319753
54179501134727362577408021347682604502285157979579764746702284099956160156910890384582450267926594205550395
87922981852648007068376504183656209455543461351341525700659748819163413595567196496540321872716026485930490
39787489589066127250794828276938953521753621850796297785146188432719223223810158744450528665238022532843891
37527384589238442253547265309817157844783421582232702069028723233005386216347988509469547200479523112015043
29322662827276321779088400878614802214753765781058197022263097174950721272484794781695729614236585957820908
30733233560348465318730293026659645013718375428897557971449924654038681799213893469244741985097334626793321
07268687076806263991936196504409954216762784091466985692571507431574079380532392523947755744159184582156251
81921552337096074833292349210345146264374498055961033079941453477845746999921285999993996122816152193148887
69388022281083001986016549416542616968586788372609587745676182507275992950893180521872924610867639958916145
85505839727420980909781729323930106766386824040111304024700735085782872462713494636853181546969046696869392
54725194139929146524238577625500474852954768147954670070503479995888676950161249722820403039954632788306959
76249361510102436555352230690612949388599015734661023712235478911292547696176005047974928060721268039226911
027772261025441492215765045081206771735712027180242968106203776578837166909109418074487814049075517
```
