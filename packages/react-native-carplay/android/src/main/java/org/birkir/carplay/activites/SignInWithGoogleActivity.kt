package org.birkir.carplay.activites

import android.content.Intent
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import java.security.InvalidParameterException
import javax.annotation.Nullable

class SignInWithGoogleActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val signInCompleteCallback =
      intent.extras!!.getBinder(BINDER_KEY) as OnSignInComplete?

    val serverClientId = intent.extras?.getString("serverClientId")
      ?: throw InvalidParameterException("missing serverClientId parameter")

    val activityResultLauncher =
      registerForActivityResult<Intent, ActivityResult>(
        ActivityResultContracts.StartActivityForResult()
      ) { result: ActivityResult ->
        val account =
          GoogleSignIn.getSignedInAccountFromIntent(
            result.data
          ).result
        signInCompleteCallback!!.onSignInComplete(account)
        finish()
      }

    val googleSignInClient = GoogleSignIn.getClient(
      this, GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestServerAuthCode(serverClientId)
        .requestEmail()
        .build()
    )
    activityResultLauncher.launch(googleSignInClient.signInIntent)
  }

  /**
   * Binder callback to provide to the sign in activity.
   */
  abstract
  class OnSignInComplete : Binder(), IBinder {
    /**
     * Notifies that sign in flow completed.
     *
     * @param account the account signed in or `null` if there were issues signing in.
     */
    abstract fun onSignInComplete(@Nullable account: GoogleSignInAccount?)
  }

  companion object {
    const val BINDER_KEY = "SignInWithGoogleActivity"
  }
}