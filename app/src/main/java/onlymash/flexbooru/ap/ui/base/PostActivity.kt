package onlymash.flexbooru.ap.ui.base

import android.content.DialogInterface
import android.text.InputType
import android.view.MenuItem
import android.widget.EditText
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import onlymash.flexbooru.ap.R
import onlymash.flexbooru.ap.common.*

abstract class PostActivity : BaseActivity() {

    private var currentAspectRatio: String = ""

    private var queryListener: QueryListener? = null

    internal fun setQueryListener(listener: QueryListener?) {
        queryListener = listener
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {

            R.id.action_order_date -> queryListener?.onOrderChange(ORDER_DATE)
            R.id.action_order_date_revers -> queryListener?.onOrderChange(ORDER_DATE_REVERS)
            R.id.action_order_rating -> queryListener?.onOrderChange(ORDER_RATING)
            R.id.action_order_downloads -> queryListener?.onOrderChange(ORDER_DOWNLOADS)
            R.id.action_order_size -> queryListener?.onOrderChange(ORDER_SIZE)
            R.id.action_order_tags_count -> queryListener?.onOrderChange(ORDER_TAGS_COUNT)

            R.id.action_date_range_anytime -> queryListener?.onDateRangeChange(DATE_RANGE_ANYTIME)
            R.id.action_date_range_past_day -> queryListener?.onDateRangeChange(DATE_RANGE_PAST_DAY)
            R.id.action_date_range_past_week -> queryListener?.onDateRangeChange(DATE_RANGE_PAST_WEEK)
            R.id.action_date_range_past_month -> queryListener?.onDateRangeChange(DATE_RANGE_PAST_MONTH)

            R.id.action_input_aspect_ratio -> changeAspectRatio()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun changeAspectRatio() {
        val layout = FrameLayout(this)
        val editText = EditText(this)
        layout.addView(editText)
        val margin = resources.getDimensionPixelSize(R.dimen.margin_horizontal_edit_text_dialog)
        editText.layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
            marginStart = margin
            marginEnd = margin
        }
        editText.maxLines = 1
        editText.setText(currentAspectRatio)
        editText.inputType = InputType.TYPE_CLASS_TEXT
        AlertDialog.Builder(this)
            .setTitle(R.string.posts_aspect_ratio)
            .setView(layout)
            .setPositiveButton(R.string.dialog_ok) { _, _ ->
                currentAspectRatio = editText.text?.toString() ?: ""
                queryListener?.onAspectRatioChange(currentAspectRatio)
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .create()
            .show()
    }
}