import javafx.beans.property.SimpleDoubleProperty
import tornadofx.getValue
import tornadofx.setValue

object Parameters {

    val animatedTempProperty = SimpleDoubleProperty(0.0)
    var animatedTemp by animatedTempProperty
}