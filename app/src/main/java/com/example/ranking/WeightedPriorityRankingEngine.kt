package com.example.ranking

import com.example.model.CriterionScore
import com.example.model.DasmariñasBarangay
import com.example.model.RankedProvider
import com.example.model.RankingCriterion
import com.example.model.ServiceProvider
import com.example.model.Timeslot
import java.util.Locale

object WeightedPriorityRankingEngine {

    /**
     * Evaluates a list of service providers against a customer's service requirement
     * and returns a ranked list sorted descending by Overall Priority Score (OPS).
     */
    fun rankProviders(
        providers: List<ServiceProvider>,
        categoryId: String,
        requestedSkills: List<String>,
        customerBarangay: DasmariñasBarangay,
        requestedTimeslot: Timeslot,
        customWeights: Map<RankingCriterion, Float>
    ): List<RankedProvider> {
        // Filter providers that offer the requested category
        val eligibleProviders = providers.filter { it.categoryIds.contains(categoryId) }

        // Normalize weights so they sum to 1.0 (in case admin adjusted them)
        val totalRawWeight = customWeights.values.sum().coerceAtLeast(0.01f)
        val normalizedWeights = customWeights.mapValues { it.value / totalRawWeight }

        val evaluated = eligibleProviders.map { provider ->
            val distanceKm = DasmariñasBarangay.calculateDistanceKm(customerBarangay, provider.baseBarangay)
            val criterionScores = mutableListOf<CriterionScore>()

            // 1. Skill Match
            val skillMatchScore: Float
            val skillExplanation: String
            if (requestedSkills.isEmpty()) {
                skillMatchScore = 100.0f
                skillExplanation = "General category specialist"
            } else {
                val matchedCount = requestedSkills.count { requested ->
                    provider.certifiedSkills.any { cert -> cert.contains(requested, ignoreCase = true) || requested.contains(cert, ignoreCase = true) }
                }
                val ratio = matchedCount.toFloat() / requestedSkills.size.toFloat()
                skillMatchScore = (ratio * 100.0f).coerceIn(0f, 100f)
                skillExplanation = "$matchedCount of ${requestedSkills.size} requested skills verified"
            }
            val smWeight = normalizedWeights[RankingCriterion.SKILL_MATCH] ?: RankingCriterion.SKILL_MATCH.defaultWeight
            criterionScores.add(
                CriterionScore(
                    criterion = RankingCriterion.SKILL_MATCH,
                    weight = smWeight,
                    rawValue = "${String.format(Locale.US, "%.0f", skillMatchScore)}%",
                    normalizedScore = skillMatchScore,
                    weightedScore = skillMatchScore * smWeight,
                    explanation = skillExplanation
                )
            )

            // 2. Customer Rating
            val ratingNormalized = ((provider.rating / 5.0f) * 100.0f).coerceIn(0f, 100f)
            val crWeight = normalizedWeights[RankingCriterion.CUSTOMER_RATING] ?: RankingCriterion.CUSTOMER_RATING.defaultWeight
            criterionScores.add(
                CriterionScore(
                    criterion = RankingCriterion.CUSTOMER_RATING,
                    weight = crWeight,
                    rawValue = "${String.format(Locale.US, "%.1f", provider.rating)} / 5.0 (${provider.totalReviews} reviews)",
                    normalizedScore = ratingNormalized,
                    weightedScore = ratingNormalized * crWeight,
                    explanation = "Computed from historical verified client ratings"
                )
            )

            // 3. Service Experience
            val yearsScore = (provider.yearsExperience * 14.0f).coerceAtMost(100f)
            val jobsScore = (provider.completedJobsCount * 1.5f).coerceAtMost(100f)
            val expScore = ((yearsScore * 0.5f) + (jobsScore * 0.5f)).coerceIn(10f, 100f)
            val seWeight = normalizedWeights[RankingCriterion.SERVICE_EXPERIENCE] ?: RankingCriterion.SERVICE_EXPERIENCE.defaultWeight
            criterionScores.add(
                CriterionScore(
                    criterion = RankingCriterion.SERVICE_EXPERIENCE,
                    weight = seWeight,
                    rawValue = "${provider.yearsExperience} yrs (${provider.completedJobsCount} jobs)",
                    normalizedScore = expScore,
                    weightedScore = expScore * seWeight,
                    explanation = "Composite of verified active trade years and job volume"
                )
            )

            // 4. Provider Availability
            val hasSlot = provider.availableTimeslots.contains(requestedTimeslot)
            val availScore = when {
                hasSlot && provider.isInstantBookingEnabled -> 100.0f
                hasSlot -> 88.0f
                else -> 35.0f
            }
            val availText = when {
                hasSlot && provider.isInstantBookingEnabled -> "Open & Instant Confirmation ready"
                hasSlot -> "Available in schedule"
                else -> "Limited / On-call notice required"
            }
            val paWeight = normalizedWeights[RankingCriterion.PROVIDER_AVAILABILITY] ?: RankingCriterion.PROVIDER_AVAILABILITY.defaultWeight
            criterionScores.add(
                CriterionScore(
                    criterion = RankingCriterion.PROVIDER_AVAILABILITY,
                    weight = paWeight,
                    rawValue = availText,
                    normalizedScore = availScore,
                    weightedScore = availScore * paWeight,
                    explanation = "Real-time calendar slot readiness for ${requestedTimeslot.label}"
                )
            )

            // 5. Customer Feedback Sentiment
            val cfWeight = normalizedWeights[RankingCriterion.CUSTOMER_FEEDBACK] ?: RankingCriterion.CUSTOMER_FEEDBACK.defaultWeight
            criterionScores.add(
                CriterionScore(
                    criterion = RankingCriterion.CUSTOMER_FEEDBACK,
                    weight = cfWeight,
                    rawValue = "${String.format(Locale.US, "%.0f", provider.positiveFeedbackPercent)}%",
                    normalizedScore = provider.positiveFeedbackPercent.coerceIn(0f, 100f),
                    weightedScore = provider.positiveFeedbackPercent * cfWeight,
                    explanation = "Positive testimonial ratio & client recommendation rate"
                )
            )

            // 6. Response Rate
            val rrWeight = normalizedWeights[RankingCriterion.RESPONSE_RATE] ?: RankingCriterion.RESPONSE_RATE.defaultWeight
            criterionScores.add(
                CriterionScore(
                    criterion = RankingCriterion.RESPONSE_RATE,
                    weight = rrWeight,
                    rawValue = "${String.format(Locale.US, "%.0f", provider.responseRatePercent)}%",
                    normalizedScore = provider.responseRatePercent.coerceIn(0f, 100f),
                    weightedScore = provider.responseRatePercent * rrWeight,
                    explanation = "Speed and reliability in acknowledging booking requests"
                )
            )

            // 7. Service Completion Rate
            val scWeight = normalizedWeights[RankingCriterion.SERVICE_COMPLETION_RATE] ?: RankingCriterion.SERVICE_COMPLETION_RATE.defaultWeight
            criterionScores.add(
                CriterionScore(
                    criterion = RankingCriterion.SERVICE_COMPLETION_RATE,
                    weight = scWeight,
                    rawValue = "${String.format(Locale.US, "%.0f", provider.completionRatePercent)}%",
                    normalizedScore = provider.completionRatePercent.coerceIn(0f, 100f),
                    weightedScore = provider.completionRatePercent * scWeight,
                    explanation = "Successful project delivery without dispute"
                )
            )

            // 8. Cancellation Rate (Inverted penalty)
            val cancellationScore = (100.0f - (provider.cancellationRatePercent * 2.5f)).coerceIn(10f, 100f)
            val caWeight = normalizedWeights[RankingCriterion.CANCELLATION_RATE] ?: RankingCriterion.CANCELLATION_RATE.defaultWeight
            criterionScores.add(
                CriterionScore(
                    criterion = RankingCriterion.CANCELLATION_RATE,
                    weight = caWeight,
                    rawValue = "${String.format(Locale.US, "%.1f", provider.cancellationRatePercent)}% cancelled",
                    normalizedScore = cancellationScore,
                    weightedScore = cancellationScore * caWeight,
                    explanation = "Inverted penalty score; lower cancellation yields higher reliability"
                )
            )

            // 9. Distance / Location
            val distanceScore = (100.0f - (distanceKm * 7.5f)).coerceIn(15f, 100f)
            val dlWeight = normalizedWeights[RankingCriterion.DISTANCE_LOCATION] ?: RankingCriterion.DISTANCE_LOCATION.defaultWeight
            criterionScores.add(
                CriterionScore(
                    criterion = RankingCriterion.DISTANCE_LOCATION,
                    weight = dlWeight,
                    rawValue = "${String.format(Locale.US, "%.1f", distanceKm)} km (${provider.baseBarangay.barangayName})",
                    normalizedScore = distanceScore,
                    weightedScore = distanceScore * dlWeight,
                    explanation = "Proximity from ${customerBarangay.barangayName} to provider base"
                )
            )

            // Sum all weighted scores to yield Overall Priority Score (OPS)
            val ops = criterionScores.map { it.weightedScore }.sum().coerceIn(0f, 100f)

            RankedProvider(
                provider = provider,
                overallPriorityScore = ops,
                criterionScores = criterionScores,
                rank = 0, // will assign after sorting
                distanceKm = distanceKm
            )
        }

        // Sort descending by Overall Priority Score (OPS)
        return evaluated
            .sortedByDescending { it.overallPriorityScore }
            .mapIndexed { index, rankedProvider ->
                rankedProvider.copy(rank = index + 1)
            }
    }
}
