package ge.edison.lockers

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import ge.edison.lockers.databinding.FragmentMainBinding
import java.text.SimpleDateFormat
import java.util.*

class Console(private var context: Context?, private val binding: FragmentMainBinding) {

    @SuppressLint("SimpleDateFormat")
    private val formatter = SimpleDateFormat("yyyy-MM-dd hh:mm:ss")

    class Message constructor(val text: String, val color: String, val paddingEnd: Int = 10)

    fun log(messages: List<Message>) {
        if (binding.consoleScroll.childCount > 49) {
            binding.consoleScroll.removeViewAt(0)
        }

        val textBox = getTextBox()

        val dateString = formatter.format(Date())
        val dateView = getTextView(dateString, "#FED62C", 15)
        textBox.addView(dateView)

        for (message in messages) {
            textBox.addView(getTextView(message.text, message.color, message.paddingEnd))
        }

        binding.console.addView(textBox)
        binding.consoleScroll.post {
            binding.consoleScroll.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun getTextView(text: String, color: String, paddingEnd: Int): TextView {
        val textView = TextView(context)
        textView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        textView.text = text
        val parsed: Int = try {
            Color.parseColor(color)
        } catch (e: Exception) {
            Color.parseColor("#FFFFFF")
        }
        textView.setTextColor(parsed)
        textView.setPadding(0, 0, paddingEnd, 0)

        return textView
    }

    private fun getTextBox(): LinearLayout {
        val linearLayout = LinearLayout(context)
        linearLayout.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        linearLayout.orientation = LinearLayout.HORIZONTAL
        linearLayout.setPadding(10, 5, 10, 5)
        return linearLayout
    }
}