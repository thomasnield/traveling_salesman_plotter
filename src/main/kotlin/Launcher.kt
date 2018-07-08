import javafx.application.Application
import java.io.File

fun main(args: Array<String>) {

    if (args.isNotEmpty()) {

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

        println(Tour.tourDistance)

    } else {
        Application.launch(TSPApp::class.java, *args)
    }
}
