# Skill-Based Service Booking Application with Weighted Priority Ranking (Dasmariñas City)

A specialized Android service booking platform designed for Dasmariñas, Cavite, implementing the thesis Chapter 1 multi-criteria decision-making model to match customers with qualified, available local service providers via Weighted Priority Ranking.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural decisions incorporate the choices confirmed in Phase 1:

- **Confirmed Decision 1: Multi-Role Switcher Active by Default**:
  - The application will feature a prominent top/navigation role switcher allowing users and thesis evaluators to seamlessly toggle between **Customer**, **Service Provider**, and **Administrator** views without logging out.
- **Confirmed Decision 2: Transparent Weighted Priority Ranking Score Card**:
  - Every matching provider will display an expandable detailed score card showing exact scores across all 9 thesis criteria, the applied weights, the mathematical formula $\sum (W_i \times S_i)$, and why that provider was ranked in that position.
- **Confirmed Decision 3: Core Thesis Categories + Expandability**:
  - Out of the box, the app features the five core thesis categories: **Plumbing**, **Electrical Repair**, **Appliance Maintenance**, **Cleaning**, and **Carpentry**, with Admin capabilities to add new categories and sub-skills.
- **Confirmed Decision 4: Dasmariñas Local Barangay & Proximity Modeling**:
  - Built-in coverage of key Dasmariñas barangays (Zone IV - AISAT College area, Salitran, Burol, Sampaloc, Paliparan, San Agustin, Fatima, Langkaan) with realistic distance and availability matrices.

---

## 1. Overview & Core Concept

### What It Does
The application connects residents of Dasmariñas City with verified local service providers. When a customer specifies their service requirement (category, needed sub-skills, barangay location, and preferred timeslot), the system evaluates qualified providers using the **Weighted Priority Ranking (WPR)** algorithm.

The system features:
1. **Weighted Priority Ranking Engine**: Calculates an Overall Priority Score (OPS) across 9 distinct criteria specified in Chapter 1.
2. **Real-Time Availability Tracking**: Dynamic calendar and timeslot management (Morning, Afternoon, Evening) preventing double-booking.
3. **Automated Booking Confirmation & Lifecycle**: Transitions bookings from `Pending` → `Accepted` → `Ongoing` → `Completed` (or `Cancelled`), with automated instant confirmation rules based on provider availability.
4. **Provider Profile & Skill Management**: Direct management of trades, certifications, hourly/job rates, and active schedule.
5. **Administrative Dashboard**: Real-time monitoring of provider rankings, booking activity, completion/cancellation rates, and custom weight adjustments for algorithm calibration.

---

## 2. User Experience & Visual Design

### Key User Flows

```
[Role Switcher: Customer / Provider / Admin]
       │
       ├─► CUSTOMER FLOW:
       │    1. Select Service Category (Plumbing, Electrical, Appliances, etc.)
       │    2. Fill Request Details (Sub-skills needed, Barangay in Dasma, Schedule, Urgency)
       │    3. View Ranked Providers (Rank 1, 2, 3...) with Overall Priority Score (OPS)
       │    4. Tap "View Score Breakdown" to inspect math formula & 9 criteria scores
       │    5. Submit Booking Request -> Instant automated confirmation & status tracker
       │    6. Post-service review & rating (updates provider's rating in real-time)
       │
       ├─► PROVIDER FLOW:
       │    1. View Profile, Skills, and active Dasmariñas service zones
       │    2. Toggle Real-time Availability (Active/Inactive, Timeslots: 8am-12pm, 1pm-5pm, 5pm-8pm)
       │    3. View Incoming Bookings & manage status (Accept, Start Job, Mark Complete)
       │    4. View Performance Metrics (Response Rate %, Completion Rate %, Star Rating)
       │
       └─► ADMIN FLOW:
            1. View Overall System Analytics (Bookings count, Active Providers, Avg Satisfaction)
            2. Calibration Studio: Adjust weights of the 9 ranking criteria with live preview
            3. Manage Service Categories and inspect Dasmariñas barangay coverage
```

### Visual Identity & Theme
- **Theme Concept**: Clean Civic Modern — inspired by reliable municipal and home service tools with high readability, trustworthy blues, energetic amber accents, and clean surface depth.
- **Color Palette (Material Design 3)**:
  - Primary: Deep Cavite Blue (`#0F4C81`) — represents reliability and professionalism
  - Secondary / Accent: Safety Amber (`#F59E0B`) — represents trade craftsmanship and priority scoring
  - Tertiary / Success: Emerald (`#10B981`) — automated confirmations and high availability
  - Background: Crisp Soft Slate (`#F8FAFC` light / `#0F172A` dark)
  - Surface Containers: Elevated white cards with subtle tonal borders (`#E2E8F0`)
- **Typography**: Clean, robust hierarchy with legible display headings and tabular figures for numerical priority scores.
- **Micro-Interactions**:
  - Smooth animated progress bars for each of the 9 criterion scores
  - Animated status pills (`Pending`, `Accepted`, `Ongoing`, `Completed`)
  - Real-time score re-ranking animations when weights are adjusted in Admin mode

---

## 3. Key Product Decisions & Trade-Offs

- **Multi-Role In-App Switcher vs. Separate Authentication Gates**:
  - *Chosen*: Integrated role switcher in the top bar / drawer allowing rapid toggling between Customer, Provider, and Admin.
  - *Why*: Critical for thesis defense, demonstration, and evaluation without needing 3 different physical devices or complex account logouts.
