package com.tripsplit.ui

import android.widget.Toast
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tripsplit.data.Expense
import com.tripsplit.data.Member
import com.tripsplit.data.Trip
import com.tripsplit.data.TripStore
import com.tripsplit.data.memberName
import com.tripsplit.R
import com.tripsplit.settlement.SettlementCalculator
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import kotlin.random.Random

private val GlassShape = RoundedCornerShape(8.dp)
private val LiquidShape = RoundedCornerShape(28.dp)
private val PosterShape = RoundedCornerShape(26.dp)
private val GlassBorder = Color(0xFFF6DCA7).copy(alpha = 0.34f)
private val GlassBorderBright = Color(0xFFFFF3C8).copy(alpha = 0.72f)
private val AccentMint = Color(0xFF3DC8BB)
private val AccentIndigo = Color(0xFF167E96)
private val AccentAmber = Color(0xFFF6B35F)
private val AccentPink = Color(0xFFE77962)
private val AccentCoral = Color(0xFFFF916F)
private val AccentLime = Color(0xFFB7C969)
private val AccentViolet = Color(0xFF4A7A88)
private val AccentSky = Color(0xFF84D9D3)
private val DeepInk = Color(0xFF07343C)
private val PalmInk = Color(0xFF06282F)
private val Sand = Color(0xFFFFD08B)
private val Cream = Color(0xFFFFF3D0)
private val InkOnGlow = Color(0xFF172D32)
private val ExpenseTypePresets = listOf(
    "Groceries",
    "Food",
    "Gas",
    "Rent",
    "Lodging",
    "Transport",
    "Activities",
    "Tickets",
    "Drinks",
    "Supplies",
    "Other",
)

@Composable
fun TripSplitApp(store: TripStore) {
    val context = LocalContext.current
    var trips by remember { mutableStateOf(store.loadTrips()) }
    var currentTripId by rememberSaveable { mutableStateOf(trips.firstOrNull()?.id) }
    var currentMemberId by rememberSaveable { mutableStateOf(trips.firstOrNull()?.members?.firstOrNull()?.id) }
    var showingEntry by rememberSaveable { mutableStateOf(trips.isEmpty()) }

    fun saveTrips(updatedTrips: List<Trip>) {
        trips = updatedTrips
        store.saveTrips(updatedTrips)
    }

    fun replaceTrip(updatedTrip: Trip) {
        saveTrips(trips.map { trip -> if (trip.id == updatedTrip.id) updatedTrip else trip })
    }

    LaunchedEffect(trips) {
        val selectedTrip = trips.firstOrNull { it.id == currentTripId } ?: trips.firstOrNull()
        if (selectedTrip == null) {
            currentTripId = null
            currentMemberId = null
            showingEntry = true
        } else {
            if (currentTripId == null || trips.none { it.id == currentTripId }) {
                currentTripId = selectedTrip.id
            }
            if (currentMemberId == null || selectedTrip.members.none { it.id == currentMemberId }) {
                currentMemberId = selectedTrip.members.firstOrNull()?.id
            }
        }
    }

    val currentTrip = trips.firstOrNull { it.id == currentTripId }
    val currentMember = currentTrip?.members?.firstOrNull { it.id == currentMemberId }

    val handleCreateTrip: (String, String) -> Unit = { rawTripName, rawAdminName ->
        val tripName = rawTripName.trim()
        val adminName = rawAdminName.trim()
        when {
            tripName.isBlank() -> toast(context, "Add a trip name")
            adminName.isBlank() -> toast(context, "Add your name")
            else -> {
                val admin = Member(id = newId("member"), name = adminName, isAdmin = true)
                val trip = Trip(
                    id = newId("trip"),
                    name = tripName,
                    code = randomCode(trips.map { it.code }.toSet()),
                    createdAt = System.currentTimeMillis(),
                    members = listOf(admin),
                )
                saveTrips(trips + trip)
                currentTripId = trip.id
                currentMemberId = admin.id
                showingEntry = false
            }
        }
    }

    val handleJoinTrip: (String, String) -> Unit = { rawCode, rawName ->
        val code = rawCode.trim().uppercase()
        val name = rawName.trim()
        val trip = trips.firstOrNull { it.code.equals(code, ignoreCase = true) }
        when {
            code.isBlank() -> toast(context, "Enter an invite code")
            name.isBlank() -> toast(context, "Add your name")
            trip == null -> toast(context, "No saved trip uses that code")
            trip.isEnded -> toast(context, "That trip has ended")
            trip.members.any { it.name.equals(name, ignoreCase = true) } -> toast(context, "That name is already in the trip")
            else -> {
                val member = Member(id = newId("member"), name = name, isAdmin = false)
                val updatedTrip = trip.copy(members = trip.members + member)
                replaceTrip(updatedTrip)
                currentTripId = updatedTrip.id
                currentMemberId = member.id
                showingEntry = false
            }
        }
    }

    val handleAddExpense: (String, Long, Set<String>, String) -> Unit = { rawTitle, amountCents, participantIds, payerId ->
        val trip = currentTrip
        val title = rawTitle.trim()
        when {
            trip == null || currentMember == null -> Unit
            trip.isEnded -> toast(context, "This trip has ended")
            title.isBlank() -> toast(context, "Add what was paid for")
            amountCents <= 0L -> toast(context, "Add an amount")
            participantIds.isEmpty() -> toast(context, "Choose who used it")
            trip.members.none { it.id == payerId } -> toast(context, "Choose who paid")
            !currentMember.isAdmin && payerId != currentMember.id -> toast(context, "Only admins can add expenses for others")
            else -> {
                val expense = Expense(
                    id = newId("expense"),
                    title = title,
                    amountCents = amountCents,
                    payerId = payerId,
                    participantIds = participantIds.toList(),
                    createdAt = System.currentTimeMillis(),
                )
                replaceTrip(trip.copy(expenses = trip.expenses + expense))
            }
        }
    }

    val handleAddGuest: (String) -> Unit = { rawName ->
        val trip = currentTrip
        val actor = currentMember
        val name = rawName.trim()
        when {
            trip == null || actor == null -> Unit
            !actor.isAdmin -> toast(context, "Only admins can invite guests")
            trip.isEnded -> toast(context, "This trip has ended")
            name.isBlank() -> toast(context, "Add a guest name")
            trip.members.any { it.name.equals(name, ignoreCase = true) } -> toast(context, "That name is already in the trip")
            else -> {
                replaceTrip(trip.copy(members = trip.members + Member(id = newId("member"), name = name)))
                toast(context, "Guest added")
            }
        }
    }

    val handlePromote: (String) -> Unit = { memberId ->
        val trip = currentTrip
        val actor = currentMember
        when {
            trip == null || actor == null -> Unit
            !actor.isAdmin -> toast(context, "Only admins can promote guests")
            trip.isEnded -> toast(context, "This trip has ended")
            else -> {
                val updatedMembers = trip.members.map { member ->
                    if (member.id == memberId) member.copy(isAdmin = true) else member
                }
                replaceTrip(trip.copy(members = updatedMembers))
            }
        }
    }

    val handleEndTrip: () -> Unit = {
        val trip = currentTrip
        val actor = currentMember
        when {
            trip == null || actor == null -> Unit
            !actor.isAdmin -> toast(context, "Only admins can end the trip")
            trip.isEnded -> Unit
            else -> replaceTrip(trip.copy(isEnded = true))
        }
    }

    if (showingEntry || currentTrip == null || currentMember == null) {
        EntryScreen(
            savedTrips = trips,
            onCreateTrip = handleCreateTrip,
            onJoinTrip = handleJoinTrip,
            onOpenTrip = { tripId ->
                val trip = trips.firstOrNull { it.id == tripId }
                currentTripId = tripId
                currentMemberId = trip?.members?.firstOrNull()?.id
                showingEntry = false
            },
        )
    } else {
        TripHomeScreen(
            trip = currentTrip,
            currentMember = currentMember,
            onShowEntry = { showingEntry = true },
            onSwitchMember = { currentMemberId = it },
            onAddExpense = handleAddExpense,
            onAddGuest = handleAddGuest,
            onPromote = handlePromote,
            onEndTrip = handleEndTrip,
        )
    }
}

