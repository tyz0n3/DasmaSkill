package com.example.model

/**
 * User roles supported in the thesis demonstration.
 */
enum class UserRole(val label: String, val description: String) {
    CUSTOMER("Customer", "Search, compare rankings, and book services"),
    PROVIDER("Service Provider", "Manage availability, skills, and accept jobs"),
    ADMIN("Administrator", "Monitor system analytics and calibrate ranking weights")
}

/**
 * The 9 evaluation criteria defined in Chapter 1 (Section 1.5) of the thesis.
 */
enum class RankingCriterion(
    val code: String,
    val displayName: String,
    val defaultWeight: Float,
    val description: String,
    val unit: String
) {
    SKILL_MATCH(
        code = "SM",
        displayName = "Skill Match",
        defaultWeight = 0.20f,
        description = "Compatibility of provider certifications with requested task requirements",
        unit = "% match"
    ),
    CUSTOMER_RATING(
        code = "CR",
        displayName = "Customer Rating",
        defaultWeight = 0.15f,
        description = "Historical average evaluation from verified customer reviews (1.0 to 5.0)",
        unit = "stars"
    ),
    SERVICE_EXPERIENCE(
        code = "SE",
        displayName = "Service Experience",
        defaultWeight = 0.10f,
        description = "Accumulated years of active trade practice and verified completed jobs",
        unit = "yrs / jobs"
    ),
    PROVIDER_AVAILABILITY(
        code = "PA",
        displayName = "Provider Availability",
        defaultWeight = 0.15f,
        description = "Real-time timetable readiness for requested date and timeslot",
        unit = "slot status"
    ),
    CUSTOMER_FEEDBACK(
        code = "CF",
        displayName = "Customer Feedback",
        defaultWeight = 0.10f,
        description = "Percentage of positive feedback sentiment and client recommendations",
        unit = "% positive"
    ),
    RESPONSE_RATE(
        code = "RR",
        displayName = "Response Rate",
        defaultWeight = 0.10f,
        description = "Ratio of service requests acknowledged within standard turnaround",
        unit = "% responded"
    ),
    SERVICE_COMPLETION_RATE(
        code = "SC",
        displayName = "Completion Rate",
        defaultWeight = 0.10f,
        description = "Percentage of accepted bookings successfully completed without dispute",
        unit = "% completed"
    ),
    CANCELLATION_RATE(
        code = "CA",
        displayName = "Cancellation Rate",
        defaultWeight = 0.05f,
        description = "Inverted reliability score where low cancellation maximizes points",
        unit = "% cancelled"
    ),
    DISTANCE_LOCATION(
        code = "DL",
        displayName = "Distance / Proximity",
        defaultWeight = 0.05f,
        description = "Physical closeness between customer location and provider base in Dasmariñas",
        unit = "km distance"
    )
}

/**
 * Timeslots for scheduling appointments.
 */
enum class Timeslot(val label: String, val timeRange: String) {
    MORNING("Morning", "8:00 AM - 12:00 PM"),
    AFTERNOON("Afternoon", "1:00 PM - 5:00 PM"),
    EVENING("Evening", "5:00 PM - 8:00 PM")
}

/**
 * Key Barangays in Dasmariñas City, Cavite with approximate relative coordinates for distance calculation.
 */
