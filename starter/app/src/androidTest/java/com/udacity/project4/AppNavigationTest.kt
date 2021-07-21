package com.udacity.project4

import android.os.Bundle
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.ReminderListFragment
import com.udacity.project4.locationreminders.reminderslist.ReminderListFragmentDirections
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderFragment
import com.udacity.project4.locationreminders.savereminder.SaveReminderFragmentDirections
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.hamcrest.core.Is
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
@LargeTest
class AppNavigationTest : KoinTest {

    private lateinit var dataSource: ReminderDataSource
    private lateinit var viewModel: SaveReminderViewModel

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
                SaveReminderViewModel(ApplicationProvider.getApplicationContext(), get() as ReminderDataSource)
            }

            // This needs to be manually casted (even though it seems useless)
            single { RemindersLocalRepository(get()) as ReminderDataSource}
            single { LocalDB.createRemindersDao(ApplicationProvider.getApplicationContext()) }
        }

        startKoin {
            androidContext(ApplicationProvider.getApplicationContext())
            modules(listOf(myModule))
        }

        dataSource = get()

        viewModel = SaveReminderViewModel(ApplicationProvider.getApplicationContext(), dataSource)

        runBlocking {
            dataSource.deleteAllReminders()
        }
    }

    private fun DataBindingIdlingResource.monitorSaveReminderFragment(fragmentScenario: FragmentScenario<SaveReminderFragment>) {
        fragmentScenario.onFragment { fragment ->
            activity = fragment.requireActivity()
        }
    }

    @Test
    fun addReminder_addLocationFromMap_savesReminder() {
        val navController = mock(NavController::class.java)
        val scenario =
            launchFragmentInContainer<SaveReminderFragment>(Bundle.EMPTY, R.style.AppTheme)

        dataBindingIdlingResource.monitorSaveReminderFragment(scenario)

        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }

        // WHEN adding title and description
        onView(withId(R.id.reminderTitle)).perform(typeText("title"))
        onView(withId(R.id.reminderDescription)).perform(typeText("description"))
        // Clicking on the add Fab navigates us to SaveReminderFragment
        onView(withId(R.id.selectLocation)).perform(click())
        verify(navController).navigate(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        // perform long click to add marker
        onView(withId(R.id.map)).perform(longClick())

        onView(withId(R.id.save_button)).perform(click())
        // Go back to SaveReminderFragment and click to save reminder
        onView(withId(R.id.saveReminder)).perform(click())
        assertThat(viewModel.showToast.value, Is.`is`("Reminder Saved !"))
    }
}