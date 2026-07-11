package com.tripsplit.settlement

import com.tripsplit.data.Expense
import com.tripsplit.data.ExpenseItem
import com.tripsplit.data.Member
import com.tripsplit.data.Trip
import org.junit.Assert.assertEquals
import org.junit.Test

class SettlementCalculatorTest {
    @Test
    fun selectedPeopleOnlyOweForExpensesTheyUsed() {
        val trip = Trip(
            id = "trip",
            name = "Beach",
            code = "ABC123",
            createdAt = 0L,
            members = listOf(
                Member("a", "A", isAdmin = true),
                Member("b", "B"),
                Member("c", "C"),
            ),
            expenses = listOf(
                Expense(
                    id = "groceries",
                    title = "Groceries",
                    amountCents = 9_000L,
                    payerId = "a",
                    participantIds = listOf("a", "b", "c"),
                    createdAt = 1L,
                ),
                Expense(
                    id = "kayak",
                    title = "Kayak",
                    amountCents = 6_000L,
                    payerId = "b",
                    participantIds = listOf("b", "c"),
                    createdAt = 2L,
                ),
            ),
        )

        assertEquals(
            listOf(
                MemberBalance("a", 6_000L),
                MemberBalance("b", 0L),
                MemberBalance("c", -6_000L),
            ),
            SettlementCalculator.memberBalances(trip),
        )
        assertEquals(
            listOf(Transfer(fromMemberId = "c", toMemberId = "a", amountCents = 6_000L)),
            SettlementCalculator.transfers(trip),
        )
    }

    @Test
    fun unevenCentsAreDistributedWithoutLosingMoney() {
        val trip = Trip(
            id = "trip",
            name = "Weekend",
            code = "DEF456",
            createdAt = 0L,
            members = listOf(
                Member("a", "A", isAdmin = true),
                Member("b", "B"),
                Member("c", "C"),
            ),
            expenses = listOf(
                Expense(
                    id = "snacks",
                    title = "Snacks",
                    amountCents = 100L,
                    payerId = "a",
                    participantIds = listOf("a", "b", "c"),
                    createdAt = 1L,
                ),
            ),
        )

        assertEquals(
            listOf(
                MemberBalance("a", 66L),
                MemberBalance("b", -33L),
                MemberBalance("c", -33L),
            ),
            SettlementCalculator.memberBalances(trip),
        )
        assertEquals(100L, trip.expenses.sumOf { it.amountCents })
    }

    @Test
    fun payerDoesNotNeedToBeIncludedInParticipants() {
        val trip = Trip(
            id = "trip",
            name = "Beach",
            code = "GHI789",
            createdAt = 0L,
            members = listOf(
                Member("admin", "Admin", isAdmin = true),
                Member("a", "A"),
                Member("b", "B"),
            ),
            expenses = listOf(
                Expense(
                    id = "kayak",
                    title = "Kayak",
                    amountCents = 4_000L,
                    payerId = "a",
                    participantIds = listOf("b"),
                    createdAt = 1L,
                ),
            ),
        )

        assertEquals(
            listOf(
                MemberBalance("admin", 0L),
                MemberBalance("a", 4_000L),
                MemberBalance("b", -4_000L),
            ),
            SettlementCalculator.memberBalances(trip),
        )
        assertEquals(
            listOf(Transfer(fromMemberId = "b", toMemberId = "a", amountCents = 4_000L)),
            SettlementCalculator.transfers(trip),
        )
    }

    @Test
    fun itemizedReceiptAssignsPrivateItemAndSharesTheRest() {
        val members = listOf(
            Member("payer", "Payer"),
            Member("x", "X"),
            Member("b", "B"),
            Member("c", "C"),
            Member("d", "D"),
            Member("e", "E"),
        )
        val groupIds = members.map { it.id }
        val trip = Trip(
            id = "trip",
            name = "Groceries",
            code = "ITEM01",
            createdAt = 0L,
            members = members,
            expenses = listOf(
                Expense(
                    id = "receipt",
                    title = "Groceries",
                    amountCents = 300_000L,
                    payerId = "payer",
                    participantIds = groupIds,
                    createdAt = 1L,
                    items = listOf(
                        ExpenseItem("shared", "Shared groceries", 290_000L, groupIds),
                        ExpenseItem("redbull", "Redbull", 10_000L, listOf("x")),
                    ),
                ),
            ),
        )

        assertEquals(
            listOf(
                MemberBalance("payer", 251_666L),
                MemberBalance("x", -58_334L),
                MemberBalance("b", -48_333L),
                MemberBalance("c", -48_333L),
                MemberBalance("d", -48_333L),
                MemberBalance("e", -48_333L),
            ),
            SettlementCalculator.memberBalances(trip),
        )
    }
}
