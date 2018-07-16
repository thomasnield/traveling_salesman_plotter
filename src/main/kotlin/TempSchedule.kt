import java.util.concurrent.ThreadLocalRandom

/**
 * Simulates a weighted TRUE/FALSE coin flip, with a percentage of probability towards TRUE
 */
fun weightedCoinFlip(trueProbability: Double) =
        ThreadLocalRandom.current().nextDouble(0.0,1.0) <= trueProbability
