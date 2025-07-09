package org.birkir.carplay.parser

import androidx.car.app.CarContext
import androidx.car.app.model.Header
import androidx.car.app.model.MessageTemplate
import com.facebook.react.bridge.ReadableMap
import org.birkir.carplay.screens.CarScreenContext

class RCTMessageTemplate(
  context: CarContext,
  carScreenContext: CarScreenContext
) : RCTTemplate(context, carScreenContext) {
  override fun parse(props: ReadableMap): MessageTemplate {
    val message = props.getString("message") ?: "No message"
    val messageText = Parser.parseCarText(message, props)
    return MessageTemplate.Builder(messageText).apply {
      props.getArray("actions")?.let {
        for (i in 0 until it.size()) {
          val map = it.getMap(i)
          addAction(Parser.parseAction(map, context, carScreenContext.eventEmitter))
        }
      }
      setHeader(Header.Builder().apply {
        props.getString("title")?.let {
          setTitle(it)
        }
        props.getMap("headerAction")?.let {
          setStartHeaderAction(Parser.parseAction(it, context, eventEmitter))
        }
      }.build())
      props.getMap("image")?.let { setIcon(Parser.parseCarIcon(it, context)) }
      props.getString("debugMessage")?.let { setDebugMessage(it) }
      setLoading(props.isLoading())
    }.build()
  }

  companion object {
    const val TAG = "RCTMessageTemplate"
  }
}
