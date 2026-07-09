package com.tripsplit.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class TripStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("trip_split_store", Context.MODE_PRIVATE)

    fun loadTrips(): List<Trip> {
        val raw = prefs.getString(TRIPS_KEY, "[]").orEmpty()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    add(array.getJSONObject(index).toTrip())
                }
            }
        } catch (_: JSONException) {
            emptyList()
        }
    }

    fun saveTrips(trips: List<Trip>) {
        val array = JSONArray()
        trips.forEach { array.put(it.toJson()) }
        prefs.edit().putString(TRIPS_KEY, array.toString()).apply()
    }

    private fun JSONObject.toTrip(): Trip =
        Trip(
            id = optString("id"),
            name = optString("name", "Trip"),
            code = optString("code"),
            createdAt = optLong("createdAt", System.currentTimeMillis()),
            isEnded = optBoolean("isEnded", false),
            members = optJSONArray("members").toMembers(),
            expenses = optJSONArray("expenses").toExpenses(),
        )

    private fun JSONObject.toMember(): Member =
        Member(
            id = optString("id"),
            name = optString("name", "Guest"),
            isAdmin = optBoolean("isAdmin", false),
        )

    private fun JSONObject.toExpense(): Expense =
        Expense(
            id = optString("id"),
            title = optString("title", "Expense"),
            amountCents = optLong("amountCents", 0L),
            payerId = optString("payerId"),
            participantIds = optJSONArray("participantIds").toStrings(),
            createdAt = optLong("createdAt", System.currentTimeMillis()),
        )

    private fun JSONArray?.toMembers(): List<Member> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) {
                add(getJSONObject(index).toMember())
            }
        }
    }

    private fun JSONArray?.toExpenses(): List<Expense> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) {
                add(getJSONObject(index).toExpense())
            }
        }
    }

    private fun JSONArray?.toStrings(): List<String> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) {
                add(getString(index))
            }
        }
    }

    private fun Trip.toJson(): JSONObject =
        JSONObject()
            .put("id", id)
            .put("name", name)
            .put("code", code)
            .put("createdAt", createdAt)
            .put("isEnded", isEnded)
            .put("members", JSONArray().apply { members.forEach { put(it.toJson()) } })
            .put("expenses", JSONArray().apply { expenses.forEach { put(it.toJson()) } })

    private fun Member.toJson(): JSONObject =
        JSONObject()
            .put("id", id)
            .put("name", name)
            .put("isAdmin", isAdmin)

    private fun Expense.toJson(): JSONObject =
        JSONObject()
            .put("id", id)
            .put("title", title)
            .put("amountCents", amountCents)
            .put("payerId", payerId)
            .put("participantIds", JSONArray().apply { participantIds.forEach { put(it) } })
            .put("createdAt", createdAt)

    private companion object {
        const val TRIPS_KEY = "trips"
    }
}
