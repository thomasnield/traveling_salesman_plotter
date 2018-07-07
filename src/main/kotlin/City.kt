import javafx.collections.FXCollections
import kotlin.math.sqrt

class City(val id: Int, val x: Double, val y: Double) {

    fun distanceTo(otherCity: City) = sqrt(Math.pow(x - otherCity.x, 2.0) + Math.pow(y - otherCity.y, 2.0))

    companion object {

        val all = FXCollections.observableArrayList<City>()

        fun create(x: Double, y: Double): City {

            val nextId = (all.asSequence().map { it.id }.max()?:-1) + 1

            return City(nextId, x, y).also { all += it }
        }
    }
}