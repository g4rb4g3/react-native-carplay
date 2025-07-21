package org.birkir.carplay.parser

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.car.app.CarContext
import androidx.car.app.CarToast
import androidx.car.app.CarToast.LENGTH_LONG
import androidx.car.app.model.Action
import androidx.car.app.model.CarIcon
import androidx.car.app.model.InputCallback
import androidx.car.app.model.ParkedOnlyOnClickListener
import androidx.car.app.model.signin.InputSignInMethod
import androidx.car.app.model.signin.ProviderSignInMethod
import androidx.car.app.model.signin.QRCodeSignInMethod
import androidx.car.app.model.signin.SignInTemplate
import androidx.core.graphics.drawable.IconCompat
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReadableMap
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import org.birkir.carplay.R
import org.birkir.carplay.activites.SignInWithGoogleActivity
import org.birkir.carplay.screens.CarScreenContext
import org.birkir.carplay.utils.PlayService
import java.security.InvalidParameterException
import javax.annotation.Nullable

class RCTSignInTemplate(
  context: CarContext, carScreenContext: CarScreenContext
) : RCTTemplate(context, carScreenContext) {
  override fun parse(props: ReadableMap): SignInTemplate {
    val title =
      props.getString("title") ?: throw InvalidParameterException("missing title parameter")
    val headerAction = props.getMap("headerAction")?.let {
      Parser.parseAction(it, context, null)
    } ?: throw InvalidParameterException("missing headerAction parameter")

    val instructions = props.getString("instructions")
      ?: throw InvalidParameterException("missing instructions parameter")

    val additionalText = props.getString("additionalText")

    val method =
      props.getMap("method") ?: throw InvalidParameterException("missing method parameter")

    val templateBuilder = when (method.getString("type")) {
      "qr" -> {
        val url = method.getString("url")
          ?: throw InvalidParameterException("missing headerAction parameter")

        SignInTemplate.Builder(QRCodeSignInMethod(Uri.parse(url)))
      }

      "google" -> {
        if (!PlayService.isPlayServiceAvailable(context)) null else {
          val actionTitle = method.getString("actionTitle")
            ?: throw InvalidParameterException("missing actionTitle parameter")
          val serverClientId = method.getString("serverClientId")
            ?: throw InvalidParameterException("missing serverClientId parameter")

          SignInTemplate.Builder(
            ProviderSignInMethod(
              Action.Builder().setTitle(actionTitle).setIcon(
                CarIcon.Builder(
                  IconCompat.createWithResource(
                    context, R.drawable.ic_google
                  )
                ).build()
              ).setOnClickListener(ParkedOnlyOnClickListener.create {
                val extras = Bundle(1)
                extras.putBinder(SignInWithGoogleActivity.BINDER_KEY,
                  object : SignInWithGoogleActivity.OnSignInComplete() {
                    override fun onSignInComplete(@Nullable account: GoogleSignInAccount?) {
                      val serverAuthCode = account?.serverAuthCode

                      if (serverAuthCode == null) {
                        CarToast.makeText(context, "Error signing in", LENGTH_LONG).show()
                      } else {
                        carScreenContext.eventEmitter.didSignIn(Arguments.createMap().apply {
                          putString("serverAuthCode", serverAuthCode)
                        })
                      }
                    }
                  })
                extras.putString("serverClientId", serverClientId)
                context.startActivity(
                  Intent().setClass(context, SignInWithGoogleActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtras(extras)
                )
              }).build()
            )
          )
        }
      }

      "mail" -> {
        val callback = object : InputCallback {
          override fun onInputSubmitted(text: String) {
            carScreenContext.eventEmitter.didSignIn(Arguments.createMap().apply {
              putString("text", text)
            })
          }
        }

        val hint = method.getString("hint")
        SignInTemplate.Builder(InputSignInMethod.Builder(callback).apply {
          hint?.let {
            setHint(it)
          }
          setInputType(method.getInt("inputType"))
        }.build())
      }

      else -> null
    }

    if (templateBuilder == null) {
      throw InvalidParameterException("missing template builder")
    }

    return templateBuilder.setHeaderAction(headerAction).setTitle(title)
      .setInstructions(instructions).apply {
        additionalText?.let {
          setAdditionalText(it)
        }
        props.getArray("actions")?.let {
          for (i in 0 until it.size()) {
            val action = it.getMap(i)
            action?.let { actionMap ->
              val id = actionMap.getString("id")
              val onClickListener = ParkedOnlyOnClickListener.create {
                if (id == null) {
                  return@create
                }
                carScreenContext.eventEmitter.buttonPressed(id)
              }
              addAction(Parser.parseAction(actionMap, context, onClickListener))
            }
          }
        }
      }.build()
  }

  companion object {
    const val TAG = "RCTSignInTemplate"
  }
}
