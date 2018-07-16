import java.io.File

fun main(args: Array<String>) {

    File(args[0])
            .readLines().asSequence()
            .drop(1)
            .map { it.split(" ").map { it.toDouble() } }
            .mapIndexed { i,c -> City(i,c[0], c[1]) }
            .toList()
            .also {
                City.all.setAll(it)
            }
    SearchStrategy.prepare()
    SearchStrategy.SIMULATED_ANNEALING.execute()

    println("${Tour.tourDistance} 0")

    println(Tour.traverseCities.map { it.id }.joinToString(" "))

}
