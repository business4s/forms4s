package forms4s

import java.time.ZoneOffset

object TimeUtils {
  def localTZOffset: ZoneOffset = {
    val offsetMinutes = new scala.scalajs.js.Date().getTimezoneOffset()
    val totalSeconds  = (-offsetMinutes * 60).toInt
    val zoneOffset    = ZoneOffset.ofTotalSeconds(totalSeconds)
    zoneOffset
  }
}
