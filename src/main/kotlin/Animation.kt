import javafx.animation.SequentialTransition
import javafx.animation.Timeline
import tornadofx.*

val sequentialTransition = SequentialTransition()
operator fun SequentialTransition.plusAssign(timeline: Timeline) { children += timeline }

var defaultSpeed = 200.millis
var speed = defaultSpeed
var defaultAnimationOn = true