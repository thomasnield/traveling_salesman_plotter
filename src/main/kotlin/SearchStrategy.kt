import tornadofx.*
import kotlin.math.exp

enum class SearchStrategy {
    RANDOM {
        override fun execute() {
            val capturedCities = mutableSetOf<Int>()

            val startingEdge = Edge.all.sample()
            var edge = startingEdge

            while (capturedCities.size < City.all.size) {
                capturedCities += edge.startCity.id

                val nextRandom = Edge.all.asSequence()
                        .filter { it.startCity.id !in capturedCities }
                        .sampleOrNull() ?: startingEdge

                edge.endCity = nextRandom.startCity
                edge = nextRandom
            }

            if (!Tour.isMaintained) throw Exception("Tour broken in RANDOM SearchStrategy \r\n${Edge.all.joinToString("\r\n")}")
            defaultAnimationOn = true
        }
    },

    GREEDY {
        override fun execute() {
            val capturedCities = mutableSetOf<Int>()

            var edge = Edge.all.first()

            while (capturedCities.size < City.all.size) {
                capturedCities += edge.startCity.id

                val closest = Edge.all.asSequence().filter { it.startCity.id !in capturedCities }
                        .minBy { edge.startCity.distanceTo(it.startCity) }
                        ?: Edge.all.first()

                edge.endCity = closest.startCity
                edge = closest
            }
            if (!Tour.isMaintained) throw Exception("Tour broken in GREEDY SearchStrategy \r\n${Edge.all.joinToString("\r\n")}")
            defaultAnimationOn = true
        }
    },

    REMOVE_OVERLAPS {
        override fun execute() {

            SearchStrategy.RANDOM.execute()
            defaultAnimationOn = false

            (1..10).forEach {
                Tour.conflicts.forEach { (x, y) ->
                    x.attemptTwoSwap(y)?.animate()
                }
            }

            defaultAnimationOn= true
        }
    },

    TWO_OPT {
        override fun execute() {

            SearchStrategy.RANDOM.execute()
            defaultAnimationOn = false

            (1..10000).forEach { iteration ->
                Edge.all.sampleDistinct(2).toList()
                        .let { it.first() to it.last() }
                        .also { (e1,e2) ->

                            val oldDistance = Tour.tourDistance
                            e1.attemptTwoSwap(e2)?.also {
                                when {
                                    oldDistance <= Tour.tourDistance -> it.reverse()
                                    oldDistance > Tour.tourDistance -> it.animate()
                                }
                            }
                        }
            }

            if (!Tour.isMaintained) throw Exception("Tour broken in TWO_OPT SearchStrategy \r\n${Edge.all.joinToString("\r\n")}")
            defaultAnimationOn = true
        }
    },
    SIMULATED_ANNEALING {
        override fun execute() {
            SearchStrategy.RANDOM.execute()
            defaultAnimationOn = false


            var bestDistance = Tour.tourDistance
            var bestSolution = Tour.toConfiguration()

            val tempSchedule = sequenceOf(


                    // modest wave 1
                    800 downTo 600,
                    600..800,
                    800 downTo 400,
                    400..600,
                    600 downTo 0,


                    // modest wave 2
                    800 downTo 600,
                    600..800,
                    800 downTo 400,
                    400..600,
                    600 downTo 0,


                    // high heat wave 1
                    2000 downTo 600 step 20,
                    600..800,
                    800 downTo 400,
                    400..600,
                    600 downTo 0,

                    800 downTo 600,
                    600..800,
                    800 downTo 400,
                    400..600,
                    600 downTo 0,

                    // high heat wave 1
                    3000 downTo 600 step 20,
                    600..800,
                    800 downTo 400,
                    400..600,
                    600 downTo 0,

                    2000 downTo 600 step 20,
                    600..800,
                    800 downTo 400,
                    400..600,
                    600 downTo 0,

                    800 downTo 600,
                    600..800,
                    800 downTo 400,
                    400..600,
                    600 downTo 0,

                    1200 downTo 600,
                    600..800,
                    800 downTo 400,
                    400..600,
                    600 downTo 0

            ).flatMap { it.asSequence() }
                    .toList()
                    .let { it.asSequence().plus(it.asSequence()) }
                    .toList().toTypedArray().toIntArray().let {
                        TempSchedule(1000, it)
                    }

            while(tempSchedule.next()) {

                Edge.all.sampleDistinct(2)
                        .toList()
                        .let { it.first() to it.last() }
                        .also { (e1,e2) ->

                            val oldDistance = Tour.tourDistance

                            e1.attemptTwoSwap(e2)?.also { swap ->

                                val neighborDistance = Tour.tourDistance

                                when {
                                    oldDistance == neighborDistance -> swap.reverse()
                                    neighborDistance == bestDistance -> swap.reverse()
                                    bestDistance > neighborDistance -> {
                                        println("${tempSchedule.ratio}: $bestDistance->$neighborDistance")
                                        bestDistance = neighborDistance
                                        bestSolution = Tour.toConfiguration()
                                        swap.animate()
                                    }
                                    bestDistance < neighborDistance -> {

                                        // Desmos graph for intuition: https://www.desmos.com/calculator/mn6av6ixx2
                                        if (weightedCoinFlip(
                                                        exp((-(neighborDistance - bestDistance)) / (tempSchedule.heat.toDouble() * .1))
                                                )
                                        ) {
                                            swap.animate()
                                            println("${tempSchedule.heat} accepting degrading solution: $bestDistance -> $neighborDistance")

                                        } else {
                                            swap.reverse()
                                        }
                                    }
                                }
                            }
                        }
            }

            (1..10).forEach {
                Tour.conflicts.forEach { (x, y) ->
                    x.attemptTwoSwap(y)?.animate()
                }
            }

            // apply best found model
            if (Tour.tourDistance > bestDistance) {
                Tour.applyConfiguration(bestSolution)
                Edge.all.forEach { it.animateChange() }
            }
            println("$bestDistance<==>${Tour.tourDistance}")
            defaultAnimationOn = true

        }
    };

    abstract fun execute()

    companion object {
        fun prepare() {
            sequentialTransition.children.clear()
            Edge.clearAndRebuild()
        }
    }
}