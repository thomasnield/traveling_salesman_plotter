import org.ojalgo.okalgo.expression
import org.ojalgo.okalgo.variable
import org.ojalgo.optimisation.ExpressionsBasedModel
import tornadofx.*
import kotlin.math.exp
import kotlin.math.roundToInt

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
                    generateSequence(80.0) { (it - .005).takeIf { it >= 0 } },
                    generateSequence(0.0) { (it + .005).takeIf { it <= 120 } },
                    generateSequence(80.0) { (it - .005).takeIf { it >= 30 } }
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

                                        //println("${tempSchedule.ratio}: $bestDistance->$neighborDistance")

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
                                            //println("${tempSchedule.heat} accepting degrading solution: $bestDistance -> $neighborDistance")

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

            // apply best found model
            if (Tour.tourDistance > bestDistance) {
                Tour.applyConfiguration(bestSolution)
                Edge.all.forEach { it.animateChange() }
            }
            //println("$bestDistance<==>${Tour.tourDistance}")
            defaultAnimationOn = true

        }
    },


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
    };

    abstract fun execute()

    companion object {
        fun prepare() {
            sequentialTransition.children.clear()
            Edge.clearAndRebuild()
        }
    }
}