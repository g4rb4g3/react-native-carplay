package org.birkir.carplay.parser

import androidx.car.app.CarContext
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.SectionedItemList
import com.facebook.react.bridge.ReadableMap
import org.birkir.carplay.screens.CarScreenContext

class RCTListTemplate(
  context: CarContext,
  screenContext: CarScreenContext
) : RCTTemplate(context, screenContext) {

  override fun parse(props: ReadableMap): ListTemplate {
    return ListTemplate.Builder().apply {
      props.getString("title")?.let { setTitle(it) }

      // Actions
      props.getArray("actions")?.let {
        setActionStrip(
          parseActionStrip(it)
        )
      }

      // Header Action
      props.getMap("headerAction")?.let {
        setHeaderAction(
          Parser.parseAction(it, context, eventEmitter)
        )
      }

      // Loading
      setLoading(props.isLoading())

      if (props.isLoading()) {
        // templates in a loading state can not have items/sections
        return@apply
      }

      // Sections
      val sections = props.getArray("sections")
      val items = props.getArray("items")

      if (sections != null && items != null) {
        throw IllegalArgumentException("invalid template configuration, use either sections or items, not both!")
      }

      // in case we get a section list that has only on section and no title we treat it as single list similar to items
      val singleListItems =
        if (sections?.size() == 1 && sections.getMap(0)?.hasKey("header") == false) sections.getMap(0)
          ?.getArray("items") else items

      // Single List
      singleListItems?.let {
        setSingleList(
          parseItemList(it)
        )
        return@apply
      }

      // Sections
      sections?.let {
        for (i in 0 until it.size()) {
          val section = it.getMap(i)
          section?.let { sectionMap ->
            val header = sectionMap.getString("header")
            sectionMap.getArray("items")?.let { itemsArray ->
              addSectionedList(
                SectionedItemList.create(
                  parseItemList(itemsArray),
                  header ?: "Missing title"
                )
              )
            }
          }
        }
      }



    }.build()
  }

  companion object {
    const val TAG = "RCTListTemplate"
  }
}
