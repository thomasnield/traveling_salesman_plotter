import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import tornadofx.*

class Edge(val id: Int, startingCity: City) {

    val startCityProperty = SimpleObjectProperty(startingCity)
    var startCity by startCityProperty

    val endCityProperty = SimpleObjectProperty(startingCity)
    var endCity by endCityProperty

    val edgeStartX = SimpleDoubleProperty(startCityProperty.get().x)
    val edgeStartY = SimpleDoubleProperty(startCityProperty.get().y)
    val edgeEndX = SimpleDoubleProperty(startCityProperty.get().x)
    val edgeEndY = SimpleDoubleProperty(startCityProperty.get().y)

    val nextEdge get() = (all.firstOrNull { it != this && it.startCity == endCity }) ?:
        (all.firstOrNull { it != this && it.endCity == endCity }?.also { it.flip() })

    val distance get() = startCity.distanceTo(endCity)

    private fun flip() {
        val city1 = startCity
        val city2 = endCity
        startCity = city2
        endCity = city1
    }


    class Swap(val city1: City,
               val city2: City,
               val edge1: Edge,
               val edge2: Edge
    ) {

        fun execute() {
            edge1.let { sequenceOf(it.startCityProperty, it.endCityProperty) }.first { it.get() == city1 }.set(city2)
            edge2.let { sequenceOf(it.startCityProperty, it.endCityProperty) }.first { it.get() == city2 }.set(city1)
        }
        fun reverse() {
            edge1.let { sequenceOf(it.startCityProperty, it.endCityProperty) }.first { it.get() == city2 }.set(city1)
            edge2.let { sequenceOf(it.startCityProperty, it.endCityProperty) }.first { it.get() == city1 }.set(city2)
        }


        fun animate() {
            sequentialTransition += timeline(play = false) {
                keyframe(speed) {
                    sequenceOf(edge1,edge2).forEach {
                        keyvalue(it.edgeStartX, it.startCity?.x ?: 0.0)
                        keyvalue(it.edgeStartY, it.startCity?.y ?: 0.0)
                        keyvalue(it.edgeEndX, it.endCity?.x ?: 0.0)
                        keyvalue(it.edgeEndY, it.endCity?.y ?: 0.0)
                    }
                }
            }
        }

        override fun toString() = "$city1-$city2 ($edge1)-($edge2)"
    }

    fun attemptSafeSwap(otherEdge: Edge): Swap? {

        val e1 = this
        val e2 = otherEdge

        val startCity1 = startCity
        val endCity1 = endCity
        val startCity2 = otherEdge.startCity
        val endCity2 = otherEdge.endCity

        return sequenceOf(
                Swap(startCity1, startCity2, e1, e2),
                Swap(endCity1, endCity2, e1, e2),

                Swap(startCity1, endCity2, e1, e2),
                Swap(endCity1, startCity2, e1, e2)

        ).filter {
            it.edge1.startCity !in it.edge2.let { setOf(it.startCity, it.endCity) } &&
                    it.edge1.endCity !in it.edge2.let { setOf(it.startCity, it.endCity) }
        }
                .firstOrNull { swap ->
                    swap.execute()
                    val result = Edge.tourMaintained
                    if (!result) {
                        swap.reverse()
                    }
                    result
                }
    }


    init {
        startCityProperty.onChange {
            if (defaultAnimationOn) {
                sequentialTransition += timeline(play = false) {
                    keyframe(speed) {
                        keyvalue(edgeStartX, it?.x ?: 0.0)
                        keyvalue(edgeStartY, it?.y ?: 0.0)
                    }
                }
            }
        }
        endCityProperty.onChange {
            if (defaultAnimationOn)
                sequentialTransition += timeline(play = false) {
                    keyframe(speed) {
                        keyvalue(edgeEndX, it?.x ?: 0.0)
                        keyvalue(edgeEndY, it?.y ?: 0.0)
                    }
                }
        }
    }

    companion object {

        val all = FXCollections.observableArrayList<Edge>()

        fun create(startingCity: City): Edge {

            val nextId = (all.asSequence().map { it.id }.max()?:-1) + 1

            return Edge(nextId, startingCity).also { all += it }
        }

        fun clearAndRebuild() {
            var id = -1
            City.all.map { Edge(id++, it) }
                    .also { all.setAll(it) }
        }

        val traverseTour: Sequence<Edge> get() {
            val captured = mutableSetOf<Edge>()

            return generateSequence(all.first()) {
                it.nextEdge?.takeIf { it !in captured }
            }.onEach { captured += it }
        }
        val tourDistance get() = all.map { it.distance }.sum()
        val tourMaintained get() = traverseTour.count() == all.count()
    }
}