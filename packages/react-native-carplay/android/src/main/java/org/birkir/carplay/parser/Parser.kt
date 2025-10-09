package org.birkir.carplay.parser

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.util.Log
import androidx.car.app.CarContext
import androidx.car.app.model.Action
import androidx.car.app.model.Action.FLAG_DEFAULT
import androidx.car.app.model.Action.FLAG_IS_PERSISTENT
import androidx.car.app.model.Action.FLAG_PRIMARY
import androidx.car.app.model.CarColor
import androidx.car.app.model.CarIcon
import androidx.car.app.model.CarIconSpan
import androidx.car.app.model.CarText
import androidx.car.app.model.DateTimeWithZone
import androidx.car.app.model.Distance
import androidx.car.app.model.DistanceSpan
import androidx.car.app.model.OnClickListener
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.Destination
import androidx.car.app.navigation.model.Lane
import androidx.car.app.navigation.model.LaneDirection
import androidx.car.app.navigation.model.Maneuver
import androidx.car.app.navigation.model.Step
import androidx.car.app.navigation.model.TravelEstimate
import androidx.core.graphics.drawable.IconCompat
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.ReadableType
import org.birkir.carplay.screens.CarScreenContext
import org.birkir.carplay.utils.EventEmitter
import java.util.TimeZone
import kotlin.math.max
import androidx.core.graphics.toColorInt

