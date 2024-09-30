/*
 * Copyright 2024 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ai.edge.aicore.demo.java;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import androidx.core.content.ContextCompat;
import com.google.ai.edge.aicore.Content;
import com.google.ai.edge.aicore.GenerateContentResponse;
import com.google.ai.edge.aicore.GenerationConfig;
import com.google.ai.edge.aicore.GenerativeModel;
import com.google.ai.edge.aicore.demo.ContentAdapter;
import com.google.ai.edge.aicore.demo.GenerationConfigDialog;
import com.google.ai.edge.aicore.demo.GenerationConfigDialog.OnConfigUpdateListener;
import com.google.ai.edge.aicore.demo.GenerationConfigUtils;
import com.google.ai.edge.aicore.demo.R;
import com.google.ai.edge.aicore.java.GenerativeModelFutures;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/** Demonstrates the AICore SDK usage from Java. */
public class MainActivity extends AppCompatActivity implements OnConfigUpdateListener {
  private static final String TAG = MainActivity.class.getSimpleName();

  private EditText requestEditText;
  private Button sendButton;
  private CompoundButton streamingSwitch;
  private Button configButton;
  private RecyclerView contentRecyclerView;
  private GenerativeModelFutures modelFutures;
  private boolean useStreaming;
  private boolean hasFirstStreamingResult;

  private final ContentAdapter contentAdapter = new ContentAdapter();

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    requestEditText = findViewById(R.id.request_edit_text);
    sendButton = findViewById(R.id.send_button);
    sendButton.setOnClickListener(
        view -> {
          String request = requestEditText.getText().toString();
          if (TextUtils.isEmpty(request)) {
            Toast.makeText(this, R.string.prompt_is_empty, Toast.LENGTH_SHORT).show();
            return;
          }

          contentAdapter.addContent(ContentAdapter.VIEW_TYPE_REQUEST, request);
          startGeneratingUi();
          generateContent(request);
        });

    streamingSwitch = findViewById(R.id.streaming_switch);
    streamingSwitch.setOnCheckedChangeListener(
        (compoundButton, isChecked) -> useStreaming = isChecked);
    useStreaming = streamingSwitch.isChecked();

    configButton = findViewById(R.id.config_button);
    configButton.setOnClickListener(
        view -> new GenerationConfigDialog().show(getSupportFragmentManager(), null));

    contentRecyclerView = findViewById(R.id.content_recycler_view);
    contentRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    contentRecyclerView.setAdapter(contentAdapter);

    initGenerativeModel();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();

    if (modelFutures != null) {
      modelFutures.getGenerativeModel().close();
    }
  }

  private void initGenerativeModel() {
    Context context = getApplicationContext();
    GenerationConfig.Builder configBuilder = GenerationConfig.Companion.builder();
    configBuilder.setContext(context);
    configBuilder.setTemperature(GenerationConfigUtils.getTemperature(context));
    configBuilder.setTopK(GenerationConfigUtils.getTopK(context));
    configBuilder.setMaxOutputTokens(GenerationConfigUtils.getMaxOutputTokens(context));

    GenerativeModel model = new GenerativeModel(configBuilder.build());
    modelFutures = GenerativeModelFutures.from(model);
  }

  private void generateContent(String request) {
    Content content = Content.Companion.builder().addText(request).build();
    if (useStreaming) {
      hasFirstStreamingResult = false;
      StringBuilder resultBuilder = new StringBuilder();
      modelFutures
          .generateContentStream(content)
          .subscribe(
              new Subscriber<GenerateContentResponse>() {
                @Override
                public void onSubscribe(Subscription s) {
                  s.request(Long.MAX_VALUE);
                }

                @Override
                public void onNext(GenerateContentResponse response) {
                  resultBuilder.append(response.getText());
                  runOnUiThread(
                      () -> {
                        if (hasFirstStreamingResult) {
                          contentAdapter.updateStreamingResponse(resultBuilder.toString());
                        } else {
                          contentAdapter.addContent(
                              ContentAdapter.VIEW_TYPE_RESPONSE, resultBuilder.toString());
                          hasFirstStreamingResult = true;
                        }
                      });
                }

                @Override
                public void onError(Throwable t) {
                  Log.e(TAG, "Failed to subscribe: " + t);
                  runOnUiThread(
                      () -> {
                        contentAdapter.addContent(
                            ContentAdapter.VIEW_TYPE_RESPONSE_ERROR, t.getMessage());
                        endGeneratingUi();
                      });
                }

                @Override
                public void onComplete() {
                  runOnUiThread(() -> endGeneratingUi());
                }
              });

    } else {
      Futures.addCallback(
          modelFutures.generateContent(content),
          new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
              contentAdapter.addContent(ContentAdapter.VIEW_TYPE_RESPONSE, result.getText());
              endGeneratingUi();
            }

            @Override
            public void onFailure(Throwable t) {
              contentAdapter.addContent(ContentAdapter.VIEW_TYPE_RESPONSE_ERROR, t.getMessage());
              endGeneratingUi();
            }
          },
          ContextCompat.getMainExecutor(this));
    }
  }

  private void startGeneratingUi() {
    sendButton.setEnabled(false);
    sendButton.setText(R.string.generating);
    requestEditText.setText(R.string.empty);
    streamingSwitch.setEnabled(false);
    configButton.setEnabled(false);
  }

  private void endGeneratingUi() {
    sendButton.setEnabled(true);
    sendButton.setText(R.string.button_send);
    streamingSwitch.setEnabled(true);
    configButton.setEnabled(true);
    contentRecyclerView.smoothScrollToPosition(contentAdapter.getItemCount() - 1);
  }

  @Override
  public void onConfigUpdated() {
    if (modelFutures != null) {
      modelFutures.getGenerativeModel().close();
    }
    initGenerativeModel();
  }
}
