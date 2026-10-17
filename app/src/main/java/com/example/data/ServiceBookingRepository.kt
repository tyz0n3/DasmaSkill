package com.example.data

import com.example.model.AuditLogEntry
import com.example.model.BookingRequest
import com.example.model.BookingStatus
import com.example.model.DasmariñasBarangay
import com.example.model.RankingCriterion
import com.example.model.ServiceCategory
import com.example.model.ServiceProvider
import com.example.model.Timeslot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class ServiceBookingRepository {

    private val _categories = MutableStateFlow<List<ServiceCategory>>(initialCategories)
    val categories: StateFlow<List<ServiceCategory>> = _categories.asStateFlow()

    private val _providers = MutableStateFlow<List<ServiceProvider>>(initialProviders)
    val providers: StateFlow<List<ServiceProvider>> = _providers.asStateFlow()

    private val _bookings = MutableStateFlow<List<BookingRequest>>(initialBookings)
    val bookings: StateFlow<List<BookingRequest>> = _bookings.asStateFlow()

    private val _rankingWeights = MutableStateFlow<Map<RankingCriterion, Float>>(
        RankingCriterion.values().associateWith { it.defaultWeight }
    )
    val rankingWeights: StateFlow<Map<RankingCriterion, Float>> = _rankingWeights.asStateFlow()

    private val _auditLogs = MutableStateFlow<List<AuditLogEntry>>(initialAuditLogs)
    val auditLogs: StateFlow<List<AuditLogEntry>> = _auditLogs.asStateFlow()

    fun updateRankingWeights(newWeights: Map<RankingCriterion, Float>) {
        _rankingWeights.value = newWeights
        addAuditLog(
            title = "Ranking Criteria Weights Calibrated",
            details = "Admin modified weights for provider matching algorithm.",
            category = "Algorithm"
        )
    }

    fun resetRankingWeights() {
        _rankingWeights.value = RankingCriterion.values().associateWith { it.defaultWeight }
        addAuditLog(
            title = "Ranking Weights Reset",
            details = "Reverted weights to thesis baseline defaults.",
            category = "Algorithm"
        )
    }

    fun createBooking(
        customerName: String,
        customerContact: String,
        customerBarangay: DasmariñasBarangay,
        specificAddress: String,
        categoryId: String,
        requestedSkills: List<String>,
        scheduledDate: String,
        timeslot: Timeslot,
        notes: String,
        providerId: String,
        overallPriorityScore: Float
    ): BookingRequest {
        val provider = _providers.value.find { it.id == providerId }
        val isInstant = provider?.isInstantBookingEnabled == true &&
                provider.availableTimeslots.contains(timeslot)

        val initialStatus = if (isInstant) BookingStatus.ACCEPTED else BookingStatus.PENDING
        val codeNumber = (1000..9999).random()
        val bookingCode = "DSMA-$codeNumber"
        val timestamp = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.US).format(Date())

        val newBooking = BookingRequest(
            id = UUID.randomUUID().toString(),
            bookingCode = bookingCode,
            customerName = customerName,
            customerContact = customerContact,
            customerBarangay = customerBarangay,
            specificAddress = specificAddress,
            categoryId = categoryId,
            requestedSkills = requestedSkills,
            scheduledDate = scheduledDate,
            timeslot = timeslot,
            notes = notes,
            providerId = providerId,
            status = initialStatus,
            automatedConfirmation = isInstant,
            createdAt = timestamp,
            overallPriorityScore = overallPriorityScore
        )

        _bookings.value = listOf(newBooking) + _bookings.value
        addAuditLog(
            title = if (isInstant) "Instant Booking Confirmed ($bookingCode)" else "New Booking Requested ($bookingCode)",
            details = "Customer $customerName booked provider ${provider?.name ?: "Unknown"} in ${customerBarangay.barangayName}. Status: ${initialStatus.label}",
            category = "Booking"
        )
        return newBooking
    }

    fun updateBookingStatus(bookingId: String, newStatus: BookingStatus) {
        _bookings.value = _bookings.value.map { booking ->
            if (booking.id == bookingId) {
                booking.copy(status = newStatus)
            } else {
                booking
            }
        }
        val booking = _bookings.value.find { it.id == bookingId }
        addAuditLog(
            title = "Booking Status Updated",
            details = "Booking ${booking?.bookingCode} transitioned to ${newStatus.label}",
            category = "Lifecycle"
        )
    }

    fun submitReview(bookingId: String, rating: Float, reviewText: String) {
        val booking = _bookings.value.find { it.id == bookingId } ?: return
        _bookings.value = _bookings.value.map { b ->
            if (b.id == bookingId) {
                b.copy(ratingGiven = rating, reviewGiven = reviewText)
            } else {
                b
            }
        }

        // Update provider rating & review count
        _providers.value = _providers.value.map { provider ->
            if (provider.id == booking.providerId) {
                val newTotal = provider.totalReviews + 1
                val newRating = ((provider.rating * provider.totalReviews) + rating) / newTotal
                provider.copy(
                    rating = String.format(Locale.US, "%.1f", newRating).toFloat(),
                    totalReviews = newTotal,
                    completedJobsCount = provider.completedJobsCount + 1
                )
            } else {
                provider
            }
        }

        addAuditLog(
            title = "Customer Review Submitted",
            details = "Rating: $rating stars on ${booking.bookingCode}. Provider metrics updated in real-time.",
            category = "Review"
        )
    }

    fun updateProviderAvailability(
        providerId: String,
        slots: Set<Timeslot>,
        instantBooking: Boolean
    ) {
        _providers.value = _providers.value.map { p ->
            if (p.id == providerId) {
                p.copy(
                    availableTimeslots = slots,
                    isInstantBookingEnabled = instantBooking
                )
            } else {
                p
            }
        }
        val provider = _providers.value.find { it.id == providerId }
        addAuditLog(
            title = "Provider Timetable Changed",
            details = "${provider?.name} updated availability slots to: ${slots.map { it.label }.joinToString()}",
            category = "Provider"
        )
    }

    fun addServiceCategory(title: String, description: String, subSkills: List<String>, fee: String) {
        val newCategory = ServiceCategory(
            id = "cat_${UUID.randomUUID().toString().take(6)}",
            title = title,
            iconName = "build",
            description = description,
            subSkills = subSkills,
            estimatedFeePesos = fee
        )
        _categories.value = _categories.value + newCategory
        addAuditLog(
            title = "New Service Category Added",
            details = "Admin created '$title' with ${subSkills.size} skills.",
            category = "Admin"
        )
    }

    private fun addAuditLog(title: String, details: String, category: String) {
        val entry = AuditLogEntry(
            id = UUID.randomUUID().toString(),
            timestamp = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date()),
            title = title,
            details = details,
            category = category
        )
        _auditLogs.value = listOf(entry) + _auditLogs.value.take(25)
    }

    companion object {
        val initialCategories = listOf(
            ServiceCategory(
                id = "plumbing",
                title = "Plumbing",
                iconName = "plumbing",
                description = "Pipe leaks, water drainage, faucet fixtures, line installation",
                subSkills = listOf(
                    "Pipe Leak Repair",
                    "Drain Unclogging",
                    "Toilet & Faucet Installation",
                    "Water Heater Setup",
                    "Water Pump Maintenance"
                ),
                estimatedFeePesos = "₱450 - ₱1,200"
            ),
            ServiceCategory(
                id = "electrical",
                title = "Electrical Repair",
                iconName = "bolt",
                description = "Wiring troubleshooting, breaker trips, lighting, outlets",
                subSkills = listOf(
                    "Circuit Breaker Repair",
                    "Wiring Troubleshooting",
                    "Lighting & Chandelier Setup",
                    "Ceiling Fan Installation",
                    "Heavy Appliance Wiring"
                ),
                estimatedFeePesos = "₱500 - ₱1,500"
            ),
            ServiceCategory(
                id = "appliance",
                title = "Appliance Maintenance",
                iconName = "kitchen",
                description = "Inverter air conditioning, refrigerator, washing machine repair",
                subSkills = listOf(
                    "Aircon Deep Cleaning & Freon",
                    "Inverter Aircon Board Repair",
                    "Refrigerator Compressor Diagnosis",
                    "Washing Machine Repair",
                    "Microwave & Oven Servicing"
                ),
                estimatedFeePesos = "₱650 - ₱2,000"
            ),
            ServiceCategory(
                id = "cleaning",
                title = "Cleaning Services",
                iconName = "cleaning_services",
                description = "Deep house sanitation, post-construction, upholstery, disinfection",
                subSkills = listOf(
                    "Post-Construction Deep Cleaning",
                    "Sofa & Mattress Disinfection",
                    "Kitchen Grease Extraction",
                    "Window & Glass Washing",
                    "Bathroom Tile Acid Wash"
                ),
                estimatedFeePesos = "₱800 - ₱2,500"
            ),
            ServiceCategory(
                id = "carpentry",
                title = "Carpentry",
                iconName = "handyman",
                description = "Cabinetry, wooden doors, furniture repair, structural woodwork",
                subSkills = listOf(
                    "Kitchen Cabinet Customization",
                    "Door Jamb & Lock Alignment",
                    "Wooden Furniture Restoration",
                    "Drywall & Partition Framing",
                    "Ceiling & Eaves Wood Repair"
                ),
                estimatedFeePesos = "₱600 - ₱1,800"
            )
        )

        val initialProviders = listOf(
            ServiceProvider(
                id = "prov_1",
                name = "Eduardo 'Eddie' Santos",
                tradeTitle = "Master Plumber & Pipe Specialist",
                contactNumber = "0917-482-9102",
                baseBarangay = DasmariñasBarangay.ZONE_IV, // Near AISAT
                categoryIds = listOf("plumbing"),
                certifiedSkills = listOf(
                    "Pipe Leak Repair",
                    "Drain Unclogging",
                    "Toilet & Faucet Installation",
                    "Water Pump Maintenance"
                ),
                yearsExperience = 8,
                completedJobsCount = 142,
                rating = 4.9f,
                totalReviews = 86,
                responseRatePercent = 98f,
                completionRatePercent = 97f,
                cancellationRatePercent = 1.2f,
                positiveFeedbackPercent = 99f,
                availableTimeslots = setOf(Timeslot.MORNING, Timeslot.AFTERNOON),
                isInstantBookingEnabled = true,
                hourlyRatePesos = 500,
                bio = "TESDA certified master plumber based right in Zone IV near Emilio Aguinaldo Highway. Fast response and 100% guaranteed leak repair."
            ),
            ServiceProvider(
                id = "prov_2",
                name = "Ramon 'Mon' Bautista",
                tradeTitle = "Licensed Residential Electrician",
                contactNumber = "0928-391-7291",
                baseBarangay = DasmariñasBarangay.SALITRAN_I,
                categoryIds = listOf("electrical"),
                certifiedSkills = listOf(
                    "Circuit Breaker Repair",
                    "Wiring Troubleshooting",
                    "Lighting & Chandelier Setup",
                    "Heavy Appliance Wiring"
                ),
                yearsExperience = 11,
                completedJobsCount = 210,
                rating = 4.8f,
                totalReviews = 114,
                responseRatePercent = 95f,
                completionRatePercent = 98f,
                cancellationRatePercent = 2.0f,
                positiveFeedbackPercent = 97f,
                availableTimeslots = setOf(Timeslot.MORNING, Timeslot.AFTERNOON, Timeslot.EVENING),
                isInstantBookingEnabled = true,
                hourlyRatePesos = 550,
                bio = "Master electrician serving Dasmariñas for over a decade. Experienced in residential power drops, breaker tripping issues, and safety code compliance."
            ),
            ServiceProvider(
                id = "prov_3",
                name = "Mark Joseph 'MJ' Dela Cruz",
                tradeTitle = "HVAC & Inverter Aircon Tech",
                contactNumber = "0919-872-3310",
                baseBarangay = DasmariñasBarangay.SAN_AGUSTIN_I, // Near SM Dasma
                categoryIds = listOf("appliance"),
                certifiedSkills = listOf(
                    "Aircon Deep Cleaning & Freon",
                    "Inverter Aircon Board Repair",
                    "Refrigerator Compressor Diagnosis"
                ),
                yearsExperience = 6,
                completedJobsCount = 89,
                rating = 4.7f,
                totalReviews = 52,
                responseRatePercent = 92f,
                completionRatePercent = 94f,
                cancellationRatePercent = 3.5f,
                positiveFeedbackPercent = 94f,
                availableTimeslots = setOf(Timeslot.MORNING, Timeslot.AFTERNOON),
                isInstantBookingEnabled = false,
                hourlyRatePesos = 650,
                bio = "Inverter AC specialist with complete pressure gauges, vacuum pumps, and genuine replacement capacitors."
            ),
            ServiceProvider(
                id = "prov_4",
                name = "Maria 'Elena' Ramos",
                tradeTitle = "Professional Deep Cleaning Lead",
                contactNumber = "0935-128-4490",
                baseBarangay = DasmariñasBarangay.BUROL_I,
                categoryIds = listOf("cleaning"),
                certifiedSkills = listOf(
                    "Post-Construction Deep Cleaning",
                    "Sofa & Mattress Disinfection",
                    "Kitchen Grease Extraction",
                    "Bathroom Tile Acid Wash"
                ),
                yearsExperience = 5,
                completedJobsCount = 110,
                rating = 4.9f,
                totalReviews = 78,
                responseRatePercent = 99f,
                completionRatePercent = 99f,
                cancellationRatePercent = 0.5f,
                positiveFeedbackPercent = 100f,
                availableTimeslots = setOf(Timeslot.MORNING, Timeslot.AFTERNOON),
                isInstantBookingEnabled = true,
                hourlyRatePesos = 700,
                bio = "Head cleaner with heavy-duty steam sanitizers and industrial vacuums. Highly rated by families in Burol and Salitran."
            ),
            ServiceProvider(
                id = "prov_5",
                name = "Danilo 'Mang Danny' Perez",
                tradeTitle = "Master Furniture & Wood Craftsman",
                contactNumber = "0918-992-1102",
                baseBarangay = DasmariñasBarangay.SAMPALOC_I,
                categoryIds = listOf("carpentry"),
                certifiedSkills = listOf(
                    "Kitchen Cabinet Customization",
                    "Door Jamb & Lock Alignment",
                    "Wooden Furniture Restoration",
                    "Ceiling & Eaves Wood Repair"
                ),
                yearsExperience = 15,
                completedJobsCount = 280,
                rating = 4.8f,
                totalReviews = 160,
                responseRatePercent = 94f,
                completionRatePercent = 97f,
                cancellationRatePercent = 1.8f,
                positiveFeedbackPercent = 96f,
                availableTimeslots = setOf(Timeslot.MORNING, Timeslot.AFTERNOON),
                isInstantBookingEnabled = true,
                hourlyRatePesos = 550,
                bio = "Specializing in custom kitchen cabinets, locksets, and mahogany finish restoration across Dasmariñas."
            ),
            ServiceProvider(
                id = "prov_6",
                name = "Arnel Villanueva",
                tradeTitle = "Certified Plumber & Drainage Pro",
                contactNumber = "0945-812-7033",
                baseBarangay = DasmariñasBarangay.SALITRAN_IV,
                categoryIds = listOf("plumbing"),
                certifiedSkills = listOf(
                    "Drain Unclogging",
                    "Water Heater Setup",
                    "Pipe Leak Repair"
                ),
                yearsExperience = 4,
                completedJobsCount = 58,
                rating = 4.6f,
                totalReviews = 34,
                responseRatePercent = 88f,
                completionRatePercent = 91f,
                cancellationRatePercent = 4.0f,
                positiveFeedbackPercent = 91f,
                availableTimeslots = setOf(Timeslot.AFTERNOON, Timeslot.EVENING),
                isInstantBookingEnabled = true,
                hourlyRatePesos = 450,
                bio = "Equipped with snake unclogging tools and thermal leak detectors. Available for afternoon and evening emergency repairs."
            ),
            ServiceProvider(
                id = "prov_7",
                name = "Christopher 'Chris' Tan",
                tradeTitle = "Domestic Electrical Specialist",
                contactNumber = "0916-221-8845",
                baseBarangay = DasmariñasBarangay.PALIPARAN_I,
                categoryIds = listOf("electrical"),
                certifiedSkills = listOf(
                    "Lighting & Chandelier Setup",
                    "Ceiling Fan Installation",
                    "Wiring Troubleshooting"
                ),
                yearsExperience = 3,
                completedJobsCount = 42,
                rating = 4.5f,
                totalReviews = 25,
                responseRatePercent = 89f,
                completionRatePercent = 90f,
                cancellationRatePercent = 5.0f,
                positiveFeedbackPercent = 88f,
                availableTimeslots = setOf(Timeslot.MORNING, Timeslot.AFTERNOON),
                isInstantBookingEnabled = false,
                hourlyRatePesos = 400,
                bio = "Affordable residential electrician serving Paliparan and surrounding areas. Quality work at reasonable rates."
            ),
            ServiceProvider(
                id = "prov_8",
                name = "Reynaldo 'Rey' Mendoza",
                tradeTitle = "Major Appliance Technician",
                contactNumber = "0920-554-1290",
                baseBarangay = DasmariñasBarangay.ZONE_I,
                categoryIds = listOf("appliance"),
                certifiedSkills = listOf(
                    "Washing Machine Repair",
                    "Refrigerator Compressor Diagnosis",
                    "Microwave & Oven Servicing"
                ),
                yearsExperience = 9,
                completedJobsCount = 175,
                rating = 4.9f,
                totalReviews = 95,
                responseRatePercent = 97f,
                completionRatePercent = 96f,
                cancellationRatePercent = 1.5f,
                positiveFeedbackPercent = 98f,
                availableTimeslots = setOf(Timeslot.MORNING, Timeslot.AFTERNOON, Timeslot.EVENING),
                isInstantBookingEnabled = true,
                hourlyRatePesos = 600,
                bio = "Specializing in automatic front-load washing machines, twin tubs, and no-frost refrigerators. Genuine spare parts provided."
            )
        )

        val initialBookings = listOf(
            BookingRequest(
                id = "b_1",
                bookingCode = "DSMA-8419",
                customerName = "Prof. Carl Nathaniel Salvatus",
                customerContact = "0917-300-0149",
                customerBarangay = DasmariñasBarangay.ZONE_IV,
                specificAddress = "AISAT Building, Emilio Aguinaldo Highway",
                categoryId = "plumbing",
                requestedSkills = listOf("Pipe Leak Repair", "Drain Unclogging"),
                scheduledDate = "Tomorrow",
                timeslot = Timeslot.MORNING,
                notes = "Water pressure leak near faculty bathroom floor drain.",
                providerId = "prov_1",
                status = BookingStatus.ACCEPTED,
                automatedConfirmation = true,
                createdAt = "Today - 10:15 AM",
                overallPriorityScore = 96.4f
            ),
            BookingRequest(
                id = "b_2",
                bookingCode = "DSMA-7320",
                customerName = "Alia Margaret Lares",
                customerContact = "0917-123-8116",
                customerBarangay = DasmariñasBarangay.SALITRAN_I,
                specificAddress = "Blk 4 Lot 12, Golden City Subdivision",
                categoryId = "electrical",
                requestedSkills = listOf("Circuit Breaker Repair", "Wiring Troubleshooting"),
                scheduledDate = "Today",
                timeslot = Timeslot.AFTERNOON,
                notes = "Main circuit breaker trips whenever second aircon turns on.",
                providerId = "prov_2",
                status = BookingStatus.ONGOING,
                automatedConfirmation = true,
                createdAt = "Today - 08:30 AM",
                overallPriorityScore = 93.8f
            ),
            BookingRequest(
                id = "b_3",
                bookingCode = "DSMA-6102",
                customerName = "Richard Jr Abiol",
                customerContact = "0922-445-9981",
                customerBarangay = DasmariñasBarangay.BUROL_MAIN,
                specificAddress = "Kadiwa Center Commercial Unit 3",
                categoryId = "cleaning",
                requestedSkills = listOf("Post-Construction Deep Cleaning", "Kitchen Grease Extraction"),
                scheduledDate = "Yesterday",
                timeslot = Timeslot.MORNING,
                notes = "Renovation cleanup completed smoothly.",
                providerId = "prov_4",
                status = BookingStatus.COMPLETED,
                automatedConfirmation = true,
                createdAt = "Oct 15, 2026",
                overallPriorityScore = 97.2f,
                ratingGiven = 5.0f,
                reviewGiven = "Super fast and spotless cleaning! Very polite and professional team."
            )
        )

        val initialAuditLogs = listOf(
            AuditLogEntry(
                id = "log_1",
                timestamp = "09:00:12",
                title = "WPR Matching Engine Initialized",
                details = "Normalized weights loaded for Dasmariñas locality (Zone IV baseline).",
                category = "System"
            ),
            AuditLogEntry(
                id = "log_2",
                timestamp = "09:30:45",
                title = "Automated Booking Confirmation Triggered",
                details = "Instant match verified for DSMA-8419 with Eduardo Santos (Score: 96.4).",
                category = "Automation"
            )
        )
    }
}
