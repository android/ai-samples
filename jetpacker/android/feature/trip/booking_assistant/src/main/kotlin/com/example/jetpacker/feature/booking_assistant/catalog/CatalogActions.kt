/*
 * Copyright 2026 The Android Open Source Project
 */

package com.example.jetpacker.feature.booking_assistant.catalog

import androidx.a2ui.compose.runtime.A2uiComponentScope

/**
 * Dispatches an action merging context parameters with explicit payload values.
 */
internal fun A2uiComponentScope.dispatchMergedAction(
  action: Map<String, Any?>?,
  defaultEventName: String,
  extraContext: Map<String, Any>,
) {
  val eventPayload = HashMap<String, Any>(extraContext)
  if (action != null) {
    val eventMap = (action["event"] as? Map<*, *>) ?: action
    val contextMap = (eventMap["context"] as? Map<*, *>) ?: (action["context"] as? Map<*, *>)
    contextMap?.forEach { (k, v) ->
      if (k is String && v != null) eventPayload[k] = v
    }
    val eventName = (eventMap["name"] as? String)
      ?: (action["eventName"] as? String)
      ?: defaultEventName
    eventPayload["eventName"] = eventName
  } else {
    eventPayload["eventName"] = defaultEventName
  }
  dispatchAction(eventPayload)
}
