package org.schabi.newpipe.local.srs

import java.time.OffsetDateTime

object SM2Algorithm {

    data class SM2Result(
        val interval: Int,
        val easeFactor: Float,
        val repetitions: Int,
        val nextReview: OffsetDateTime
    )

    /**
     * Calculate spaced repetition values using the SM-2 algorithm.
     * @param quality 0=Again (forgot completely), 2=Hard, 3=Good, 4=Easy
     */
    fun calculate(
        interval: Int,
        easeFactor: Float,
        repetitions: Int,
        quality: Int
    ): SM2Result {
        var newEF = easeFactor
        var newReps = repetitions
        var newInterval: Int

        when (quality) {
            0 -> { // Again - forgot completely
                newReps = 0
                newInterval = 1
                newEF = maxOf(1.3f, newEF - 0.20f)
            }

            2 -> { // Hard
                newReps++
                newInterval = when (newReps) {
                    1 -> 1
                    2 -> 3
                    else -> (interval * 1.2).toInt()
                }
                newEF = maxOf(1.3f, newEF - 0.15f)
            }

            3 -> { // Good
                newReps++
                newInterval = when (newReps) {
                    1 -> 1
                    2 -> 6
                    else -> (interval * newEF).toInt()
                }
            }

            4 -> { // Easy
                newReps++
                newInterval = when (newReps) {
                    1 -> 4
                    2 -> 10
                    else -> (interval * newEF * 1.3f).toInt()
                }
                newEF += 0.15f
            }

            else -> throw IllegalArgumentException("Quality must be 0, 2, 3, or 4")
        }

        if (newInterval < 1) newInterval = 1
        val nextReview = OffsetDateTime.now().plusDays(newInterval.toLong())

        return SM2Result(newInterval, newEF, newReps, nextReview)
    }
}
