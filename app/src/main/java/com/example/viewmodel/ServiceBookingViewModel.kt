package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ServiceBookingRepository
import com.example.model.AuditLogEntry
import com.example.model.BookingRequest
import com.example.model.BookingStatus
import com.example.model.DasmariñasBarangay
import com.example.model.RankedProvider
import com.example.model.RankingCriterion
import com.example.model.ServiceCategory
import com.example.model.ServiceProvider
import com.example.model.Timeslot
import com.example.model.UserRole
import com.example.ranking.WeightedPriorityRankingEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class ServiceBookingViewModel(
    private val repository: ServiceBookingRepository = ServiceBookingRepository()
) : ViewModel() {

    val categories: StateFlow<List<ServiceCategory>> = repository.categories
    val providers: StateFlow<List<ServiceProvider>> = repository.providers
    val bookings: StateFlow<List<BookingRequest>> = repository.bookings
    val rankingWeights: StateFlow<Map<RankingCriterion, Float>> = repository.rankingWeights
    val auditLogs: StateFlow<List<AuditLogEntry>> = repository.auditLogs

    // Active User Role Switcher
    private val _currentRole = MutableStateFlow(UserRole.CUSTOMER)
    val currentRole: StateFlow<UserRole> = _currentRole.asStateFlow()

    // Active Provider Selected for Provider View
    private val _activeProviderId = MutableStateFlow("prov_1")
    val activeProviderId: StateFlow<String> = _activeProviderId.asStateFlow()

    // Customer Form & Matching State
    private val _selectedCategoryId = MutableStateFlow("plumbing")
    val selectedCategoryId: StateFlow<String> = _selectedCategoryId.asStateFlow()

    private val _selectedSkills = MutableStateFlow<Set<String>>(
        setOf("Pipe Leak Repair", "Drain Unclogging")
    )
    val selectedSkills: StateFlow<Set<String>> = _selectedSkills.asStateFlow()

    private val _customerBarangay = MutableStateFlow(DasmariñasBarangay.ZONE_IV)
    val customerBarangay: StateFlow<DasmariñasBarangay> = _customerBarangay.asStateFlow()

    private val _customerTimeslot = MutableStateFlow(Timeslot.MORNING)
    val customerTimeslot: StateFlow<Timeslot> = _customerTimeslot.asStateFlow()

    private val _customerAddress = MutableStateFlow("Zone IV, near AISAT College Campus")
    val customerAddress: StateFlow<String> = _customerAddress.asStateFlow()

    private val _customerNotes = MutableStateFlow("Emergency pipe leak under bathroom sink")
    val customerNotes: StateFlow<String> = _customerNotes.asStateFlow()

    private val _customerName = MutableStateFlow("Carl Nathaniel Salvatus")
    val customerName: StateFlow<String> = _customerName.asStateFlow()

    private val _customerContact = MutableStateFlow("0917-300-0149")
    val customerContact: StateFlow<String> = _customerContact.asStateFlow()

    // Detailed Score Breakdown Dialog State
    private val _inspectionTarget = MutableStateFlow<RankedProvider?>(null)
    val inspectionTarget: StateFlow<RankedProvider?> = _inspectionTarget.asStateFlow()

    // Automated Confirmation Success Dialog
    private val _latestConfirmedBooking = MutableStateFlow<BookingRequest?>(null)
    val latestConfirmedBooking: StateFlow<BookingRequest?> = _latestConfirmedBooking.asStateFlow()

    // Rating modal target
    private val _ratingTargetBooking = MutableStateFlow<BookingRequest?>(null)
    val ratingTargetBooking: StateFlow<BookingRequest?> = _ratingTargetBooking.asStateFlow()

    // Admin Add Category dialog
    private val _showAddCategoryDialog = MutableStateFlow(false)
    val showAddCategoryDialog: StateFlow<Boolean> = _showAddCategoryDialog.asStateFlow()

    private data class CustomerMatchCriteria(
        val categoryId: String,
        val skills: Set<String>,
        val barangay: DasmariñasBarangay,
        val timeslot: Timeslot
    )

    private val customerCriteriaFlow = combine(
        selectedCategoryId,
        selectedSkills,
        customerBarangay,
        customerTimeslot
    ) { catId, skills, barangay, slot ->
        CustomerMatchCriteria(catId, skills, barangay, slot)
    }

    // Dynamically Computed Ranked Providers using Weighted Priority Ranking Engine
    val rankedProviders: StateFlow<List<RankedProvider>> = combine(
        providers,
        customerCriteriaFlow,
        rankingWeights
    ) { allProviders, criteria, weights ->
        WeightedPriorityRankingEngine.rankProviders(
            providers = allProviders,
            categoryId = criteria.categoryId,
            requestedSkills = criteria.skills.toList(),
            customerBarangay = criteria.barangay,
            requestedTimeslot = criteria.timeslot,
            customWeights = weights
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun setRole(role: UserRole) {
        _currentRole.value = role
    }

    fun setActiveProviderId(id: String) {
        _activeProviderId.value = id
    }

    fun selectCategory(categoryId: String) {
        _selectedCategoryId.value = categoryId
        val category = categories.value.find { it.id == categoryId }
        // Pre-select first 2 skills as defaults
        _selectedSkills.value = category?.subSkills?.take(2)?.toSet() ?: emptySet()
    }

    fun toggleSkill(skill: String) {
        val current = _selectedSkills.value.toMutableSet()
        if (current.contains(skill)) {
            current.remove(skill)
        } else {
            current.add(skill)
        }
        _selectedSkills.value = current
    }

    fun setCustomerBarangay(barangay: DasmariñasBarangay) {
        _customerBarangay.value = barangay
    }

    fun setCustomerTimeslot(timeslot: Timeslot) {
        _customerTimeslot.value = timeslot
    }

    fun setCustomerAddress(address: String) {
        _customerAddress.value = address
    }

    fun setCustomerNotes(notes: String) {
        _customerNotes.value = notes
    }

    fun setCustomerName(name: String) {
        _customerName.value = name
    }

    fun setCustomerContact(contact: String) {
        _customerContact.value = contact
    }

    fun showScoreBreakdown(rankedProvider: RankedProvider) {
        _inspectionTarget.value = rankedProvider
    }

    fun dismissScoreBreakdown() {
        _inspectionTarget.value = null
    }

    fun dismissConfirmationDialog() {
        _latestConfirmedBooking.value = null
    }

    fun openRatingDialog(booking: BookingRequest) {
        _ratingTargetBooking.value = booking
    }

    fun dismissRatingDialog() {
        _ratingTargetBooking.value = null
    }

    fun openAddCategoryDialog() {
        _showAddCategoryDialog.value = true
    }

    fun dismissAddCategoryDialog() {
        _showAddCategoryDialog.value = false
    }

    fun bookRankedProvider(rankedProvider: RankedProvider) {
        val booking = repository.createBooking(
            customerName = _customerName.value.ifBlank { "Resident Customer" },
            customerContact = _customerContact.value.ifBlank { "0917-000-0000" },
            customerBarangay = _customerBarangay.value,
            specificAddress = _customerAddress.value.ifBlank { _customerBarangay.value.barangayName },
            categoryId = _selectedCategoryId.value,
            requestedSkills = _selectedSkills.value.toList(),
            scheduledDate = "Tomorrow",
            timeslot = _customerTimeslot.value,
            notes = _customerNotes.value,
            providerId = rankedProvider.provider.id,
            overallPriorityScore = rankedProvider.overallPriorityScore
        )
        _latestConfirmedBooking.value = booking
    }

    fun updateBookingStatus(bookingId: String, newStatus: BookingStatus) {
        repository.updateBookingStatus(bookingId, newStatus)
    }

    fun submitRating(bookingId: String, rating: Float, reviewText: String) {
        repository.submitReview(bookingId, rating, reviewText)
        _ratingTargetBooking.value = null
    }

    fun updateRankingWeight(criterion: RankingCriterion, newWeight: Float) {
        val current = repository.rankingWeights.value.toMutableMap()
        current[criterion] = newWeight.coerceIn(0.01f, 0.60f)
        repository.updateRankingWeights(current)
    }

    fun resetRankingWeights() {
        repository.resetRankingWeights()
    }

    fun toggleProviderSlot(slot: Timeslot) {
        val provider = providers.value.find { it.id == _activeProviderId.value } ?: return
        val current = provider.availableTimeslots.toMutableSet()
        if (current.contains(slot)) {
            current.remove(slot)
        } else {
            current.add(slot)
        }
        repository.updateProviderAvailability(provider.id, current, provider.isInstantBookingEnabled)
    }

    fun toggleProviderInstantBooking() {
        val provider = providers.value.find { it.id == _activeProviderId.value } ?: return
        repository.updateProviderAvailability(
            provider.id,
            provider.availableTimeslots,
            !provider.isInstantBookingEnabled
        )
    }

    fun createCategory(title: String, description: String, skills: List<String>, fee: String) {
        repository.addServiceCategory(title, description, skills, fee)
        _showAddCategoryDialog.value = false
    }
}
