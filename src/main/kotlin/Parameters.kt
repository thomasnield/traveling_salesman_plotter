import javafx.beans.property.SimpleDoubleProperty
import tornadofx.*

object Parameters {

    val animatedTempProperty = SimpleDoubleProperty(0.0)
    var animatedTemp by animatedTempProperty
}