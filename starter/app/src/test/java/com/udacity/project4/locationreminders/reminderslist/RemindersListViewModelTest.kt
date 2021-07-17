package com.udacity.project4.locationreminders.reminderslist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Rule
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RemindersListViewModelTest {
    //TODO: provide testing to the RemindersListViewModel and its live data objects

    private lateinit var viewModel: RemindersListViewModel
    private lateinit var dataSource: FakeDataSource

    private val reminder1 = ReminderDataItem("Reminder1", "Description1", "Location1", 10.0, 10.0)
    private val reminder2 = ReminderDataItem("Reminder2", "Description2", "Location2", 20.0, 20.0)

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

}