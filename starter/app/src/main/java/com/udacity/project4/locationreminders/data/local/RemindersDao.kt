package com.udacity.project4.locationreminders.data.local

import androidx.room.*
import com.udacity.project4.locationreminders.data.dto.ReminderDTO

/**
 * Data Access Object for the reminders table.
 */
@Dao
interface RemindersDao {
    /**
     * @return all reminders.
     */
    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("SELECT * FROM reminders")
    suspend fun getReminders(): List<ReminderDTO>

    /**
     * @param reminderId the id of the reminder
     * @return the reminder object with the reminderId
     */
    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("SELECT * FROM reminders where entry_id = :reminderId")
    suspend fun getReminderById(reminderId: String): ReminderDTO?

    /**
     * Insert a reminder in the database. If the reminder already exists, replace it.
     *
     * @param reminder the reminder to be inserted.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveReminder(reminder: ReminderDTO)

    /**
     * Delete all reminders.
     */
    @Query("DELETE FROM reminders")
    suspend fun deleteAllReminders()

}