package ge.edison.lockers

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import ge.edison.lockers.databinding.ErrorDisplayBinding

class ErrorDisplay : AppCompatActivity() {
    private var binding: ErrorDisplayBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ErrorDisplayBinding.inflate(layoutInflater)
        setContentView(binding!!.root)
    }

    override fun onCreateView(name: String, context: Context, attrs: AttributeSet): View {
        super.onCreateView(name, context, attrs)

        binding!!.textView.text = intent.extras?.getString("error") ?: intent.getStringExtra("error")

        binding!!.backButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            this.startActivity(intent)
        }

        return binding!!.root
    }
}