class Parser(
  context: CarContext, carScreenContext: CarScreenContext
) : RCTTemplate(context, carScreenContext) {
  override fun parse(props: ReadableMap): Template {
    return PaneTemplate.Builder(Pane.Builder().build()).build()
  }


  companion object {
    fun parseStep(map: ReadableMap, context: CarContext): Step {
      return Step.Builder().apply {
        map.getArray("lanes")?.let { parseLanes(it, this) }
        parseCue(map, context)?.let { setCue(it) }
        map.getMap("lanesImage")?.let { setLanesImage(parseCarIcon(it, context)) }
        map.getMap("maneuver")?.let { setManeuver(parseManeuver(it, context)) }
        map.getString("road")?.let { setRoad(it) }
      }.build()
    }

    fun parseBitmap(map: ReadableMap, context: Context): Bitmap {
      map.getString("uri")?.let {
        return BitmapCache.get(it, context)
      }

      throw IllegalArgumentException("can not parseBitmap, uri missing in map")
    }

    fun parseCarIcon(map: ReadableMap, context: CarContext): CarIcon {
      val bitmap = parseBitmap(map, context)
      val iconCompat = IconCompat.createWithBitmap(bitmap)

      if (map.hasKey("tintColor") && !map.isNull("tintColor")) {
        val colorStr = map.getString("tintColor")
        try {
          val color = colorStr!!.toColorInt()
          iconCompat.setTint(color)
        } catch (e: IllegalArgumentException) {
        }
      }

      return CarIcon.Builder(iconCompat).build()
    }

    fun parseTravelEstimate(map: ReadableMap): TravelEstimate {
      val dateTimeMap = map.getMap("destinationTime")!!
      val destinationDateTime = DateTimeWithZone.create(
        dateTimeMap.getDouble("timeSinceEpochMillis").toLong(),
        TimeZone.getTimeZone(dateTimeMap.getString("id")),
      )
      val builder = TravelEstimate.Builder(
        Distance.create(
          max(map.getDouble("distanceRemaining"), 0.0),
          parseDistanceUnit(map.getString("distanceUnits"))
        ),
        destinationDateTime,
      )
      map.getString("distanceRemainingColor")?.let {
        builder.setRemainingDistanceColor(parseColor(it))
      }
      map.getString("timeRemainingColor")?.let {
        builder.setRemainingTimeColor(parseColor(it))
      }
      map.getString("tripText")?.let {
        builder.setTripText(CarText.Builder(it).build())
      }
      var remainingTime = map.getDouble("timeRemaining").toLong()
      if (remainingTime < 0) {
        remainingTime = TravelEstimate.REMAINING_TIME_UNKNOWN
      }
      builder.setRemainingTimeSeconds(remainingTime)
      return builder.build()
    }

    fun parseColor(colorName: String?): CarColor {
      if (colorName?.startsWith("#") == true) {
        return CarColor.createCustom(Color.parseColor(colorName),Color.parseColor(colorName))
      }

      return when (colorName) {
        "blue" -> CarColor.BLUE
        "green" -> CarColor.GREEN
        "primary" -> CarColor.PRIMARY
        "red" -> CarColor.RED
        "secondary" -> CarColor.SECONDARY
        "yellow" -> CarColor.YELLOW
        "default" -> CarColor.DEFAULT
        else -> CarColor.DEFAULT
      }
    }

    fun parseDestination(destination: ReadableMap, context: CarContext): Destination {
      return Destination.Builder().apply {
        setName(destination.getString("name")!!)
        destination.getString("address")?.let {
          setAddress(it)
        }
        destination.getMap("image")?.let {
          setImage(parseCarIcon(it, context))
        }
      }.build()
    }

    fun parseDistance(map: ReadableMap): Distance {
      return Distance.create(
        max(map.getDouble("distance"), 0.0),
        parseDistanceUnit(map.getString("distanceUnits"))
      )
    }

    fun parseCarText(title: String, props: ReadableMap?): CarText {
      val spanBuilder = SpannableString(title)
      props?.let {
        try {
          val index = title.indexOf("%d")
          if (index != -1) {
            spanBuilder.setSpan(
              DistanceSpan.create(Parser.parseDistance(props)),
              index,
              index + 2,
              Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
          }
          it
        } catch (e: Exception) {
          Log.w(TAG, "getCarText: failed to parse the CarText")
        }
      }
      return CarText.Builder(spanBuilder).build()
    }

    fun parseAction(map: ReadableMap?, context: CarContext, onClickListener: OnClickListener?): Action {
      val type = map?.getString("type")
      if (type == "appIcon") {
        return Action.APP_ICON
      } else if (type == "back") {
        return Action.BACK
      } else if (type == "pan") {
        return Action.PAN
      }
      val builder = Action.Builder()
      if (map != null) {
        map.getString("title")?.let {
          builder.setTitle(it)
        }
        map.getMap("image")?.let {
          builder.setIcon(Parser.parseCarIcon(it, context))
        }
        map.getString("visibility")?.let {
          if (it == "primary") {
            builder.setFlags(FLAG_PRIMARY)
          }
          if (it == "persistent") {
            builder.setFlags(FLAG_IS_PERSISTENT)
          }
          if (it == "default") {
            builder.setFlags(FLAG_DEFAULT)
          }
        }
        try {
          builder.setBackgroundColor(parseColor(map.getString("backgroundColor")))
        } catch (e: Exception) {
          Log.e(TAG, "failed to set button background color", e)
        }
        onClickListener?.let {
          builder.setOnClickListener(it)
        }
      }
      return builder.build()
    }

    fun parseAction(map: ReadableMap?, context: CarContext, eventEmitter: EventEmitter): Action {
      val id = map?.getString("id")

      return parseAction(map, context) {
        if (id != null) {
          eventEmitter.buttonPressed(id)
        }
      }
    }

    private fun parseDistanceUnit(value: String?): Int {
      return when (value) {
        "meters" -> Distance.UNIT_METERS
        "miles" -> Distance.UNIT_MILES
        "kilometers" -> Distance.UNIT_KILOMETERS
        "yards" -> Distance.UNIT_YARDS
        "feet" -> Distance.UNIT_FEET
        else -> Distance.UNIT_METERS
      }
    }

    private fun parseLanes(lanes: ReadableArray, builder: Step.Builder) {
      for (i in 0 until lanes.size()) {
        val map = lanes.getMap(i)
        map?.let {
          val laneBuilder = Lane.Builder()
          val shape = it.getInt("shape")
          val recommended = it.getBoolean("recommended")
          val lane = laneBuilder.addDirection(LaneDirection.create(shape, recommended)).build()
          builder.addLane(lane)
        }
      }
    }

    private fun parseCue(map: ReadableMap, context: CarContext): SpannableString? {
      if (!map.hasKey("cue")) {
        return null
      }
      val cue = map.getDynamic("cue")
      if (cue.isNull) {
        return null
      }
      if (cue.type == ReadableType.String) {
        return SpannableString(cue.asString())
      }
      if (cue.type == ReadableType.Map) {
        val cueMap = cue.asMap()
        val text = cueMap.getString("text")
        val image = parseCarIcon(cueMap.getMap("image")!!, context)
        val alignment = cueMap.getInt("alignment")
        val start = cueMap.getInt("start")
        val end = cueMap.getInt("end")

        return SpannableString(text).apply {
          setSpan(
            CarIconSpan.create(image, alignment), start, end, Spannable.SPAN_INCLUSIVE_EXCLUSIVE
          )
        }
      }
      throw IllegalArgumentException("unsupported type ${cue.type}")
    }

    private fun parseManeuver(map: ReadableMap, context: CarContext): Maneuver {
      val type = map.getInt("type")
      val builder = Maneuver.Builder(type)
      builder.setIcon(parseCarIcon(map.getMap("image")!!, context))
      if (type == Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CW_WITH_ANGLE || type == Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW_WITH_ANGLE) {
        builder.setRoundaboutExitAngle(map.getInt("roundaboutExitAngle"))
      }

      if (type == Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CW || type == Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW || type == Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CW_WITH_ANGLE || type == Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW_WITH_ANGLE) {
        builder.setRoundaboutExitNumber(map.getInt("roundaboutExitNumber"))
      }

      return builder.build()
    }

  }

}
