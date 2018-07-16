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
        }
    },

    TWO_OPT {
        override fun execute() {

            SearchStrategy.RANDOM.execute()

            (1..20000).forEach { iteration ->
                Edge.all.sampleDistinct(2).toList()
                        .let { it.first() to it.last() }
                        .also { (e1,e2) ->

                            val oldDistance = Tour.tourDistance
                            e1.attemptTwoSwap(e2)?.also {
                                when {
                                    oldDistance <= Tour.tourDistance -> it.reverse()
                                }
                            }
                        }
            }

            if (!Tour.isMaintained) throw Exception("Tour broken in TWO_OPT SearchStrategy \r\n${Edge.all.joinToString("\r\n")}")
        }
    },
    SIMULATED_ANNEALING {
        override fun execute() {
            SearchStrategy.RANDOM.execute()

            var bestDistance = Tour.tourDistance
            var bestSolution = Tour.toConfiguration()

            sequenceOf(
                    generateSequence(80.0) { (it - .0001).takeIf { it >= 0 } },
                    generateSequence(80.0) { (it - .0001).takeIf { it >= 0 } },
                    generateSequence(80.0) { (it - .0001).takeIf { it >= 0 } },

                    generateSequence(120.0) { (it - .0001).takeIf { it >= 0 } },
                    generateSequence(80.0) { (it - .0001).takeIf { it >= 0 } },
                    generateSequence(80.0) { (it - .0001).takeIf { it >= 0 } },

                    generateSequence(140.0) { (it - .0001).takeIf { it >= 0 } },
                    generateSequence(80.0) { (it - .0001).takeIf { it >= 0 } },
                    generateSequence(80.0) { (it - .0001).takeIf { it >= 0 } },

                    generateSequence(120.0) { (it - .0001).takeIf { it >= 0 } },
                    generateSequence(80.0) { (it - .0001).takeIf { it >= 0 } },
                    generateSequence(80.0) { (it - .0001).takeIf { it >= 0 } },
                    generateSequence(80.0) { (it - .0001).takeIf { it >= 0 } }

        ).flatMap { it }
            .forEach { temp ->
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

                                        if (bestDistance > neighborDistance) {
                                            bestDistance = neighborDistance
                                            bestSolution = Tour.toConfiguration()
                                        }
                                    }
                                    oldDistance < neighborDistance -> {

                                        // Desmos graph for intuition: https://www.desmos.com/calculator/mn6av6ixx2
                                        if (!weightedCoinFlip(
                                                        exp((-(neighborDistance - bestDistance)) / temp)
                                                )
                                        ) {
                                            swap.reverse()
                                        }
                                    }
                                }
                            }
                        }

            }

            (1..10).forEach {
                Tour.conflicts.forEach { (x, y) ->
                    x.attemptTwoSwap(y)
                }
            }

            // apply best found model
            if (Tour.tourDistance > bestDistance) {
                Tour.applyConfiguration(bestSolution)
            }
            //println("$bestDistance<==>${Tour.tourDistance}")
        }
    };

    abstract fun execute()

    companion object {
        fun prepare() {
            Edge.clearAndRebuild()
        }
    }
}