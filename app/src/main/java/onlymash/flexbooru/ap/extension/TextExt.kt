package onlymash.flexbooru.ap.extension

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.text.format.DateFormat
import java.text.SimpleDateFormat
import java.util.*

private const val DES_DATE_FORMAT = "yyyy-MM-dd HH:mm"
private const val SOURCE_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSSSSS"

fun Context.copyText(text: String?) {
    val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    cm.setPrimaryClip(ClipData.newPlainText("text", text ?: ""))
}

fun Long.formatDate(): CharSequence {
    val cal = Calendar.getInstance(Locale.getDefault())
    cal.timeInMillis = this
    return DateFormat.format(DES_DATE_FORMAT, cal)
}

fun String.formatDate(): CharSequence {
    val dateFormat = SimpleDateFormat(SOURCE_DATE_FORMAT, Locale.ENGLISH).apply {
        timeZone = TimeZone.getTimeZone("GMT")
    }
    val date = dateFormat.parse(this.replace('T', ' ')) ?: return ""
    return date.time.formatDate()
}