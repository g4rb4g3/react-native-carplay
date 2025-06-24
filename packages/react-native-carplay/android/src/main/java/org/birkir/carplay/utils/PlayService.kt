package org.birkir.carplay.utils

import android.content.Context
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability

object PlayService {
  fun isPlayServiceAvailable(context: Context): Boolean {
    return GoogleApiAvailability
      .getInstance()
      .isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS
  }
}