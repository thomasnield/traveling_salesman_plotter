import javafx.application.Application
import javafx.beans.property.SimpleDoubleProperty
import javafx.geometry.Orientation
import javafx.scene.input.MouseEvent
import javafx.scene.paint.Color
import javafx.scene.shape.Circle
import javafx.scene.shape.Line
import javafx.stage.FileChooser
import tornadofx.*
import java.math.BigDecimal
import java.math.RoundingMode


fun main(args: Array<String>) = Application.launch(TSPApp::class.java, *args)

class TSPApp: App(TSPView::class)

val plotAreaWidthProperty = SimpleDoubleProperty()
val plotAreaWidth get() = plotAreaWidthProperty.get()

val plotAreaHeightProperty = SimpleDoubleProperty()
val plotAreaHeight get() = plotAreaHeightProperty.get()

class TSPView: View() {

    private val dots = mutableListOf<Circle>()
    private val lines = mutableListOf<Line>()

    override val root = borderpane {

        top = menubar {
            menu("File") {
                item("Import CSV") {
                    setOnAction {
                        FileChooser().showOpenDialog(null)
                                .readLines().asSequence()
                                .drop(1)
                                .map { it.split(" ").map { it.toDouble() } }
                                .toList().let {
                                    val maxWidth = it.asSequence().map { it[0] }.max()!!
                                    val maxHeight = it.asSequence().map { it[1] }.max()!!

                                    val xScale = (plotAreaWidth - maxWidth) / maxWidth
                                    val yScale = (plotAreaHeight - maxHeight) / maxHeight

                                    it.mapIndexed { i,c -> City(i, c[0], c[1], xScale, yScale) }
                                }
                                .also {
                                    City.all.setAll(it)
                                }

                    }
                }
                item("Export CSV") {
                    setOnAction {
                        FileChooser().showOpenDialog(null)
                    }
                }
            }
        }

        left = toolbar {
            orientation = Orientation.VERTICAL

            SearchStrategy.values().forEach { ss ->

                button(ss.toString().replace("_", " ")) {
                    setOnAction {
                        SearchStrategy.prepare()
                        ss.execute()
                        sequentialTransition.play()
                    }
                    useMaxWidth = true
                }
            }

            stackpane {
                progressbar(Parameters.animatedTempProperty) {
                    style {
                        accentColor = Color.RED
                    }
                    useMaxWidth = true
                }
                label {
                    Parameters.animatedTempProperty.onChange {
                        text = BigDecimal(it).setScale(2, RoundingMode.HALF_UP).toString()
                    }
                }
                useMaxWidth = true
            }
        }

        center = pane {
            plotAreaWidthProperty.bind(widthProperty())
            plotAreaHeightProperty.bind(heightProperty())

            // Sychronize changes to cities
            City.all.onChange {
                children.removeAll(dots)
                dots.clear()

                it.list.forEach {
                    Circle(it.displayX, it.displayY, 5.0).apply {
                        fill = Color.RED
                        children += this
                        dots += this
                    }
                }
            }
            // Synchronize Edges

            Edge.all.onChange {

                children.removeAll(lines)
                lines.clear()

                it.list.forEach { edge ->
                    Line().apply {
                        startXProperty().bind(edge.edgeStartX)
                        startYProperty().bind(edge.edgeStartY)
                        endXProperty().bind(edge.edgeEndX)
                        endYProperty().bind(edge.edgeEndY)
                        strokeWidth = 3.0
                        stroke = Color.RED
                    }.also {
                        children += it
                        lines += it
                    }
                }
            }

            addEventHandler(MouseEvent.MOUSE_CLICKED) {
                City.create(it.x, it.y)
            }
        }
    }
}