enum class DasmariñasBarangay(
    val barangayName: String,
    val landmark: String,
    val latOffset: Double,
    val lngOffset: Double
) {
    ZONE_IV("Zone IV (Poblacion)", "AISAT College Campus / Aguinaldo Hwy", 14.3275, 120.9360),
    ZONE_I("Zone I (Poblacion)", "Dasmariñas City Hall / Plaza", 14.3290, 120.9372),
    SALITRAN_I("Salitran I", "Near Central Mall / Aguinaldo Hwy", 14.3410, 120.9450),
    SALITRAN_II("Salitran II", "Salitran Access Road", 14.3480, 120.9490),
    SALITRAN_IV("Salitran IV", "San Marino City", 14.3540, 120.9530),
    BUROL_I("Burol I", "DLSU-Dasmariñas Vicinity", 14.3180, 120.9420),
    BUROL_MAIN("Burol Main", "Kadiwa Park / Market", 14.3120, 120.9480),
    SAMPALOC_I("Sampaloc I", "Governor's Drive Junction", 14.3050, 120.9320),
    SAMPALOC_IV("Sampaloc IV", "Dasmariñas Resettlement Area", 14.2980, 120.9380),
    PALIPARAN_I("Paliparan I", "Paliparan Commercial Strip", 14.3160, 120.9920),
    PALIPARAN_III("Paliparan III", "Molino-Paliparan Road", 14.3050, 121.0020),
    SAN_AGUSTIN_I("San Agustin I", "SM City Dasmariñas / Robinson's Place", 14.3010, 120.9570),
    SAN_AGUSTIN_II("San Agustin II", "Governor's Drive South", 14.2950, 120.9610),
    FATIMA_I("Fatima I", "Area B / Congressional Road", 14.3310, 120.9620),
    LANGKAAN_I("Langkaan I", "FCIE Industrial Zone", 14.2820, 120.9550),
    SABANG("Sabang", "Sabang Public Elementary", 14.3380, 120.9250);

    companion object {
        fun calculateDistanceKm(b1: DasmariñasBarangay, b2: DasmariñasBarangay): Float {
            if (b1 == b2) return 0.5f // within same barangay
            val latDiff = (b1.latOffset - b2.latOffset) * 111.0
            val lngDiff = (b1.lngOffset - b2.lngOffset) * 105.0
            val distance = Math.sqrt(latDiff * latDiff + lngDiff * lngDiff)
            return String.format("%.1f", Math.max(0.8, distance)).toFloat()
        }
    }
}

/**
 * Service category model.
 */
data class ServiceCategory(
    val id: String,
    val title: String,
    val iconName: String,
    val description: String,
    val subSkills: List<String>,
    val estimatedFeePesos: String
)

/**
 * Service provider profile model.
 */
data class ServiceProvider(
    val id: String,
    val name: String,
    val tradeTitle: String,
    val contactNumber: String,
    val baseBarangay: DasmariñasBarangay,
    val categoryIds: List<String>,
    val certifiedSkills: List<String>,
    val yearsExperience: Int,
    val completedJobsCount: Int,
    val rating: Float, // 1.0 to 5.0
    val totalReviews: Int,
    val responseRatePercent: Float, // e.g. 98%
    val completionRatePercent: Float, // e.g. 96%
    val cancellationRatePercent: Float, // e.g. 2%
    val positiveFeedbackPercent: Float, // e.g. 97%
    val availableTimeslots: Set<Timeslot>,
    val isInstantBookingEnabled: Boolean,
    val hourlyRatePesos: Int,
    val bio: String
)

/**
 * Booking status transitions.
 */
enum class BookingStatus(val label: String) {
    PENDING("Pending Confirmation"),
    ACCEPTED("Accepted / Confirmed"),
    ONGOING("Service Ongoing"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled")
}

/**
 * Service request and booking transaction.
 */
data class BookingRequest(
    val id: String,
    val bookingCode: String,
    val customerName: String,
    val customerContact: String,
    val customerBarangay: DasmariñasBarangay,
    val specificAddress: String,
    val categoryId: String,
    val requestedSkills: List<String>,
    val scheduledDate: String,
    val timeslot: Timeslot,
    val notes: String,
    val providerId: String,
    val status: BookingStatus,
    val automatedConfirmation: Boolean,
    val createdAt: String,
    val overallPriorityScore: Float,
    val ratingGiven: Float? = null,
    val reviewGiven: String? = null
)

/**
 * Mathematical breakdown for each criterion of the Weighted Priority Ranking.
 */
data class CriterionScore(
    val criterion: RankingCriterion,
    val weight: Float, // e.g. 0.20
    val rawValue: String, // e.g. "4.9 stars"
    val normalizedScore: Float, // 0 to 100
    val weightedScore: Float, // normalizedScore * weight
    val explanation: String
)

/**
 * Ranked provider evaluation result.
 */
data class RankedProvider(
    val provider: ServiceProvider,
    val overallPriorityScore: Float, // 0 to 100
    val criterionScores: List<CriterionScore>,
    val rank: Int,
    val distanceKm: Float
)

/**
 * Admin Audit and Activity Log.
 */
data class AuditLogEntry(
    val id: String,
    val timestamp: String,
    val title: String,
    val details: String,
    val category: String
)
