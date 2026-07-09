package com.tripsplit.settlement

import com.tripsplit.data.Trip
import kotlin.math.min

data class MemberBalance(
    val memberId: String,
    val balanceCents: Long,
)

data class Transfer(
    val fromMemberId: String,
    val toMemberId: String,
    val amountCents: Long,
)

object SettlementCalculator {
    fun memberBalances(trip: Trip): List<MemberBalance> {
        val balances = trip.members.associate { it.id to 0L }.toMutableMap()

        trip.expenses.forEach { expense ->
            val participants = expense.participantIds.filter { balances.containsKey(it) }
            if (participants.isEmpty() || !balances.containsKey(expense.payerId) || expense.amountCents <= 0L) {
                return@forEach
            }

            balances[expense.payerId] = balances.getValue(expense.payerId) + expense.amountCents

            val baseShare = expense.amountCents / participants.size
            var remainder = expense.amountCents % participants.size
            participants.forEach { participantId ->
                val share = baseShare + if (remainder > 0L) {
                    remainder -= 1L
                    1L
                } else {
                    0L
                }
                balances[participantId] = balances.getValue(participantId) - share
            }
        }

        return trip.members.map { member ->
            MemberBalance(memberId = member.id, balanceCents = balances[member.id] ?: 0L)
        }
    }

    fun transfers(trip: Trip): List<Transfer> {
        val debtors = memberBalances(trip)
            .filter { it.balanceCents < 0L }
            .map { BalanceBucket(memberId = it.memberId, amountCents = -it.balanceCents) }
            .sortedByDescending { it.amountCents }
            .toMutableList()
        val creditors = memberBalances(trip)
            .filter { it.balanceCents > 0L }
            .map { BalanceBucket(memberId = it.memberId, amountCents = it.balanceCents) }
            .sortedByDescending { it.amountCents }
            .toMutableList()

        val transfers = mutableListOf<Transfer>()
        var debtorIndex = 0
        var creditorIndex = 0

        while (debtorIndex < debtors.size && creditorIndex < creditors.size) {
            val debtor = debtors[debtorIndex]
            val creditor = creditors[creditorIndex]
            val amount = min(debtor.amountCents, creditor.amountCents)

            if (amount > 0L) {
                transfers += Transfer(
                    fromMemberId = debtor.memberId,
                    toMemberId = creditor.memberId,
                    amountCents = amount,
                )
            }

            debtors[debtorIndex] = debtor.copy(amountCents = debtor.amountCents - amount)
            creditors[creditorIndex] = creditor.copy(amountCents = creditor.amountCents - amount)

            if (debtors[debtorIndex].amountCents == 0L) debtorIndex += 1
            if (creditors[creditorIndex].amountCents == 0L) creditorIndex += 1
        }

        return transfers
    }

    private data class BalanceBucket(
        val memberId: String,
        val amountCents: Long,
    )
}
