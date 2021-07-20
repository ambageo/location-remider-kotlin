package com.udacity.project4.locationreminders.reminderslist


import android.os.Bundle
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.Fragment
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.monitorFragment

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.not
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel

import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.get

import org.mockito.Mockito.*

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
class ReminderListFragmentTest: KoinTest {

    private lateinit var dataSource: ReminderDataSource
    private val dataBindingIdlingResource = DataBindingIdlingResource()

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Before
    fun initializeRepository() {
        stopKoin()
        /**
         * Use Koin as a Service Locator
         * */
        val myModule = module {
            viewModel {
                RemindersListViewModel(getApplicationContext(), get() as ReminderDataSource)
            }

            // This needs to be manually casted (even though it seems useless)
            single { RemindersLocalRepository(get()) as ReminderDataSource}
            single { LocalDB.createRemindersDao(getApplicationContext()) }
        }

        startKoin {
            androidContext(getApplicationContext())
            modules(listOf(myModule))
        }

        dataSource = get()

        runBlocking {
            dataSource.deleteAllReminders()
        }
    }

    //    TODO: test the displayed data on the UI.
    @Test
    fun showRemindersInUI() = runBlockingTest {
        val reminder = ReminderDTO("title", "description", "location", 10.0, 10.0)
        // GIVEN a reminder
        runBlocking {
            dataSource.saveReminder(reminder)
        }

        // WHEN Launching the fragment
        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle.EMPTY, R.style.AppTheme)
        val navController = mock(NavController::class.java)
        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }
        dataBindingIdlingResource.monitorFragment(scenario)



        // THEN the reminder is displayed
        onView(withId(R.id.noDataTextView)).check(matches(not(isDisplayed())))
        onView(withId(R.id.reminderTitle)).check(matches(withText("title")))
        onView(withId(R.id.reminderDescription)).check(matches(withText("description")))
        onView(withId(R.id.selectLocation)).check(matches(withText("location")))

    }

    @Test
    fun noReminders_showsEmptyData() {
        // WHEN Launching the fragment
        launchFragmentInContainer<ReminderListFragment>(Bundle.EMPTY, R.style.AppTheme)
        // THEN no data textview is displayed
        onView(withId(R.id.noDataTextView)).check(matches(isDisplayed()))
    }

    //    TODO: test the navigation of the fragments //DONE
    @Test
    fun clickOnFab_navigatesToSaveReminderFragment() {
        val scenario =
            launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        val navController = mock(NavController::class.java)

        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }

        // WHEN clicking on the fab
        onView(withId(R.id.addReminderFAB)).perform(click())
        // THEN we navigate to SaveReminderFragment
        verify(navController).navigate(ReminderListFragmentDirections.toSaveReminder())
    }

}




//    TODO: add testing for the error messages.
