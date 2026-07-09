package com.tripsplit.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
private val GlassPanel = Color.White.copy(alpha = 0.10f)
private val GlassPanelStrong = Color.White.copy(alpha = 0.14f)
private val GlassBorder = Color.White.copy(alpha = 0.18f)
private val GlassBorderBright = Color.White.copy(alpha = 0.30f)
private val AccentMint = Color(0xFF52E0C4)
private val AccentIndigo = Color(0xFF9FA9FF)
private val AccentAmber = Color(0xFFFFB45E)

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

    val handleAddExpense: (String, Long, Set<String>) -> Unit = { rawTitle, amountCents, participantIds ->
        val trip = currentTrip
        val payer = currentMember
        val title = rawTitle.trim()
        when {
            trip == null || payer == null -> Unit
            trip.isEnded -> toast(context, "This trip has ended")
            title.isBlank() -> toast(context, "Add what was paid for")
            amountCents <= 0L -> toast(context, "Add an amount")
            participantIds.isEmpty() -> toast(context, "Choose who used it")
            else -> {
                val expense = Expense(
                    id = newId("expense"),
                    title = title,
                    amountCents = amountCents,
                    payerId = payer.id,
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
                Text(
                    text = "TripSplit",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
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
                    Button(
                        onClick = { onCreateTrip(tripName, adminName) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Create")
                    }
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
                    OutlinedButton(
                        onClick = { onJoinTrip(inviteCode, guestName) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Join")
                    }
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
                                OutlinedButton(onClick = { onOpenTrip(trip.id) }) {
                                    Text("Open")
                                }
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
    onAddExpense: (String, Long, Set<String>) -> Unit,
    onAddGuest: (String) -> Unit,
    onPromote: (String) -> Unit,
    onEndTrip: () -> Unit,
) {
    var selectedTab by rememberSaveable(trip.id) { mutableStateOf(0) }
    val tabs = listOf("Expenses", "Summary", "Admin")

    GlassBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                        titleContentColor = Color.White,
                        actionIconContentColor = Color.White,
                    ),
                    title = {
                        Column {
                            Text(trip.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("Code ${trip.code}", style = MaterialTheme.typography.labelMedium)
                        }
                    },
                    actions = {
                        TextButton(onClick = onShowEntry) {
                            Text("Trips")
                        }
                    },
                )
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                TripStatusHeader(
                    trip = trip,
                    currentMember = currentMember,
                    onSwitchMember = onSwitchMember,
                )
                PrimaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = GlassPanelStrong,
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .border(BorderStroke(1.dp, GlassBorder), GlassShape),
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) },
                        )
                    }
                }
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
            Column(modifier = Modifier.weight(1f)) {
                Text("Acting as", style = MaterialTheme.typography.labelMedium)
                MemberPicker(
                    members = trip.members,
                    currentMember = currentMember,
                    onSwitchMember = onSwitchMember,
                )
            }
            Spacer(Modifier.width(12.dp))
            AssistChip(
                onClick = {},
                label = { Text(if (trip.isEnded) "Ended" else "Active") },
            )
        }
    }
}