@Composable
private fun EntryScreen(
    savedTrips: List<Trip>,
    onCreateTrip: (String, String) -> Unit,
    onJoinTrip: (String, String) -> Unit,
    onOpenTrip: (String) -> Unit,
) {
    var tripName by rememberSaveable { mutableStateOf("") }
    var adminName by rememberSaveable { mutableStateOf("") }
    var inviteCode by rememberSaveable { mutableStateOf("") }
    var guestName by rememberSaveable { mutableStateOf("") }

    GlassBackground {
        Scaffold(containerColor = Color.Transparent) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                TravelPosterHero(
                    title = "TripSplit",
                    kicker = "TROPICAL GROUP WALLET",
                    subtitle = "Invite friends. Track spend. Settle the trip.",
                )

                SectionCard {
                    Text("Create trip", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = tripName,
                        onValueChange = { tripName = it },
                        label = { Text("Trip name") },
                        singleLine = true,
                        colors = glassTextFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = adminName,
                        onValueChange = { adminName = it },
                        label = { Text("Your name") },
                        singleLine = true,
                        colors = glassTextFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(12.dp))
                    LiquidPrimaryButton(
                        text = "Create",
                        onClick = { onCreateTrip(tripName, adminName) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                SectionCard {
                    Text("Join by code", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = inviteCode,
                        onValueChange = { inviteCode = it.uppercase() },
                        label = { Text("Invite code") },
                        singleLine = true,
                        colors = glassTextFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = guestName,
                        onValueChange = { guestName = it },
                        label = { Text("Your name") },
                        singleLine = true,
                        colors = glassTextFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(12.dp))
                    LiquidSecondaryButton(
                        text = "Join",
                        onClick = { onJoinTrip(inviteCode, guestName) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                if (savedTrips.isNotEmpty()) {
                    SectionCard {
                        Text("Saved trips", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        savedTrips.forEachIndexed { index, trip ->
                            if (index > 0) GlassDivider()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(trip.name, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "${trip.members.size} people  |  ${trip.expenses.size} expenses",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                LiquidSecondaryButton(text = "Open", onClick = { onOpenTrip(trip.id) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TripHomeScreen(
    trip: Trip,
    currentMember: Member,
    onShowEntry: () -> Unit,
    onSwitchMember: (String) -> Unit,
    onAddExpense: (String, Long, Set<String>, String) -> Unit,
    onAddGuest: (String) -> Unit,
    onPromote: (String) -> Unit,
    onEndTrip: () -> Unit,
) {
    var selectedTab by rememberSaveable(trip.id) { mutableStateOf(0) }
    val tabs = listOf("Expenses", "Summary", "Admin")

    GlassBackground {
        Scaffold(
            containerColor = Color.Transparent,
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                TravelTripHero(
                    trip = trip,
                    onShowEntry = onShowEntry,
                    modifier = Modifier.padding(16.dp),
                )
                TripStatusHeader(
                    trip = trip,
                    currentMember = currentMember,
                    onSwitchMember = onSwitchMember,
                )
                LiquidTabBar(
                    tabs = tabs,
                    selectedTab = selectedTab,
                    onSelected = { selectedTab = it },
                    modifier = Modifier
                        .padding(horizontal = 16.dp),
                )
                when (selectedTab) {
                    0 -> ExpensesTab(
                        trip = trip,
                        currentMember = currentMember,
                        onAddExpense = onAddExpense,
                    )
                    1 -> SummaryTab(trip = trip)
                    2 -> AdminTab(
                        trip = trip,
                        currentMember = currentMember,
                        onAddGuest = onAddGuest,
                        onPromote = onPromote,
                        onEndTrip = onEndTrip,
                    )
                }
            }
        }
    }
}

@Composable
private fun TripStatusHeader(
    trip: Trip,
    currentMember: Member,
    onSwitchMember: (String) -> Unit,
) {
    val totalCents = remember(trip.expenses) { trip.expenses.sumOf { it.amountCents } }

    SectionCard(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = "Live trip board",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = formatMoney(totalCents),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                )
            }
            StatusPill(text = if (trip.isEnded) "Ended" else "Active", live = !trip.isEnded)
        }
        Spacer(Modifier.height(14.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatTile("People", trip.members.size.toString(), AccentSky)
            StatTile("Expenses", trip.expenses.size.toString(), AccentPink)
            StatTile("Admins", trip.members.count { it.isAdmin }.toString(), AccentLime)
        }
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Acting as", style = MaterialTheme.typography.labelMedium)
                MemberPicker(
                    members = trip.members,
                    currentMember = currentMember,
                    onSwitchMember = onSwitchMember,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun StatusPill(text: String, live: Boolean) {
    val border = if (live) Sand.copy(alpha = 0.72f) else GlassBorder
    val fill = if (live) Sand.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.08f)

    Box(
        modifier = Modifier
            .clip(LiquidShape)
            .background(fill)
            .border(BorderStroke(1.dp, border), LiquidShape)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (live) Cream else Color.White,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun TravelPosterHero(
    title: String,
    kicker: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(360.dp)
            .shadow(24.dp, PosterShape, clip = false)
            .clip(PosterShape)
            .background(AccentSky)
            .border(BorderStroke(1.dp, Cream.copy(alpha = 0.32f)), PosterShape),
    ) {
        TravelSceneCanvas(showBus = true, modifier = Modifier.fillMaxSize())
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            DeepInk.copy(alpha = 0.86f),
                            DeepInk.copy(alpha = 0.98f),
                        ),
                    ),
                )
                .padding(18.dp),
        ) {
            Column {
                Text(
                    text = kicker,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Sand,
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Cream.copy(alpha = 0.86f),
                )
                Spacer(Modifier.height(14.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MiniGlowPill("Invite", Sand)
                    MiniGlowPill("Spend", AccentCoral)
                    MiniGlowPill("Settle", AccentSky)
                }
            }
        }
    }
}

@Composable
private fun TravelTripHero(
    trip: Trip,
    onShowEntry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val totalCents = remember(trip.expenses) { trip.expenses.sumOf { it.amountCents } }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp)
            .shadow(22.dp, PosterShape, clip = false)
            .clip(PosterShape)
            .background(AccentSky)
            .border(BorderStroke(1.dp, Cream.copy(alpha = 0.34f)), PosterShape),
    ) {
        TravelSceneCanvas(showBus = false, modifier = Modifier.fillMaxSize())
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            LogoMark(size = 44)
            LiquidSecondaryButton(
                text = "Trips",
                onClick = onShowEntry,
                modifier = Modifier.widthIn(min = 92.dp),
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "TRIP BOARD",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Cream.copy(alpha = 0.82f),
            )
            Text(
                text = trip.name,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "CODE ${trip.code}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Sand,
            )
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            DeepInk.copy(alpha = 0.80f),
                            DeepInk.copy(alpha = 0.98f),
                        ),
                    ),
                )
                .padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("TOTAL", style = MaterialTheme.typography.labelSmall, color = Cream.copy(alpha = 0.72f))
                Text(formatMoney(totalCents), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Color.White)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("FRIENDS", style = MaterialTheme.typography.labelSmall, color = Cream.copy(alpha = 0.72f))
                Text("${trip.members.size}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Sand)
            }
        }
    }
}

@Composable
private fun TravelSceneCanvas(showBus: Boolean, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "travelPosterScene")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(9000), repeatMode = RepeatMode.Reverse),
        label = "sceneDrift",
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRect(
            brush = Brush.verticalGradient(
                listOf(
                    Color(0xFF78DAD3),
                    Color(0xFF8BD9D0),
                    Color(0xFFF8C47A),
                    Color(0xFFE57F5D),
                    DeepInk,
                ),
                startY = 0f,
                endY = h,
            ),
        )
        drawCircle(
            color = Color(0xFFFFF0B8).copy(alpha = 0.92f),
            radius = w * 0.12f,
            center = Offset(w * (0.20f + drift * 0.03f), h * 0.42f),
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.30f),
            radius = w * 0.20f,
            center = Offset(w * (0.20f + drift * 0.03f), h * 0.42f),
        )

        val farMountain = Path().apply {
            moveTo(w * 0.18f, h * 0.68f)
            lineTo(w * 0.52f, h * 0.28f)
            lineTo(w * 0.88f, h * 0.68f)
            close()
        }
        drawPath(
            path = farMountain,
            brush = Brush.verticalGradient(
                listOf(
                    Cream.copy(alpha = 0.82f),
                    Color(0xFFD18863).copy(alpha = 0.86f),
                    DeepInk.copy(alpha = 0.58f),
                ),
                startY = h * 0.26f,
                endY = h * 0.70f,
            ),
        )
        val nearRockLeft = Path().apply {
            moveTo(w * 0.08f, h * 0.76f)
            lineTo(w * 0.26f, h * 0.48f)
            lineTo(w * 0.40f, h * 0.78f)
            close()
        }
        val nearRockRight = Path().apply {
            moveTo(w * 0.64f, h * 0.78f)
            lineTo(w * 0.82f, h * 0.44f)
            lineTo(w * 1.04f, h * 0.78f)
            close()
        }
        drawPath(nearRockLeft, color = Color(0xFF8F463C).copy(alpha = 0.84f))
        drawPath(nearRockRight, color = Color(0xFF7B3940).copy(alpha = 0.86f))

        repeat(4) { index ->
            val y = h * (0.67f + index * 0.04f)
            drawLine(
                color = Color.White.copy(alpha = 0.30f - index * 0.04f),
                start = Offset(w * 0.02f, y),
                end = Offset(w * 0.98f, y + drift * 4f),
                strokeWidth = 2.dp.toPx(),
            )
        }
        drawRect(
            brush = Brush.verticalGradient(
                listOf(
                    AccentIndigo.copy(alpha = 0.42f),
                    DeepInk.copy(alpha = 0.95f),
                ),
                startY = h * 0.66f,
                endY = h,
            ),
            topLeft = Offset(0f, h * 0.66f),
            size = Size(w, h * 0.34f),
        )

        drawPalm(w * 0.14f, h * 0.62f, h * 0.40f)
        drawPalm(w * 0.92f, h * 0.56f, h * 0.32f)
        drawPlane(w * (0.30f + drift * 0.04f), h * 0.32f, 0.70f)
        drawPlane(w * (0.70f - drift * 0.04f), h * 0.36f, 0.54f)

        if (showBus) {
            val road = Path().apply {
                moveTo(0f, h)
                lineTo(w * 0.40f, h * 0.68f)
                lineTo(w * 0.62f, h * 0.68f)
                lineTo(w * 0.90f, h)
                close()
            }
            drawPath(road, color = Color(0xFF263E42).copy(alpha = 0.82f))
            drawLine(
                color = Sand.copy(alpha = 0.82f),
                start = Offset(w * 0.50f, h * 0.72f),
                end = Offset(w * 0.54f, h * 0.96f),
                strokeWidth = 2.dp.toPx(),
            )
            drawRoundRect(
                color = Color(0xFFE8AA66),
                topLeft = Offset(w * 0.22f, h * 0.68f),
                size = Size(w * 0.42f, h * 0.16f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(18.dp.toPx(), 18.dp.toPx()),
            )
            drawRoundRect(
                color = DeepInk.copy(alpha = 0.84f),
                topLeft = Offset(w * 0.30f, h * 0.71f),
                size = Size(w * 0.25f, h * 0.045f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx()),
            )
            drawCircle(color = PalmInk, radius = 8.dp.toPx(), center = Offset(w * 0.30f, h * 0.84f))
            drawCircle(color = PalmInk, radius = 8.dp.toPx(), center = Offset(w * 0.58f, h * 0.84f))
        }

        drawRect(
            brush = Brush.verticalGradient(
                listOf(Color.Transparent, DeepInk.copy(alpha = 0.52f)),
                startY = h * 0.34f,
                endY = h,
            ),
        )
    }
}

@Composable
private fun MiniGlowPill(text: String, accent: Color) {
    Box(
        modifier = Modifier
            .clip(LiquidShape)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Cream.copy(alpha = 0.18f),
                        accent.copy(alpha = 0.24f),
                        DeepInk.copy(alpha = 0.20f),
                    ),
                ),
            )
            .border(BorderStroke(1.dp, accent.copy(alpha = 0.52f)), LiquidShape)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = Cream,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun StatTile(label: String, value: String, accent: Color) {
    Box(
        modifier = Modifier
            .clip(GlassShape)
            .background(
                Brush.linearGradient(
                    listOf(
                        accent.copy(alpha = 0.18f),
                        DeepInk.copy(alpha = 0.82f),
                        Color(0xFF03272F),
                    ),
                ),
            )
            .border(BorderStroke(1.dp, Sand.copy(alpha = 0.26f)), GlassShape)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Column {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Sand,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LiquidTabBar(
    tabs: List<String>,
    selectedTab: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(LiquidShape)
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF0A4650),
                        Color(0xFF062F38),
                        Color(0xFF04252D),
                    ),
                ),
            )
            .border(
                BorderStroke(
                    1.dp,
                    Brush.horizontalGradient(
                        listOf(
                            Cream.copy(alpha = 0.30f),
                            Sand.copy(alpha = 0.28f),
                            AccentSky.copy(alpha = 0.16f),
                        ),
                    ),
                ),
                LiquidShape,
            )
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        tabs.forEachIndexed { index, title ->
            val selected = selectedTab == index
            val accent = when (index) {
                0 -> Sand
                1 -> AccentSky
                else -> AccentCoral
            }
            val interactionSource = remember { MutableInteractionSource() }
            val pressed by interactionSource.collectIsPressedAsState()
            val scale by animateFloatAsState(
                targetValue = if (pressed) 0.96f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                label = "tabScale",
            )
            val selectedAlpha by animateFloatAsState(
                targetValue = if (selected) 1f else 0f,
                animationSpec = tween(180),
                label = "tabSelected",
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .scale(scale)
                    .clip(LiquidShape)
                    .background(
                        if (selected) {
                            Brush.horizontalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.88f),
                                    Sand,
                                    AccentAmber,
                                ),
                            )
                        } else {
                            Brush.horizontalGradient(
                                listOf(
                                    Cream.copy(alpha = 0.08f * (1f - selectedAlpha)),
                                    DeepInk.copy(alpha = 0.10f),
                                ),
                            )
                        },
                    )
                    .border(
                        BorderStroke(
                            1.dp,
                            if (selected) Cream.copy(alpha = 0.78f) else Cream.copy(alpha = 0.14f),
                        ),
                        LiquidShape,
                    )
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                    ) { onSelected(index) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = title,
                    color = if (selected) InkOnGlow else Cream.copy(alpha = 0.88f),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun MemberPicker(
    members: List<Member>,
    currentMember: Member,
    onSwitchMember: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        LiquidSecondaryButton(
            text = currentMember.name + if (currentMember.isAdmin) "  Admin" else "",
            onClick = { expanded = true },
            modifier = modifier,
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            members.forEach { member ->
                DropdownMenuItem(
                    text = {
                        Text(member.name + if (member.isAdmin) "  Admin" else "")
                    },
                    onClick = {
                        onSwitchMember(member.id)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun ExpensePayerPicker(
    members: List<Member>,
    payerId: String,
    enabled: Boolean,
    onPayerSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val payer = members.firstOrNull { it.id == payerId }

    Box {
        LiquidSecondaryButton(
            text = payer?.name ?: "Choose payer",
            onClick = { expanded = true },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            members.forEach { member ->
                DropdownMenuItem(
                    text = {
                        Text(member.name + if (member.isAdmin) "  Admin" else "")
                    },
                    onClick = {
                        onPayerSelected(member.id)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun ExpensesTab(
    trip: Trip,
    currentMember: Member,
    onAddExpense: (String, Long, Set<String>, String) -> Unit,
) {
    val context = LocalContext.current
    var title by rememberSaveable(trip.id) { mutableStateOf("") }
    var amount by rememberSaveable(trip.id) { mutableStateOf("") }
    var selectedExpenseType by rememberSaveable(trip.id) { mutableStateOf(ExpenseTypePresets.first()) }
    var selectedPayerId by rememberSaveable(trip.id) { mutableStateOf(currentMember.id) }
    var selectedParticipantIds by remember(trip.id, trip.members.map { it.id }) {
        mutableStateOf(trip.members.map { it.id }.toSet())
    }

    LaunchedEffect(trip.members, currentMember) {
        val memberIds = trip.members.map { it.id }.toSet()
        selectedParticipantIds = selectedParticipantIds.intersect(memberIds).ifEmpty { memberIds }
        selectedPayerId = when {
            !currentMember.isAdmin -> currentMember.id
            selectedPayerId !in memberIds -> currentMember.id
            else -> selectedPayerId
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SectionCard {
            Text("New expense", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Expense type",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            ExpenseTypePicker(
                selectedType = selectedExpenseType,
                enabled = !trip.isEnded,
                onTypeSelected = { selectedExpenseType = it },
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Custom note (optional)") },
                singleLine = true,
                enabled = !trip.isEnded,
                colors = glassTextFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                enabled = !trip.isEnded,
                colors = glassTextFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            if (currentMember.isAdmin) {
                Text(
                    text = "Paid by",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                ExpensePayerPicker(
                    members = trip.members,
                    payerId = selectedPayerId,
                    enabled = !trip.isEnded,
                    onPayerSelected = { selectedPayerId = it },
                )
            } else {
                Text("Paid by ${currentMember.name}", style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(8.dp))
            PeopleDropdown(
                members = trip.members,
                selectedIds = selectedParticipantIds,
                enabled = !trip.isEnded,
                onSelectionChange = { selectedParticipantIds = it },
            )
            Spacer(Modifier.height(12.dp))
            LiquidPrimaryButton(
                text = "Add expense",
                onClick = {
                    val cents = parseAmountCents(amount)
                    if (cents == null) {
                        toast(context, "Enter a valid amount")
                    } else {
                        val expenseTitle = title.trim().ifBlank { selectedExpenseType }
                        onAddExpense(expenseTitle, cents, selectedParticipantIds, selectedPayerId)
                        title = ""
                        amount = ""
                        selectedExpenseType = ExpenseTypePresets.first()
                        selectedParticipantIds = trip.members.map { it.id }.toSet()
                    }
                },
                enabled = !trip.isEnded,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Text("Expenses", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        if (trip.expenses.isEmpty()) {
            EmptyState("No expenses yet")
        } else {
            trip.expenses.sortedByDescending { it.createdAt }.forEach { expense ->
                ExpenseCard(trip = trip, expense = expense)
            }
        }
    }
}

@Composable
private fun PeopleDropdown(
    members: List<Member>,
    selectedIds: Set<String>,
    enabled: Boolean,
    onSelectionChange: (Set<String>) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val label = when (selectedIds.size) {
        0 -> "Choose people"
        1 -> "1 person selected"
        else -> "${selectedIds.size} people selected"
    }

    Box {
        LiquidSecondaryButton(
            text = label,
            onClick = { expanded = true },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.92f),
        ) {
            members.forEach { member ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = selectedIds.contains(member.id),
                                onCheckedChange = null,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(member.name)
                        }
                    },
                    onClick = {
                        val nextSelection = if (selectedIds.contains(member.id)) {
                            selectedIds - member.id
                        } else {
                            selectedIds + member.id
                        }
                        onSelectionChange(nextSelection)
                    },
                )
            }
        }
    }
}

@Composable
private fun ExpenseTypePicker(
    selectedType: String,
    enabled: Boolean,
    onTypeSelected: (String) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        ExpenseTypePresets.forEach { type ->
            LiquidChoiceChip(
                text = type,
                selected = selectedType == type,
                enabled = enabled,
                onClick = { onTypeSelected(type) },
            )
        }
    }
}

@Composable
private fun LiquidChoiceChip(
    text: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val accent = expenseAccent(text)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = when {
            pressed -> 0.94f
            selected -> 1.03f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "chipScale",
    )
    val brush = if (selected) {
        Brush.horizontalGradient(listOf(Cream.copy(alpha = 0.96f), Sand, accent.copy(alpha = 0.86f)))
    } else {
        Brush.linearGradient(
            listOf(
                Color(0xFF0B4650),
                accent.copy(alpha = 0.14f),
                Color(0xFF052C35),
            ),
        )
    }
    val contentColor = if (selected) InkOnGlow else Color.White

    Button(
        onClick = onClick,
        enabled = enabled,
        shape = LiquidShape,
        interactionSource = interactionSource,
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp, disabledElevation = 0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = contentColor,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = Color.White.copy(alpha = 0.56f),
        ),
        contentPadding = PaddingValues(horizontal = 13.dp, vertical = 0.dp),
        modifier = Modifier
            .height(38.dp)
            .scale(scale)
            .clip(LiquidShape)
            .background(brush)
            .border(
                BorderStroke(
                    1.dp,
                    Brush.horizontalGradient(
                        listOf(
                            Color.White.copy(alpha = if (selected) 0.82f else 0.46f),
                            Sand.copy(alpha = if (selected) 0.82f else 0.28f),
                            accent.copy(alpha = if (selected) 0.62f else 0.28f),
                        ),
                    ),
                ),
                LiquidShape,
            ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(LiquidShape)
                    .background(if (selected) InkOnGlow.copy(alpha = 0.54f) else Sand),
            )
            Text(text = text, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ExpenseCard(trip: Trip, expense: Expense) {
    val accent = expenseAccent(expense.title)
    SectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(LiquidShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Cream.copy(alpha = 0.96f),
                                    Sand,
                                    accent.copy(alpha = 0.80f),
                                ),
                            ),
                        )
                        .border(BorderStroke(1.dp, Cream.copy(alpha = 0.62f)), LiquidShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = expense.title.firstOrNull()?.uppercaseChar()?.toString() ?: "$",
                        color = InkOnGlow,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(expense.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                    Text(
                        "Paid by ${trip.memberName(expense.payerId)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "For ${expense.participantIds.map { trip.memberName(it) }.joinToString()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .clip(LiquidShape)
                    .background(Sand.copy(alpha = 0.18f))
                    .border(BorderStroke(1.dp, Sand.copy(alpha = 0.48f)), LiquidShape)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(
                    formatMoney(expense.amountCents),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Sand,
                )
            }
        }
    }
}

@Composable
private fun SummaryTab(trip: Trip) {
    val balances = remember(trip) { SettlementCalculator.memberBalances(trip) }
    val transfers = remember(trip) { SettlementCalculator.transfers(trip) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SectionCard {
            Text(
                if (trip.isEnded) "Final settlement" else "Current settlement",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(12.dp))
            if (transfers.isEmpty()) {
                EmptyState("Everyone is even")
            } else {
                transfers.forEachIndexed { index, transfer ->
                    if (index > 0) GlassDivider()
                    SettlementRow(
                        from = trip.memberName(transfer.fromMemberId),
                        to = trip.memberName(transfer.toMemberId),
                        amount = transfer.amountCents,
                    )
                }
            }
        }

        SectionCard {
            Text("Balances", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            balances.forEachIndexed { index, balance ->
                if (index > 0) GlassDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(trip.memberName(balance.memberId), style = MaterialTheme.typography.bodyLarge)
                    val label = when {
                        balance.balanceCents > 0L -> "Gets ${formatMoney(balance.balanceCents)}"
                        balance.balanceCents < 0L -> "Pays ${formatMoney(-balance.balanceCents)}"
                        else -> "Even"
                    }
                    Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun SettlementRow(from: String, to: String, amount: Long) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(from, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
            Text("pays $to", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .clip(LiquidShape)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            AccentAmber.copy(alpha = 0.22f),
                            Sand.copy(alpha = 0.20f),
                        ),
                    ),
                )
                .border(BorderStroke(1.dp, Sand.copy(alpha = 0.48f)), LiquidShape)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text(formatMoney(amount), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun AdminTab(
    trip: Trip,
    currentMember: Member,
    onAddGuest: (String) -> Unit,
    onPromote: (String) -> Unit,
    onEndTrip: () -> Unit,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var guestName by rememberSaveable(trip.id) { mutableStateOf("") }
    var confirmEnd by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (!currentMember.isAdmin) {
            SectionCard {
                Text("Only admins can manage this trip", style = MaterialTheme.typography.titleMedium)
            }
            return@Column
        }

        SectionCard {
            Text("Invite code", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text(trip.code, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            LiquidSecondaryButton(
                text = "Copy code",
                onClick = {
                    clipboard.setText(AnnotatedString(trip.code))
                    toast(context, "Code copied")
                },
                enabled = !trip.isEnded,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        SectionCard {
            Text("Add guest", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = guestName,
                onValueChange = { guestName = it },
                label = { Text("Guest name") },
                singleLine = true,
                enabled = !trip.isEnded,
                colors = glassTextFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            LiquidPrimaryButton(
                text = "Add guest",
                onClick = {
                    onAddGuest(guestName)
                    guestName = ""
                },
                enabled = !trip.isEnded,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        SectionCard {
            Text("Admins", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            val guests = trip.members.filter { !it.isAdmin }
            if (guests.isEmpty()) {
                EmptyState("Everyone is an admin")
            } else {
                guests.forEachIndexed { index, member ->
                    if (index > 0) GlassDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(member.name, style = MaterialTheme.typography.bodyLarge)
                        LiquidSecondaryButton(
                            text = "Promote",
                            onClick = { onPromote(member.id) },
                            enabled = !trip.isEnded,
                        )
                    }
                }
            }
        }

        LiquidPrimaryButton(
            text = if (trip.isEnded) "Trip ended" else "End trip",
            onClick = { confirmEnd = true },
            enabled = !trip.isEnded,
            modifier = Modifier.fillMaxWidth(),
            danger = true,
        )
    }

    if (confirmEnd) {
        AlertDialog(
            onDismissRequest = { confirmEnd = false },
            title = { Text("End trip?") },
            text = { Text("Expenses will lock and the settlement will be final.") },
            confirmButton = {
                Button(
                    onClick = {
                        confirmEnd = false
                        onEndTrip()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("End trip")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmEnd = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun LogoMark(size: Int) {
    Image(
        painter = painterResource(id = R.drawable.app_logo),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(BorderStroke(1.dp, GlassBorderBright), RoundedCornerShape(8.dp)),
    )
}

@Composable
private fun LiquidPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    danger: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.965f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "primaryButtonScale",
    )
    val enabledBrush = if (danger) {
        Brush.horizontalGradient(listOf(Color(0xFFB94F47), AccentCoral, Sand))
    } else {
        Brush.horizontalGradient(
            listOf(
                Cream,
                Sand,
                AccentAmber,
            ),
        )
    }
    val disabledBrush = Brush.horizontalGradient(
        listOf(
            Color.White.copy(alpha = 0.12f),
            Color.White.copy(alpha = 0.07f),
        ),
    )

    Button(
        onClick = onClick,
        enabled = enabled,
        shape = LiquidShape,
        interactionSource = interactionSource,
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp, disabledElevation = 0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = if (danger) Color.White else InkOnGlow,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = Color.White.copy(alpha = 0.56f),
        ),
        modifier = modifier
            .height(52.dp)
            .scale(scale)
            .shadow(10.dp, LiquidShape, clip = false)
            .clip(LiquidShape)
            .background(if (enabled) enabledBrush else disabledBrush)
            .border(
                BorderStroke(
                    1.dp,
                    Brush.horizontalGradient(
                        listOf(
                            Cream.copy(alpha = 0.88f),
                            Sand.copy(alpha = if (danger) 0.30f else 0.78f),
                            AccentAmber.copy(alpha = 0.56f),
                        ),
                    ),
                ),
                LiquidShape,
            ),
    ) {
        Text(text = text, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun LiquidSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.965f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "secondaryButtonScale",
    )
    val enabledBrush = Brush.linearGradient(
        listOf(
            Color(0xFF0A4650),
            Color(0xFF06333C),
            Color(0xFF041F27),
        ),
    )
    val disabledBrush = Brush.linearGradient(
        listOf(
            Color.White.copy(alpha = 0.08f),
            Color.White.copy(alpha = 0.04f),
        ),
    )

    Button(
        onClick = onClick,
        enabled = enabled,
        shape = LiquidShape,
        interactionSource = interactionSource,
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp, disabledElevation = 0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = Cream,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = Color.White.copy(alpha = 0.54f),
        ),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 0.dp),
        modifier = modifier
            .height(48.dp)
            .scale(scale)
            .clip(LiquidShape)
            .background(if (enabled) enabledBrush else disabledBrush)
            .border(
                BorderStroke(
                    1.dp,
                    Brush.horizontalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.72f),
                            Sand.copy(alpha = 0.36f),
                            AccentSky.copy(alpha = 0.20f),
                            Cream.copy(alpha = 0.22f),
                        ),
                    ),
                ),
                LiquidShape,
            ),
    ) {
        Text(text = text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(18.dp, GlassShape, clip = false)
            .clip(GlassShape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xF2073C45),
                        Color(0xF2053039),
                        Color(0xF203242C),
                    ),
                ),
            )
            .border(
                BorderStroke(
                    1.dp,
                    Brush.linearGradient(
                        listOf(
                            Cream.copy(alpha = 0.42f),
                            Sand.copy(alpha = 0.30f),
                            AccentSky.copy(alpha = 0.20f),
                            Cream.copy(alpha = 0.16f),
                        ),
                    ),
                ),
                GlassShape,
            ),
        shape = GlassShape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content,
        )
    }
}

@Composable
private fun GlassBackground(content: @Composable () -> Unit) {
    val transition = rememberInfiniteTransition(label = "travelAmbient")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "ambientDrift",
    )
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "ambientSweep",
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(
                        Color(0xFFB6DED5),
                        Color(0xFF93CEC6),
                        Color(0xFFEBC790),
                        Color(0xFFF1B773),
                    ),
                ),
            )
            drawRect(
                brush = Brush.radialGradient(
                    listOf(
                        Color.White.copy(alpha = 0.36f),
                        Sand.copy(alpha = 0.28f),
                        Color.Transparent,
                    ),
                    center = Offset(w * (0.18f + drift * 0.05f), h * 0.74f),
                    radius = w * 0.78f,
                ),
            )
            drawRect(
                brush = Brush.linearGradient(
                    listOf(
                        Color.Transparent,
                        Cream.copy(alpha = 0.20f),
                        AccentSky.copy(alpha = 0.22f),
                        Color.Transparent,
                    ),
                    start = Offset(w * (0.80f - sweep * 0.16f), -h * 0.04f),
                    end = Offset(w * (0.16f + sweep * 0.16f), h * 0.86f),
                ),
                alpha = 0.78f,
            )

            val firstRibbon = Path().apply {
                moveTo(-w * 0.10f, h * (0.20f + drift * 0.03f))
                cubicTo(w * 0.20f, h * 0.10f, w * 0.52f, h * 0.18f, w * 1.10f, h * 0.04f)
                lineTo(w * 1.18f, h * 0.22f)
                cubicTo(w * 0.62f, h * (0.34f + drift * 0.03f), w * 0.24f, h * 0.28f, -w * 0.18f, h * 0.42f)
                close()
            }
            drawPath(
                path = firstRibbon,
                brush = Brush.linearGradient(
                    listOf(
                        Cream.copy(alpha = 0.04f),
                        Color.White.copy(alpha = 0.20f),
                        AccentSky.copy(alpha = 0.12f),
                        Color.Transparent,
                    ),
                ),
            )

            val secondRibbon = Path().apply {
                moveTo(w * 0.16f, h * 1.08f)
                cubicTo(w * 0.20f, h * 0.80f, w * 0.44f, h * (0.66f - drift * 0.04f), w * 0.78f, h * 0.48f)
                cubicTo(w * 0.94f, h * 0.40f, w * 1.04f, h * 0.20f, w * 1.12f, -h * 0.02f)
                lineTo(w * 0.82f, -h * 0.04f)
                cubicTo(w * 0.70f, h * 0.22f, w * 0.44f, h * 0.48f, w * 0.24f, h * 0.70f)
                cubicTo(w * 0.10f, h * 0.86f, 0f, h, -w * 0.10f, h * 1.12f)
                close()
            }
            drawPath(
                path = secondRibbon,
                brush = Brush.linearGradient(
                    listOf(
                        AccentAmber.copy(alpha = 0.18f),
                        AccentCoral.copy(alpha = 0.16f),
                        Color.White.copy(alpha = 0.16f),
                        Color.Transparent,
                    ),
                ),
            )

            val glassPane = Path().apply {
                moveTo(w * (0.08f + drift * 0.04f), h * 0.05f)
                lineTo(w * (0.64f + drift * 0.03f), h * -0.02f)
                lineTo(w * (0.46f + sweep * 0.04f), h * 0.96f)
                lineTo(w * (0.02f + drift * 0.02f), h * 1.04f)
                close()
            }
            drawPath(
                path = glassPane,
                brush = Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.16f),
                        Color.White.copy(alpha = 0.02f),
                        DeepInk.copy(alpha = 0.08f),
                    ),
                ),
            )

            val hairline = 1.dp.toPx()
            repeat(7) { index ->
                val x = w * (index / 6f)
                drawLine(
                    color = Color.White.copy(alpha = 0.055f),
                    start = Offset(x - w * 0.16f, -h * 0.08f),
                    end = Offset(x + w * 0.10f, h * 1.08f),
                    strokeWidth = hairline,
                )
            }
            repeat(5) { index ->
                val y = h * (0.18f + index * 0.17f)
                drawLine(
                    color = DeepInk.copy(alpha = 0.045f),
                    start = Offset(-w * 0.08f, y + sweep * 18f),
                    end = Offset(w * 1.08f, y - h * 0.10f + drift * 16f),
                    strokeWidth = hairline,
                )
            }
            drawRect(
                color = Cream.copy(alpha = 0.13f),
                topLeft = Offset(w * 0.04f, h * 0.06f),
                size = Size(w * 0.92f, h * 0.88f),
                style = Stroke(width = hairline),
            )
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(
                        Color.Transparent,
                        DeepInk.copy(alpha = 0.10f),
                        DeepInk.copy(alpha = 0.20f),
                    ),
                    startY = h * 0.20f,
                    endY = h,
                ),
            )
        }
        content()
    }
}

@Composable
private fun glassTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Cream,
    unfocusedTextColor = Cream,
    disabledTextColor = Color.White.copy(alpha = 0.56f),
    focusedContainerColor = Color(0xFF073942).copy(alpha = 0.94f),
    unfocusedContainerColor = Color(0xFF052F38).copy(alpha = 0.90f),
    disabledContainerColor = Color(0xFF052F38).copy(alpha = 0.58f),
    focusedBorderColor = Sand.copy(alpha = 0.96f),
    unfocusedBorderColor = Cream.copy(alpha = 0.34f),
    disabledBorderColor = GlassBorder,
    cursorColor = Sand,
    focusedLabelColor = Sand,
    unfocusedLabelColor = Cream.copy(alpha = 0.78f),
    disabledLabelColor = Color.White.copy(alpha = 0.56f),
)

@Composable
private fun GlassDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 8.dp),
        color = Cream.copy(alpha = 0.14f),
    )
}

@Composable
private fun EmptyState(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = Cream.copy(alpha = 0.76f),
    )
}

private fun parseAmountCents(input: String): Long? {
    val normalized = input.trim().replace(",", "")
    if (normalized.isBlank()) return null
    return try {
        val decimal = BigDecimal(normalized)
        if (decimal <= BigDecimal.ZERO) return null
        decimal.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact()
    } catch (_: NumberFormatException) {
        null
    } catch (_: ArithmeticException) {
        null
    }
}

private fun formatMoney(cents: Long): String =
    NumberFormat.getCurrencyInstance().format(cents / 100.0)

private fun expenseAccent(title: String): Color {
    val key = title.lowercase()
    return when {
        "grocer" in key || "suppl" in key -> AccentLime
        "food" in key || "drink" in key -> AccentCoral
        "gas" in key || "transport" in key -> AccentSky
        "rent" in key || "lodg" in key -> AccentViolet
        "activit" in key || "ticket" in key -> AccentAmber
        else -> AccentMint
    }
}

private fun DrawScope.drawPalm(baseX: Float, baseY: Float, palmHeight: Float) {
    val top = Offset(baseX + palmHeight * 0.08f, baseY - palmHeight)
    val trunk = Path().apply {
        moveTo(baseX - palmHeight * 0.025f, baseY)
        cubicTo(baseX + palmHeight * 0.04f, baseY - palmHeight * 0.34f, baseX + palmHeight * 0.02f, baseY - palmHeight * 0.68f, top.x, top.y)
        lineTo(top.x + palmHeight * 0.035f, top.y)
        cubicTo(baseX + palmHeight * 0.08f, baseY - palmHeight * 0.66f, baseX + palmHeight * 0.07f, baseY - palmHeight * 0.30f, baseX + palmHeight * 0.025f, baseY)
        close()
    }
    drawPath(trunk, color = PalmInk.copy(alpha = 0.88f))

    val leaves = listOf(
        Offset(-0.40f, -0.10f),
        Offset(-0.30f, -0.28f),
        Offset(-0.08f, -0.36f),
        Offset(0.20f, -0.34f),
        Offset(0.38f, -0.18f),
        Offset(0.34f, 0.02f),
        Offset(-0.28f, 0.04f),
    )
    leaves.forEach { leaf ->
        val end = Offset(top.x + palmHeight * leaf.x, top.y + palmHeight * leaf.y)
        drawLine(
            color = PalmInk.copy(alpha = 0.88f),
            start = top,
            end = end,
            strokeWidth = 3.dp.toPx(),
        )
    }
}

private fun DrawScope.drawPlane(centerX: Float, centerY: Float, scale: Float) {
    val unit = 22.dp.toPx() * scale
    val plane = Path().apply {
        moveTo(centerX - unit * 1.25f, centerY)
        lineTo(centerX + unit * 1.18f, centerY - unit * 0.16f)
        lineTo(centerX + unit * 0.84f, centerY + unit * 0.08f)
        lineTo(centerX - unit * 1.25f, centerY)
        close()
    }
    val wing = Path().apply {
        moveTo(centerX - unit * 0.22f, centerY)
        lineTo(centerX + unit * 0.18f, centerY + unit * 0.50f)
        lineTo(centerX + unit * 0.34f, centerY + unit * 0.04f)
        close()
    }
    drawPath(plane, color = Cream.copy(alpha = 0.86f))
    drawPath(wing, color = DeepInk.copy(alpha = 0.32f))
    drawLine(
        color = Cream.copy(alpha = 0.50f),
        start = Offset(centerX - unit * 1.45f, centerY + unit * 0.16f),
        end = Offset(centerX - unit * 2.20f, centerY + unit * 0.40f),
        strokeWidth = 1.dp.toPx(),
    )
}

private fun newId(prefix: String): String =
    "${prefix}_${System.currentTimeMillis()}_${Random.nextInt(1000, 9999)}"

private fun randomCode(existingCodes: Set<String>): String {
    repeat(100) {
        val code = buildString {
            repeat(6) {
                append(CODE_CHARS[Random.nextInt(CODE_CHARS.length)])
            }
        }
        if (code !in existingCodes) return code
    }
    return newId("code").takeLast(6).uppercase()
}

private fun toast(context: android.content.Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

private const val CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
