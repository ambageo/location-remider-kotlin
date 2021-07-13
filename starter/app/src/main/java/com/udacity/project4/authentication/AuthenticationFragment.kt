package com.udacity.project4.authentication

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.udacity.project4.R
import com.udacity.project4.databinding.FragmentAuthenticationBinding


class AuthenticationFragment : Fragment() {

    private lateinit var registerForActivityResult: ActivityResultLauncher<Intent>
    private lateinit var binding: FragmentAuthenticationBinding

    private val viewModel by viewModels<AuthenticationViewModel>()

    companion object {
        private val TAG = AuthenticationFragment::class.java.simpleName
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding= DataBindingUtil.inflate(inflater, R.layout.fragment_authentication, container, false)
        Log.d(TAG, "onCreateView")
        registerForSignInResult()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated")

        binding.loginButton.setOnClickListener {
            // TODO: Implement the create account and sign in using FirebaseUI, use sign in using email and sign in using Google //DONE
            Log.d(TAG, "Clicked to log in")
            launchSignInFlow()
        }

        // TODO: If the user was authenticated, send him to RemindersActivity //DONE
        viewModel.authenticationState.observe(viewLifecycleOwner, Observer { authenticationState ->
            when(authenticationState) {
                AuthenticationViewModel.AuthenticationState.AUTHENTICATED ->
                    findNavController().navigate(R.id.action_authenticationFragment_to_reminderListFragment)
            }
        })
    }

    /**
     * Registers a launcher used to start the process of executing an ActivityResultContract
     * so that we can listen to the result of the sign - in process
     * This must be done in onCreate() or on Attach, ie before the fragment is created
     */
    private fun registerForSignInResult() {
        registerForActivityResult = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()){ result: ActivityResult ->
            val response = IdpResponse.fromResultIntent(result.data)
            // Listen to the result of the sign - in process
            if(result.resultCode == Activity.RESULT_OK){
                Log.i(TAG, "User ${FirebaseAuth.getInstance().currentUser?.displayName} has signed in")
            } else {
                Log.i(TAG, "Sign in unsuccessful ${response?.error?.errorCode}")
            }
        }
    }

    private fun launchSignInFlow() {
        Log.d(TAG, "Launching sign in flow")
        // Give users the option to either sign in / register with their email or Google account
        val providers = arrayListOf(AuthUI.IdpConfig.EmailBuilder().build(), AuthUI.IdpConfig.GoogleBuilder().build())

        registerForActivityResult.launch(
            AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(providers)
            .build())

    }
}