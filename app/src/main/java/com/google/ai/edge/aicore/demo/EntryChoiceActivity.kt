package com.google.ai.edge.aicore.demo

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import com.google.ai.edge.aicore.demo.java.MainActivity

class EntryChoiceActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_entry_choice)

    findViewById<TextView>(R.id.kotlin_entry_point).setOnClickListener {
      val intent =
        Intent(
          this@EntryChoiceActivity,
          com.google.ai.edge.aicore.demo.kotlin.MainActivity::class.java,
        )
      startActivity(intent)
    }

    findViewById<TextView>(R.id.java_entry_point).setOnClickListener {
      val intent = Intent(this@EntryChoiceActivity, MainActivity::class.java)
      startActivity(intent)
    }
  }
}