- **Formula Transparency**:
  - *Chosen*: Detailed card breakdown and an "Inspect Ranking Formula" dialog displaying exact formulas:
    $$\text{OPS} = \sum_{i=1}^9 (W_i \times S_i)$$
  - *Why*: Directly validates Chapter 1, Section 1.5 & 1.6 of the thesis for research evaluators.
- **Data Persistence Strategy**:
  - *Chosen*: Robust in-memory + local state repository pre-seeded with rich, realistic Dasmariñas data (AISAT campus area, Salitran, Burol, Paliparan, etc.) ensuring zero network failure risk during presentations and rapid instant testing.
- **Automated Confirmation Logic**:
  - *Chosen*: Providers with active instant-booking availability for the selected timeslot receive automated confirmation, instantly moving the status to `Accepted` and notifying the customer.

---

## 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                        Jetpack Compose UI Layer                        │
├────────────────────────────────────────────────────────────────────────┤
│  TopAppBar (Role Switcher: Customer | Provider | Admin)                │
│  ├─ CustomerScreen (CategoryGrid, RequestSheet, RankedResultsList)     │
│  ├─ ProviderScreen (AvailabilityToggle, SkillManager, JobQueue)        │
│  ├─ AdminScreen (WeightsEditor, MetricsDashboard, SystemAudit)         │
│  └─ ScoreBreakdownDialog (Mathematical formula & 9-factor radar/bars)   │
└─────────────────────────────────▲──────────────────────────────────────┘
                                  │ (StateFlow / Events)
┌─────────────────────────────────┴──────────────────────────────────────┐
│                    ServiceBookingViewModel                             │
│  - currentRole: UserRole                                               │
│  - selectedCategory: ServiceCategory?                                  │
│  - rankingWeights: Map<RankingCriteria, Float>                         │
│  - activeBookings: List<BookingRequest>                                │
│  - providers: List<ServiceProvider>                                    │
│  - rankedResults: List<RankedProvider>                                 │
└─────────────────────────────────▲──────────────────────────────────────┘
                                  │
┌─────────────────────────────────┴──────────────────────────────────────┐
│             WeightedPriorityRankingEngine (Core Thesis Logic)          │
│  Computes normalized scores for 9 criteria:                            │
│   1. Skill Match (0-100)        6. Response Rate (0-100)               │
│   2. Customer Rating (0-100)    7. Completion Rate (0-100)             │
│   3. Service Experience (0-100) 8. Cancellation Rate (0-100 inverted)  │
│   4. Provider Availability      9. Proximity/Distance (0-100)          │
│   5. Customer Feedback Sentiment                                       │
│  Result: Overall Priority Score (OPS) & descending sort                │
└─────────────────────────────────▲──────────────────────────────────────┘
                                  │
┌─────────────────────────────────┴──────────────────────────────────────┐
│                  ServiceBookingRepository (Local Data)                 │
│  - Pre-seeded Dasmariñas service providers, skills, and barangay geo   │
│  - Real-time booking state updates & audit logging                     │
└────────────────────────────────────────────────────────────────────────┘
```

### The 9 Ranking Criteria & Default Thesis Weights

| Criterion | Default Weight | Description & Calculation |
| :--- | :--- | :--- |
| **Skill Match** | 20% | Degree of match between customer requested sub-skills and provider certifications |
| **Customer Rating** | 15% | Historical rating average (1.0 to 5.0 scaled to 0–100) |
| **Service Experience** | 10% | Verified years in trade and completed jobs volume |
| **Provider Availability** | 15% | Immediate availability for requested date/timeslot (100 = open, 50 = partial) |
| **Customer Feedback** | 10% | Positive review sentiment ratio and repeat client percentage |
| **Response Rate** | 10% | Percentage of inquiries answered within standard turnaround |
| **Service Completion Rate** | 10% | Ratio of completed bookings over accepted requests |
| **Cancellation Rate** | 5% | Inverted penalty score ($100 - \text{Cancellation}\%$) |
| **Distance / Proximity** | 5% | Proximity between provider's home barangay and customer's Dasma barangay |
| **Total** | **100%** | Normalized Overall Priority Score (0–100) |

---

## 5. Verification Plan

- **Automated Verification**:
  - Run `compile_applet` to ensure full build success, syntax correctness, and resource linkage.
- **Thesis Scenario Testing**:
  1. **Customer Matching Scenario**: Search for "Plumbing" in "Barangay Zone IV" for "Morning" timeslot $\rightarrow$ Verify providers are ranked by OPS with detailed breakdown.
  2. **Score Card Inspection**: Expand the top provider's score breakdown $\rightarrow$ Confirm all 9 criteria and formula values are mathematically accurate.
  3. **Automated Booking Confirmation**: Book the top provider $\rightarrow$ Verify booking status immediately transitions to `Accepted` with confirmation code.
  4. **Provider Lifecycle Scenario**: Switch role to "Service Provider" $\rightarrow$ Verify the new job appears, update status to `Ongoing` and then `Completed`.
  5. **Admin Calibration Scenario**: Switch role to "Admin" $\rightarrow$ Adjust "Distance/Location" weight higher $\rightarrow$ Re-run customer search to observe re-ordered rankings in real time.
