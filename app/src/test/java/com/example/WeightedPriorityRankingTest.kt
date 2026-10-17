package com.example

import com.example.data.ServiceBookingRepository
import com.example.model.BookingStatus
import com.example.model.DasmariñasBarangay
import com.example.model.RankingCriterion
import com.example.model.Timeslot
import com.example.ranking.WeightedPriorityRankingEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WeightedPriorityRankingTest {

    @Test
    fun testWeightedPriorityRankingCalculation() {
        val providers = ServiceBookingRepository.initialProviders
        val weights = RankingCriterion.values().associateWith { it.defaultWeight }

        val ranked = WeightedPriorityRankingEngine.rankProviders(
            providers = providers,
            categoryId = "plumbing",
            requestedSkills = listOf("Pipe Leak Repair", "Drain Unclogging"),
            customerBarangay = DasmariñasBarangay.ZONE_IV, // AISAT vicinity
            requestedTimeslot = Timeslot.MORNING,
            customWeights = weights
        )

        assertTrue("Should return ranked plumbing providers", ranked.isNotEmpty())

        // Check descending sort order
        for (i in 0 until ranked.size - 1) {
            assertTrue(
                "Rank ${ranked[i].rank} score (${ranked[i].overallPriorityScore}) should be >= Rank ${ranked[i + 1].rank} score (${ranked[i + 1].overallPriorityScore})",
                ranked[i].overallPriorityScore >= ranked[i + 1].overallPriorityScore
            )
        }

        // Verify each provider has all 9 criteria evaluated
        val topMatch = ranked.first()
        assertEquals(9, topMatch.criterionScores.size)
        assertTrue(topMatch.overallPriorityScore in 0f..100f)

        topMatch.criterionScores.forEach { criterionScore ->
            assertTrue(
                "Criterion ${criterionScore.criterion.displayName} normalized score should be 0-100",
                criterionScore.normalizedScore in 0f..100f
            )
            assertTrue(criterionScore.weightedScore >= 0f)
        }
    }

    @Test
    fun testAutomatedBookingConfirmation() {
        val repo = ServiceBookingRepository()
        // prov_1 (Eddie Santos) has instant booking enabled for MORNING
        val booking = repo.createBooking(
            customerName = "Carl Nathaniel Salvatus",
            customerContact = "0917-300-0149",
            customerBarangay = DasmariñasBarangay.ZONE_IV,
            specificAddress = "AISAT Building, Emilio Aguinaldo Highway",
            categoryId = "plumbing",
            requestedSkills = listOf("Pipe Leak Repair"),
            scheduledDate = "Tomorrow",
            timeslot = Timeslot.MORNING,
            notes = "Faculty leak test",
            providerId = "prov_1",
            overallPriorityScore = 96.4f
        )

        assertNotNull(booking)
        assertTrue("Instant booking should trigger automated confirmation", booking.automatedConfirmation)
        assertEquals(BookingStatus.ACCEPTED, booking.status)
        assertTrue("Booking code should have DSMA prefix", booking.bookingCode.startsWith("DSMA-"))
    }
}
