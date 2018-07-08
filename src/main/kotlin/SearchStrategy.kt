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
                    generateSequence(80.0) { (it - .005).takeIf { it >= 0 } }/*,
                    generateSequence(0.0) { (it + .005).takeIf { it <= 80 } },
                    generateSequence(80.0) { (it - .005).takeIf { it >= 30 } }*/
                    ).flatMap { it }
            .toList().toTypedArray().toDoubleArray().let {
                TempSchedule(200, it)
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
                                    oldDistance > neighborDistance -> {

                                        println("${tempSchedule.ratio}: $bestDistance->$neighborDistance")

                                        if (bestDistance > neighborDistance) {
                                            bestDistance = neighborDistance
                                            bestSolution = Tour.toConfiguration()
                                        }
                                        swap.animate()
                                    }
                                    oldDistance < neighborDistance -> {

                                        // Desmos graph for intuition: https://www.desmos.com/calculator/mn6av6ixx2
                                        if (weightedCoinFlip(
                                                        exp((-(neighborDistance - bestDistance)) / tempSchedule.heat)
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

                sequentialTransition += timeline(play=false) {
                    keyframe(1.millis) {
                        keyvalue(Parameters.animatedTempProperty, tempSchedule.ratio)
                    }
                }
            }

            (1..10).forEach {
                Tour.conflicts.forEach { (x, y) ->
                    x.attemptTwoSwap(y)?.animate()
                }
            }

            sequentialTransition += timeline(play=false) {
                keyframe(2.seconds) {
                    keyvalue(Parameters.animatedTempProperty, 0)
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