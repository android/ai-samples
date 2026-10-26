/*
 * Copyright 2026 The Android Open Source Project
 */

package com.example.jetpacker.feature.booking_assistant.catalog

import androidx.a2ui.compose.runtime.A2uiComponentScope

/**
 * Dispatches an action merging context parameters with explicit payload values.
 * Wraps the payload in an A2UI 'event' schema matching [A2uiUserActionParser].
 */
internal fun A2uiComponentScope.dispatchMergedAction(
  action: Map<String, Any?>?,
  defaultEventName: String,
  extraContext: Map<String, Any>,
) {
  val mergedContext = HashMap<String, Any>(extraContext)
  val eventName: String
  if (action != null) {
    val eventMap = (action["event"] as? Map<*, *>) ?: action
    val contextMap = (eventMap["context"] as? Map<*, *>) ?: (action["context"] as? Map<*, *>)
    contextMap?.forEach { (k, v) ->
      if (k is String && v != null) mergedContext[k] = v
    }
    eventName = (eventMap["name"] as? String)
      ?: (action["name"] as? String)
      ?: (action["eventName"] as? String)
      ?: defaultEventName
  } else {
    eventName = defaultEventName
  }

  // Preserve event name in context map as well for compatibility with consumers
  mergedContext["name"] = eventName
  mergedContext["eventName"] = eventName

  val actionPayload = mapOf(
    "event" to mapOf(
      "name" to eventName,
      "context" to mergedContext,
    )
  )
  dispatchAction(actionPayload)
}
