import javafx.collections.FXCollections
import kotlin.math.sqrt

class City(val id: Int, val x: Double, val y: Double, val scaleX: Double = 1.0, val scaleY: Double = 1.0) {

    val displayX get() = x * scaleX
    val displayY get() = y * scaleY

    fun distanceTo(otherCity: City) = sqrt(Math.pow(x - otherCity.x, 2.0) + Math.pow(y - otherCity.y, 2.0))

    companion object {

        val all = FXCollections.observableArrayList<City>()

        fun clear() = all.clear()

        fun create(x: Double, y: Double): City {

            val nextId = (all.asSequence().map { it.id }.max()?:-1) + 1

            return City(nextId, x, y).also { all += it }
        }
    }
}