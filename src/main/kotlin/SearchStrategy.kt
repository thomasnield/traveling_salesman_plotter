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

             sequenceOf(
                    generateSequence(80.0) { (it - .05).takeIf { it >= 0 } }/*,
                    generateSequence(0.0) { (it + .005).takeIf { it <= 80 } },
                    generateSequence(80.0) { (it - .005).takeIf { it >= 30 } }*/
                    ).flatMap { it }
                     .plus(0.0)
                     .forEach { temp ->

                        Edge.all.sampleDistinct(2)
                                .toList()
                                .also { (e1,e2) ->

                                    val oldDistance = Tour.tourDistance

                                    // try to swap vertices on the two random edges
                                    val swap = e1.attemptTwoSwap(e2)

                                    // track changes in distance
                                    val newDistance = Tour.tourDistance

                                    //if a swap was possible
                                    if (swap != null) {

                                        // if swap is superior to curent distance, keep it
                                        if (newDistance < oldDistance) {

                                            swap.animate()
                                            // if swap is superior to the last best found solution, save it as the new best solution
                                            if (newDistance < bestDistance) {
                                                bestDistance = newDistance

                                                bestSolution = Tour.toConfiguration()
                                            }
                                        }
                                        // shall I take an inferior move? Let's flip a coin
                                        else {
                                            // Desmos graph for intuition: https://www.desmos.com/calculator/rpfpfiq7ce
                                            if (weightedCoinFlip(
                                                            exp((-(newDistance - oldDistance)) / temp)
                                                    )
                                            ) {
                                                swap.animate()
                                            } else {
                                                swap.reverse()
                                            }
                                        }
                                    }
                                }

                        sequentialTransition += timeline(play=false) {
                            keyframe(1.millis) {
                                keyvalue(Parameters.animatedTempProperty, temp / 80 )
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
    }/*,


    INTEGER {
        override fun execute() {

            val solver = ExpressionsBasedModel()

            val cities = City.all

            val cityDummies = cities.map { it to solver.variable(isInteger = true, lower = 0) }.toMap()

            data class Segment(val city1: City, val city2: City) {
                val selected = solver.variable(isBinary = true)
                val distance get() = city1.distanceTo(city2)

                val u_i = cityDummies[city1]!!
                val u_j = cityDummies[city2]!!

                // TODO this is not working :/
                // https://en.wikipedia.org/wiki/Travelling_salesman_problem#Integer_linear_programming_formulation
                init {
                    solver.expression {
                        set(u_i, 1)
                        set(u_j, -1)
                        set(selected, cities.size)
                        upper(cities.size-1)
                    }
                }
                operator fun contains(city: City) = city == city1 || city == city2
            }

            // create segments
            val segments = cities.flatMap { city1 ->
                cities.filter { it != city1 }
                        .map { city1 to it }
                        //.map { city2 -> if (city1.id > city2.id) city2 to city1 else city1 to city2 }
            }.distinct()
            .map { Segment(it.first, it.second) }
            .toList()

            solver.apply {

                // constrain each city to have two connections
                cities.forEach { city ->
                    expression(lower=2, upper=2) {
                        segments.filter { city in it }.forEach { set(it.selected, 1) }
                    }
                }

                // minimize distance objective
                expression(weight = 1) {
                    segments.forEach {
                        set(it.selected, it.distance)
                    }
                }

            }

            // execute and plot
            val result = solver.minimise().also(::println)

            segments.filter { it.selected.value.toInt() == 1 }
                    .zip(Edge.all)
                    .forEach { (selectedSegment, edge) ->
                        edge.startCity = selectedSegment.city1
                        edge.endCity = selectedSegment.city2
                        edge.animateChange()
                    }

        }
    }*/;

    abstract fun execute()

    companion object {
        fun prepare() {
            sequentialTransition.children.clear()
            Edge.clearAndRebuild()
        }
    }
}