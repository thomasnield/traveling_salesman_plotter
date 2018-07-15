object Tour {

    val traverseTour: Sequence<Edge> get() {
        val captured = mutableSetOf<Edge>()

        return generateSequence(Edge.all.minBy { it.id }) {
            it.nextEdge?.takeIf { it !in captured }
        }.onEach { captured += it }
    }

    val traverseCities get() = traverseTour.map { it.startCity }

    val tourDistance get() = Edge.all.map { it.distance }.sum()
    val isMaintained get() = traverseTour.count() == Edge.all.count()

    val conflicts get() = Edge.all.asSequence()
            .map { edge1 -> edge1.intersectConflicts.map { edge2 -> edge1 to edge2}.sampleOrNull() }
            .filterNotNull()

    fun toConfiguration() = traverseTour.map { it.startCity to it.endCity }.toList().toTypedArray()
    fun applyConfiguration(configuration: Array<Pair<City,City>>) = Edge.all.zip(configuration).forEach { (e,c) ->
        e.startCity = c.first
        e.endCity = c.second
    }
}