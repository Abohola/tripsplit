package com.tripsplit.data

data class Trip(
    val id: String,
    val name: String,
    val code: String,
    val createdAt: Long,
    val isEnded: Boolean = false,
    val members: List<Member> = emptyList(),
    val expenses: List<Expense> = emptyList(),
)

data class Member(
    val id: String,
    val name: String,
    val isAdmin: Boolean = false,
)

data class Expense(
    val id: String,
    val title: String,
    val amountCents: Long,
    val payerId: String,
    val participantIds: List<String>,
    val createdAt: Long,
    val items: List<ExpenseItem> = emptyList(),
)

data class ExpenseItem(
    val id: String,
    val name: String,
    val amountCents: Long,
    val participantIds: List<String>,
)

fun Trip.memberName(memberId: String): String =
    members.firstOrNull { it.id == memberId }?.name ?: "Unknown"