@Composable
private fun MemberPicker(
    members: List<Member>,
    currentMember: Member,
    onSwitchMember: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text(currentMember.name + if (currentMember.isAdmin) "  Admin" else "")
        }
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
private fun ExpensesTab(
    trip: Trip,
    currentMember: Member,
    onAddExpense: (String, Long, Set<String>) -> Unit,
) {
    val context = LocalContext.current
    var title by rememberSaveable(trip.id) { mutableStateOf("") }
    var amount by rememberSaveable(trip.id) { mutableStateOf("") }
    var selectedParticipantIds by remember(trip.id, trip.members.map { it.id }) {
        mutableStateOf(trip.members.map { it.id }.toSet())
    }

    LaunchedEffect(trip.members) {
        val memberIds = trip.members.map { it.id }.toSet()
        selectedParticipantIds = selectedParticipantIds.intersect(memberIds).ifEmpty { memberIds }
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
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("What was paid for") },
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
            Text("Paid by ${currentMember.name}", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            PeopleDropdown(
                members = trip.members,
                selectedIds = selectedParticipantIds,
                enabled = !trip.isEnded,
                onSelectionChange = { selectedParticipantIds = it },
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    val cents = parseAmountCents(amount)
                    if (cents == null) {
                        toast(context, "Enter a valid amount")
                    } else {
                        onAddExpense(title, cents, selectedParticipantIds)
                        title = ""
                        amount = ""
                        selectedParticipantIds = trip.members.map { it.id }.toSet()
                    }
                },
                enabled = !trip.isEnded,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Add expense")
            }
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
        OutlinedButton(
            onClick = { expanded = true },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(label)
        }
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
private fun ExpenseCard(trip: Trip, expense: Expense) {
    SectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(expense.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
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
            Spacer(Modifier.width(12.dp))
            Text(formatMoney(expense.amountCents), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
    Column {
        Text("$from pays $to", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(formatMoney(amount), style = MaterialTheme.typography.bodyLarge)
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
            OutlinedButton(
                onClick = {
                    clipboard.setText(AnnotatedString(trip.code))
                    toast(context, "Code copied")
                },
                enabled = !trip.isEnded,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Copy code")
            }
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
            Button(
                onClick = {
                    onAddGuest(guestName)
                    guestName = ""
                },
                enabled = !trip.isEnded,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Add guest")
            }
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
                        OutlinedButton(
                            onClick = { onPromote(member.id) },
                            enabled = !trip.isEnded,
                        ) {
                            Text("Promote")
                        }
                    }
                }
            }
        }

        Button(
            onClick = { confirmEnd = true },
            enabled = !trip.isEnded,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (trip.isEnded) "Trip ended" else "End trip")
        }
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
private fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = GlassShape,
        colors = CardDefaults.cardColors(containerColor = GlassPanel),
        border = BorderStroke(1.dp, GlassBorder),
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF020409),
                        Color(0xFF071017),
                        Color(0xFF120F1C),
                        Color(0xFF05070B),
                    ),
                ),
            ),
    ) {
        Image(
            painter = painterResource(id = R.drawable.trip_glass_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = 0.42f,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xAA020409),
                            Color(0xCC071017),
                            Color(0xEE05070B),
                        ),
                    ),
                ),
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            val hairline = 1.dp.toPx()
            drawLine(
                color = AccentMint.copy(alpha = 0.22f),
                start = Offset(-size.width * 0.10f, size.height * 0.18f),
                end = Offset(size.width * 0.92f, size.height * 0.04f),
                strokeWidth = hairline,
            )
            drawLine(
                color = AccentIndigo.copy(alpha = 0.18f),
                start = Offset(size.width * 0.06f, size.height * 0.88f),
                end = Offset(size.width * 1.04f, size.height * 0.66f),
                strokeWidth = hairline,
            )
            drawLine(
                color = AccentAmber.copy(alpha = 0.16f),
                start = Offset(size.width * 0.72f, -size.height * 0.05f),
                end = Offset(size.width * 0.98f, size.height * 0.46f),
                strokeWidth = hairline,
            )
            drawRect(
                color = Color.White.copy(alpha = 0.035f),
                topLeft = Offset(size.width * 0.05f, size.height * 0.08f),
                size = androidx.compose.ui.geometry.Size(size.width * 0.90f, size.height * 0.84f),
                style = Stroke(width = hairline),
            )
        }
        content()
    }
}

@Composable
private fun glassTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
    focusedContainerColor = Color.White.copy(alpha = 0.09f),
    unfocusedContainerColor = Color.White.copy(alpha = 0.06f),
    disabledContainerColor = Color.White.copy(alpha = 0.035f),
    focusedBorderColor = AccentMint.copy(alpha = 0.86f),
    unfocusedBorderColor = GlassBorderBright,
    disabledBorderColor = GlassBorder,
    cursorColor = AccentMint,
    focusedLabelColor = AccentMint,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
)

@Composable
private fun GlassDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 8.dp),
        color = Color.White.copy(alpha = 0.12f),
    )
}

@Composable
private fun EmptyState(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
