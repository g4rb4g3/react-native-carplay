package org.birkir.carplay.parser

import androidx.car.app.CarContext
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Template
import com.facebook.react.bridge.ReadableMap
import org.birkir.carplay.screens.CarScreenContext

class TemplateParser internal constructor(
  private val context: CarContext,
  private val carScreenContext: CarScreenContext) {

  fun parse(props: ReadableMap): Template {
    val template = when (props.getString("type")) {
      "list" -> RCTListTemplate(context, carScreenContext)
      "grid" -> RCTGridTemplate(context, carScreenContext)
      "map" -> RCTMapTemplate(context, carScreenContext)
      "navigation" -> RCTMapTemplate(context, carScreenContext)
      "place-list-map" -> RCTMapTemplate(context, carScreenContext)
      "place-list-navigation" -> RCTMapTemplate(context, carScreenContext)
      "route-preview" -> RCTMapTemplate(context, carScreenContext)
      "map-with-list" -> RCTMapTemplate(context, carScreenContext)
      "map-with-pane" -> RCTMapTemplate(context, carScreenContext)
      "map-with-grid" -> RCTMapTemplate(context, carScreenContext)
      "pane" -> RCTPaneTemplate(context, carScreenContext)
      "search" -> RCTSearchTemplate(context, carScreenContext)
      "tabbar" -> RCTTabTemplate(context, carScreenContext)
      "message" -> RCTMessageTemplate(context, carScreenContext)
      "signin" -> RCTSignInTemplate(context, carScreenContext)
      else -> null
    }

    return template?.parse(props) ?: PaneTemplate
      .Builder(
        Pane.Builder().setLoading(true).build()
      ).setTitle("Template missing").build()
  }
}
