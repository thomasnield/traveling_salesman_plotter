import javafx.geometry.Orientation
import javafx.scene.input.MouseEvent
import javafx.scene.paint.Color
import javafx.scene.shape.Circle
import javafx.scene.shape.Line
import tornadofx.*


class TSPApp: App(TSPView::class)




class TSPView: View() {

    private val dots = mutableListOf<Circle>()
    private val lines = mutableListOf<Line>()

    override val root = borderpane {


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
                label(Parameters.animatedTempProperty)
                useMaxWidth = true
            }
        }

/*
        left = drawer {
            item("SIMULATED ANNEALING") {
                vbox {
                    stackpane {
                        progressbar(Parameters.animatedTempProperty) {
                            style {
                                accentColor = Color.RED
                            }
                        }
                        label(Parameters.animatedTempProperty)
                        useMaxWidth = true
                    }
                    button("RUN") {
                        setOnAction {
                            SearchStrategy.prepare()
                            SearchStrategy.SIMULATED_ANNEALING.execute()
                            sequentialTransition.play()
                        }
                        useMaxWidth = true
                    }
                }
            }
        }*/

        center = pane {

            // Sychronize changes to cities
            City.all.onChange {
                children.removeAll(dots)
                dots.clear()

                it.list.forEach {
                    Circle(it.x, it.y, 5.0).apply {
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