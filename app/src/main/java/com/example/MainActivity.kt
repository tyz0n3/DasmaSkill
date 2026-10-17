package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.UserRole
import com.example.ui.AddCategoryDialog
import com.example.ui.AdminScreen
import com.example.ui.AutomatedConfirmationDialog
import com.example.ui.CustomerScreen
import com.example.ui.ProviderScreen
import com.example.ui.RatingReviewDialog
import com.example.ui.RoleSwitcherHeader
import com.example.ui.ScoreBreakdownDialog
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.ServiceBookingViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppContainer()
            }
        }
    }
}

@Composable
fun MainAppContainer(
    viewModel: ServiceBookingViewModel = viewModel()
) {
    val currentRole by viewModel.currentRole.collectAsState()
    val inspectionTarget by viewModel.inspectionTarget.collectAsState()
    val latestConfirmedBooking by viewModel.latestConfirmedBooking.collectAsState()
    val ratingTargetBooking by viewModel.ratingTargetBooking.collectAsState()
    val showAddCategoryDialog by viewModel.showAddCategoryDialog.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            RoleSwitcherHeader(
                currentRole = currentRole,
                onRoleSelected = { viewModel.setRole(it) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentRole) {
                UserRole.CUSTOMER -> {
                    CustomerScreen(viewModel = viewModel)
                }
                UserRole.PROVIDER -> {
                    ProviderScreen(viewModel = viewModel)
                }
                UserRole.ADMIN -> {
                    AdminScreen(viewModel = viewModel)
                }
            }

            // Dialogs
            inspectionTarget?.let { ranked ->
                ScoreBreakdownDialog(
                    rankedProvider = ranked,
                    onDismiss = { viewModel.dismissScoreBreakdown() }
                )
            }

            latestConfirmedBooking?.let { booking ->
                AutomatedConfirmationDialog(
                    booking = booking,
                    onDismiss = { viewModel.dismissConfirmationDialog() },
                    onViewBookings = {
                        // Switch to customer bookings tab if needed
                    }
                )
            }

            ratingTargetBooking?.let { booking ->
                RatingReviewDialog(
                    booking = booking,
                    onDismiss = { viewModel.dismissRatingDialog() },
                    onSubmit = { rating, review ->
                        viewModel.submitRating(booking.id, rating, review)
                    }
                )
            }

            if (showAddCategoryDialog) {
                AddCategoryDialog(
                    onDismiss = { viewModel.dismissAddCategoryDialog() },
                    onAdd = { title, desc, skills, fee ->
                        viewModel.createCategory(title, desc, skills, fee)
                    }
                )
            }
        }
    }
}
