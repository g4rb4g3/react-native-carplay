package org.birkir.carplay.parser

import android.text.Spannable
import android.text.SpannableString
import android.util.Log
import androidx.car.app.CarContext
import androidx.car.app.HostException
import androidx.car.app.constraints.ConstraintManager
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarLocation
import androidx.car.app.model.DistanceSpan
import androidx.car.app.model.DurationSpan
import androidx.car.app.model.GridItem
import androidx.car.app.model.Header
import androidx.car.app.model.ItemList
import androidx.car.app.model.Metadata
import androidx.car.app.model.Pane
import androidx.car.app.model.Place
import androidx.car.app.model.PlaceMarker
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.car.app.model.Toggle
import androidx.car.app.navigation.model.MessageInfo
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.car.app.navigation.model.RoutingInfo
import androidx.car.app.versioning.CarAppApiLevels
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.ReadableMap
import org.birkir.carplay.BuildConfig
import org.birkir.carplay.screens.CarScreenContext
import org.birkir.carplay.utils.EventEmitter
import kotlin.math.min


/**
 * Base class for parsing the template based on the props passed from ReactNative
 *
 * @property context
 * @property carScreenContext
 */
abstract class RCTTemplate(
  protected val context: CarContext,
  protected val carScreenContext: CarScreenContext
) {

  abstract fun parse(props: ReadableMap): Template

  protected val eventEmitter: EventEmitter
    get() = carScreenContext.eventEmitter

  protected fun parseActionStrip(actions: ReadableArray): ActionStrip {
    val builder = ActionStrip.Builder()
    for (i in 0 until actions.size()) {
      val actionMap = actions.getMap(i)
      val action = Parser.parseAction(map = actionMap, context = context, eventEmitter = eventEmitter)
      builder.addAction(action)
    }
    return builder.build()
  }

  protected fun parseItemList(
    items: ReadableArray?,
    type: ItemListType = ItemListType.Row,
    isMapWithContentTemplate: Boolean = false
  ): ItemList {
    return ItemList.Builder().apply {
      var selectedIndex: Int? = null
      val itemIds = mutableListOf<String>()

      val contentType = when(type) {
        ItemListType.Row -> ConstraintManager.CONTENT_LIMIT_TYPE_LIST
        ItemListType.Grid -> ConstraintManager.CONTENT_LIMIT_TYPE_GRID
        ItemListType.PlaceListNavigation -> ConstraintManager.CONTENT_LIMIT_TYPE_PLACE_LIST
        ItemListType.RouteList -> ConstraintManager.CONTENT_LIMIT_TYPE_ROUTE_LIST
      }

      items?.let {
        for (i in 0 until getMaxContentSize(
          carContext = context,
          contentType = contentType,
          preferredContentSize = it.size()
        )) {
          val item = it.getMap(i)
          val id = if (item.hasKey("id")) item.getString("id") else null
          itemIds.add(i, id ?: "")

          if (item.hasKey("selected") && item.getBoolean("selected")) {
            selectedIndex = i
          }

          if (type == ItemListType.Row || type == ItemListType.RouteList || type == ItemListType.PlaceListNavigation) {
            addItem(parseRowItem(item, i))
          } else if (type == ItemListType.Grid) {
            addItem(parseGridItem(item, i, isMapWithContentTemplate))
          }
        }
      }

      selectedIndex?.let {
        setSelectedIndex(it)
      }

      if (type === ItemListType.RouteList || selectedIndex != null) {
        setOnSelectedListener {
          val id = itemIds.get(it)
          eventEmitter.didSelectListItem(id, it)
        }
      }
    }.build()
  }

  protected fun parseRowItem(item: ReadableMap, index: Int): Row {
    val id = item.getString("id") ?: index.toString()
    return Row.Builder().apply {
      item.getString("text")?.let {
        val text = SpannableString(it)
        item.getMap("distance")?.let { distance ->
          val distanceSpan = DistanceSpan.create(Parser.parseDistance(distance))
          val start = distance.getInt("start")
          val end = distance.getInt("end")
          text.setSpan(distanceSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        item.getMap("duration")?.let { duration ->
          val durationSpan = DurationSpan.create(duration.getDouble("seconds").toLong())
          val start = duration.getInt("start")
          val end = duration.getInt("end")
          text.setSpan(durationSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        setTitle(text)
      }
      item.getString("detailText")?.let { addText(it) }
      item.getMap("image")?.let { setImage(Parser.parseCarIcon(it, context)) }
      if (item.hasKey("browsable") && item.getBoolean("browsable")) {
        setOnClickListener {
          eventEmitter.didSelectListItem(
            id,
            index
          )
        }
      }
      if (item.hasKey("toggle")) {
        setToggle(
          Toggle.Builder {
            eventEmitter.didSelectListItem(id, index)
          }.setChecked(item.getBoolean("toggle"))
            .build()
        )
      }

      if (item.hasKey("action")) {
        addAction(
          Parser.parseAction(item.getMap("action"), context, eventEmitter)
        )
      }
    }.build()
  }

  protected fun parseGridItem(
    item: ReadableMap,
    index: Int,
    isMapWithContentTemplate: Boolean = false
  ): GridItem {
    val id = item.getString("id") ?: index.toString()
    return GridItem.Builder().apply {
      val titleVariants = item.getArray("titleVariants")
      val metadata = item.getMap("metadata")

      if (titleVariants != null) {
        if (titleVariants.size() > 0) {
          setTitle(
            Parser.parseCarText(
              titleVariants.getString(0),
              metadata
            )
          )
        }
        if (titleVariants.size() > 1) {
          setText(titleVariants.getString(1))
        }
      }
      item.getMap("image")?.let { setImage(Parser.parseCarIcon(it, context)) }
      setLoading(item.isLoading())
      setOnClickListener {
        if (isMapWithContentTemplate) eventEmitter.buttonPressed(id) else eventEmitter.gridButtonPressed(
          id,
          index
        )
      }
    }.build()
  }

  fun parsePlace(props: ReadableMap): Place {
    val builder = Place.Builder(
      CarLocation.create(
        props.getDouble("latitude"),
        props.getDouble("longitude"),
      )
    )
    PlaceMarker.Builder().apply {
      setIcon(Parser.parseCarIcon(props.getMap("image")!!, context), PlaceMarker.TYPE_IMAGE)
      builder.setMarker(this.build())

    }

    return builder.build()
  }

  fun parseMetadata(props: ReadableMap?): Metadata? {
    val type = props?.getString("type")
    if (props == null || type == null || type != "place") {
      Log.w(TAG, "parseMetaData: invalid type provided $type")
      return null
    }
    return Metadata.Builder().setPlace(parsePlace(props)).build()
  }

  protected fun buildRow(props: ReadableMap): Row {
    val builder = Row.Builder()
    builder.setTitle(
      Parser.parseCarText(
        props.getString("title")!!,
        props.getMap("metadata")
      )
    )
    props.getArray("texts")?.let {
      for (i in 0 until it.size()) {
        builder.addText(it.getString(i))
      }
    }
    props.getMap("image")?.let {
      builder.setImage(Parser.parseCarIcon(it, context))
    }
    try {
      val onPress = props.getInt("onPress")
      builder.setBrowsable(true)
//      builder.setOnClickListener { invokeCallback(onPress) }
    } catch (e: Exception) {
      Log.w(TAG, "buildRow: failed to set clickListener on the row")
    }
    parseMetadata(props.getMap("metadata"))?.let {
      builder.setMetadata(it)
    }
    return builder.build()
  }

  protected fun parsePane(item: ReadableMap): Pane {
    return Pane.Builder().apply {
      setLoading(item.isLoading())
      item.getMap("image")?.let {
        setImage(Parser.parseCarIcon(it, context))
      }
      item.getArray("actions")?.let {
        for (i in 0 until it.size()) {
          addAction(Parser.parseAction(it.getMap(i), context, eventEmitter))
        }
      }
      item.getArray("items")?.let {
        for (i in 0 until getMaxContentSize(
          carContext = context,
          contentType = ConstraintManager.CONTENT_LIMIT_TYPE_PANE,
          it.size()
        )) {
          addRow(parseRowItem(it.getMap(i), i))
        }
      }
    }.build()
  }

  protected fun parseHeader(map: ReadableMap): Header {
    return Header.Builder().apply {
      map.getString("title")?.let { setTitle(Parser.parseCarText(it, map)) }
      map.getMap("startAction")?.let { setStartHeaderAction(Parser.parseAction(it, context, eventEmitter)) }
      map.getArray("endActions")?.let {
        for (i in 0 until it.size()) {
          addEndHeaderAction(Parser.parseAction(it.getMap(i), context, eventEmitter))
        }
      }
    }.build()
  }

  protected fun parseMessageInfo(map: ReadableMap): MessageInfo {
    val builder = MessageInfo.Builder(map.getString("title")!!)
    map.getMap("image")?.let { builder.setImage(Parser.parseCarIcon(it, context)) }
    return builder.build()
  }

  protected fun parseRoutingInfo(map: ReadableMap): RoutingInfo {
    val isLoading = map.isLoading()

    if (isLoading) {
      return RoutingInfo.Builder().apply {
        setLoading(true)
      }.build()
    }

    return RoutingInfo.Builder()
      .apply {
        setCurrentStep(
          Parser.parseStep(map.getMap("step")!!, context),
          Parser.parseDistance(map)
        )
        map.getMap("junctionImage")?.let { setJunctionImage(Parser.parseCarIcon(it, context)) }
        map.getMap("nextStep")?.let { setNextStep(Parser.parseStep(it, context)) }
      }.build()
  }

  protected fun parseNavigationInfo(map: ReadableMap): NavigationTemplate.NavigationInfo {
    val type = map.getString("type")
    return if (type == "routingInfo") {
      parseRoutingInfo(map)
    } else {
      parseMessageInfo(map)
    }
  }

  private fun getMaxContentSize(carContext: CarContext, contentType: Int, preferredContentSize: Int): Int {
    val maxContentSize =
      if (carContext.carAppApiLevel < CarAppApiLevels.LEVEL_2) getMaxContentDefaults(contentType) else getMaxContentSize(
        carContext,
        contentType
      )
    if (BuildConfig.DEBUG && preferredContentSize > maxContentSize) {
      Log.w(TAG, "tried to fit more items then possible $maxContentSize used instead of $preferredContentSize for contentType $contentType")
    }
    return min(preferredContentSize, maxContentSize)
  }

  private fun getMaxContentDefaults(contentType: Int): Int {
    if (contentType == ConstraintManager.CONTENT_LIMIT_TYPE_ROUTE_LIST) {
      return 3
    }
    return 6
  }

  private fun getMaxContentSize(carContext: CarContext, contentType: Int): Int {
    return try {
      carContext.getCarService(ConstraintManager::class.java).getContentLimit(contentType)
    } catch (exception: HostException) {
      // we sometimes still get exceptions here due to missing API level (which is a bug on AA side)
      getMaxContentDefaults(contentType)
    }
  }

  companion object {
    const val TAG = "RNCarPlayTemplate"
  }
}
