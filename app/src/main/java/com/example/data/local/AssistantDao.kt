package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AssistantChatMessage
import com.example.data.model.CommunicationLog
import com.example.data.model.ContactRecord
import com.example.data.model.EmailSummaryItem
import com.example.data.model.ReminderItem
import com.example.data.model.SaharanpurNewsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AssistantDao {
    // Chat messages
    @Query("SELECT * FROM assistant_messages ORDER BY timestamp ASC")
    fun getAllChatMessages(): Flow<List<AssistantChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(msg: AssistantChatMessage): Long

    @Query("DELETE FROM assistant_messages")
    suspend fun clearChatMessages()

    // Communications (Calls, SMS, WhatsApp, Telegram)
    @Query("SELECT * FROM communication_logs ORDER BY timestamp DESC")
    fun getAllCommunicationLogs(): Flow<List<CommunicationLog>>

    @Query("SELECT * FROM communication_logs WHERE LOWER(contactName) LIKE '%' || LOWER(:query) || '%' OR LOWER(content) LIKE '%' || LOWER(:query) || '%' ORDER BY timestamp DESC")
    fun searchLogs(query: String): Flow<List<CommunicationLog>>

    @Query("SELECT * FROM communication_logs WHERE LOWER(contactName) = LOWER(:contactName) ORDER BY timestamp DESC")
    fun getLogsByContact(contactName: String): Flow<List<CommunicationLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommunicationLog(log: CommunicationLog): Long

    @Query("DELETE FROM communication_logs WHERE id = :id")
    suspend fun deleteCommunicationLog(id: Long)

    // Reminders
    @Query("SELECT * FROM reminders ORDER BY isCompleted ASC, timeInMillis ASC, id DESC")
    fun getAllReminders(): Flow<List<ReminderItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderItem): Long

    @Update
    suspend fun updateReminder(reminder: ReminderItem)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteReminder(id: Long)

    // Contacts
    @Query("SELECT * FROM contacts ORDER BY name ASC")
    fun getAllContacts(): Flow<List<ContactRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: ContactRecord): Long

    @Update
    suspend fun updateContact(contact: ContactRecord)

    @Query("DELETE FROM contacts WHERE id = :id")
    suspend fun deleteContact(id: Long)

    // Emails
    @Query("SELECT * FROM email_summaries ORDER BY timestamp DESC")
    fun getAllEmails(): Flow<List<EmailSummaryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmail(email: EmailSummaryItem): Long

    @Query("UPDATE email_summaries SET isRead = 1 WHERE id = :id")
    suspend fun markEmailAsRead(id: Long)

    // Saharanpur News & Facebook Publishing
    @Query("SELECT * FROM saharanpur_news ORDER BY timestamp DESC")
    fun getAllSaharanpurNews(): Flow<List<SaharanpurNewsEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaharanpurNews(news: SaharanpurNewsEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSaharanpurNews(news: List<SaharanpurNewsEntity>)

    @Update
    suspend fun updateSaharanpurNews(news: SaharanpurNewsEntity)

    @Query("DELETE FROM saharanpur_news WHERE id = :id")
    suspend fun deleteSaharanpurNews(id: Long)

    @Query("DELETE FROM saharanpur_news")
    suspend fun clearAllSaharanpurNews()
}
