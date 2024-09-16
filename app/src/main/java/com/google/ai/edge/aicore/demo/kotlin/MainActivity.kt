package com.google.ai.edge.aicore.demo.kotlin

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.Button
import android.widget.CompoundButton
import android.widget.EditText
import androidx.lifecycle.lifecycleScope
import com.google.ai.edge.aicore.GenerativeAIException
import com.google.ai.edge.aicore.GenerativeModel
import com.google.ai.edge.aicore.demo.ContentAdapter
import com.google.ai.edge.aicore.demo.GenerationConfigDialog
import com.google.ai.edge.aicore.demo.GenerationConfigUtils
import com.google.ai.edge.aicore.demo.R
import com.google.ai.edge.aicore.generationConfig
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch

/** Demonstrates the AICore SDK usage from Kotlin. */
class MainActivity : AppCompatActivity(), GenerationConfigDialog.OnConfigUpdateListener {

  private var requestEditText: EditText? = null
  private var sendButton: Button? = null
  private var streamingSwitch: CompoundButton? = null
  private var configButton: Button? = null
  private var contentRecyclerView: RecyclerView? = null
  private var model: GenerativeModel? = null
  private var useStreaming = false

  private val contentAdapter = ContentAdapter()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    requestEditText = findViewById(R.id.request_edit_text)
    sendButton = findViewById(R.id.send_button)
    sendButton!!.setOnClickListener {
      val request = requestEditText!!.text.toString()
      contentAdapter.addContent(ContentAdapter.VIEW_TYPE_REQUEST, request)
      startGeneratingUi()
      generateContent(request)
    }

    streamingSwitch = findViewById<CompoundButton>(R.id.streaming_switch)
    streamingSwitch!!.setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
      useStreaming = isChecked
    }
    useStreaming = streamingSwitch!!.isChecked

    configButton = findViewById(R.id.config_button)
    configButton!!.setOnClickListener {
      GenerationConfigDialog().show(supportFragmentManager, null)
    }

    contentRecyclerView = findViewById<RecyclerView>(R.id.content_recycler_view)
    contentRecyclerView!!.layoutManager = LinearLayoutManager(this)
    contentRecyclerView!!.adapter = contentAdapter

    initGenerativeModel()
  }

  override fun onDestroy() {
    super.onDestroy()
    model?.close()
  }

  private fun initGenerativeModel() {
    model =
      GenerativeModel(
        generationConfig {
          context = applicationContext
          temperature = GenerationConfigUtils.getTemperature(applicationContext)
          topK = GenerationConfigUtils.getTopK(applicationContext)
          maxOutputTokens = GenerationConfigUtils.getMaxOutputTokens(applicationContext)
        }
      )
  }

  private fun generateContent(request: String) {
    lifecycleScope.launch {
      try {
        if (useStreaming) {
          var hasFirstStreamingResult = false
          var result = ""
          model!!
            .generateContentStream(request)
            .onCompletion { endGeneratingUi() }
            .collect { response ->
              run {
                result += response.text
                if (hasFirstStreamingResult) {
                  contentAdapter.updateStreamingResponse(result)
                } else {
                  contentAdapter.addContent(ContentAdapter.VIEW_TYPE_RESPONSE, result)
                  hasFirstStreamingResult = true
                }
              }
            }
        } else {
          val response = model!!.generateContent(request)
          contentAdapter.addContent(ContentAdapter.VIEW_TYPE_RESPONSE, response.text!!)
          endGeneratingUi()
        }
      } catch (e: GenerativeAIException) {
        contentAdapter.addContent(ContentAdapter.VIEW_TYPE_RESPONSE_ERROR, e.message!!)
        endGeneratingUi()
      }
    }
  }

  private fun startGeneratingUi() {
    sendButton!!.isEnabled = false
    sendButton!!.setText(R.string.generating)
    requestEditText!!.setText(R.string.empty)
    streamingSwitch!!.isEnabled = false
    configButton!!.isEnabled = false
  }

  private fun endGeneratingUi() {
    sendButton!!.isEnabled = true
    sendButton!!.setText(R.string.send)
    streamingSwitch!!.isEnabled = true
    configButton!!.isEnabled = true
    contentRecyclerView!!.smoothScrollToPosition(contentAdapter.itemCount - 1)
  }

  override fun onConfigUpdated() {
    model?.close()
    initGenerativeModel()
  }
}
