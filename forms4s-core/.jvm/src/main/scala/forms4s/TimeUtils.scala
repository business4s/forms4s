package forms4s

import java.time.{ZoneOffset, ZonedDateTime}

object TimeUtils {
  def localTZOffset: ZoneOffset = {
    ZonedDateTime.now().getOffset
  }
}
