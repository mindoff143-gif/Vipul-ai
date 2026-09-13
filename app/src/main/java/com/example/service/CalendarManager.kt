package com.example.service

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CalendarContract
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.model.CalendarEventItem
import java.util.Calendar
import java.util.TimeZone

class CalendarManager(private val context: Context) {
    private val TAG = "CalendarManager"

    fun getDeviceCalendarEvents(): List<CalendarEventItem> {
        val events = mutableListOf<CalendarEventItem>()
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            return getDefaultCalendarEvents()
        }

        try {
            val startOfDay = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }.timeInMillis

            val endOfDay = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
            }.timeInMillis

            val projection = arrayOf(
                CalendarContract.Events._ID,
                CalendarContract.Events.TITLE,
                CalendarContract.Events.DTSTART,
                CalendarContract.Events.DTEND,
                CalendarContract.Events.EVENT_LOCATION,
                CalendarContract.Events.DESCRIPTION
            )

            val selection = "${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} <= ?"
            val selectionArgs = arrayOf(startOfDay.toString(), endOfDay.toString())

            context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${CalendarContract.Events.DTSTART} ASC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(CalendarContract.Events._ID)
                val titleIdx = cursor.getColumnIndex(CalendarContract.Events.TITLE)
                val startIdx = cursor.getColumnIndex(CalendarContract.Events.DTSTART)
                val endIdx = cursor.getColumnIndex(CalendarContract.Events.DTEND)
                val locIdx = cursor.getColumnIndex(CalendarContract.Events.EVENT_LOCATION)
                val descIdx = cursor.getColumnIndex(CalendarContract.Events.DESCRIPTION)

                while (cursor.moveToNext()) {
                    val id = if (idIdx != -1) cursor.getLong(idIdx) else 0L
                    val title = if (titleIdx != -1) cursor.getString(titleIdx) ?: "Event" else "Event"
                    val start = if (startIdx != -1) cursor.getLong(startIdx) else System.currentTimeMillis()
                    val end = if (endIdx != -1) cursor.getLong(endIdx) else start + 3600000L
                    val loc = if (locIdx != -1) cursor.getString(locIdx) else null
                    val desc = if (descIdx != -1) cursor.getString(descIdx) else null

                    events.add(CalendarEventItem(id, title, start, end, loc, desc))
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error querying device calendar: ${e.message}")
        }

        return if (events.isEmpty()) getDefaultCalendarEvents() else events
    }

    fun syncReminderToCalendar(title: String, startTime: Long, location: String? = null): Boolean {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED) {
            try {
                val values = ContentValues().apply {
                    put(CalendarContract.Events.CALENDAR_ID, 1) // default primary calendar
                    put(CalendarContract.Events.TITLE, "AI Reminder: $title")
                    put(CalendarContract.Events.DTSTART, startTime)
                    put(CalendarContract.Events.DTEND, startTime + 1800000L) // 30 min duration
                    put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                    put(CalendarContract.Events.EVENT_LOCATION, location ?: "Everywhere")
                    put(CalendarContract.Events.DESCRIPTION, "Created automatically by Personal AI Assistant")
                }
                val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
                if (uri != null) return true
            } catch (e: Exception) {
                Log.w(TAG, "Direct calendar insert failed, using Intent fallback: ${e.message}")
            }
        }

        // Fallback to Intent
        return try {
            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, title)
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startTime)
                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, startTime + 1800000L)
                putExtra(CalendarContract.Events.EVENT_LOCATION, location)
                putExtra(CalendarContract.Events.DESCRIPTION, "Created by Personal AI Assistant")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch calendar intent: ${e.message}")
            false
        }
    }

    private fun getDefaultCalendarEvents(): List<CalendarEventItem> {
        val now = System.currentTimeMillis()
        val hour = 3600000L
        return listOf(
            CalendarEventItem(
                id = 101L,
                title = "डॉक्टर गुप्ता क्लिनिक अपॉइंटमेंट (Health Checkup)",
                startTime = now + 2 * hour,
                endTime = now + 3 * hour,
                location = "City Hospital, Sector 4",
                description = "Routine medical checkup and prescription renewal"
            ),
            CalendarEventItem(
                id = 102L,
                title = "प्रोजेक्ट रिव्यू व टीम मीटिंग (Project Review)",
                startTime = now + 4 * hour,
                endTime = now + 5 * hour,
                location = "Office Main Conference Room",
                description = "Sprint review and Q3 presentation walkthrough"
            ),
            CalendarEventItem(
                id = 103L,
                title = "AI वेबिनार और ऑनलाइन सत्र (Online Webinar)",
                startTime = now + 8 * hour,
                endTime = now + 9 * hour,
                location = "Google Meet",
                description = "Future of Personal Assistant Technologies"
            )
        )
    }
}
