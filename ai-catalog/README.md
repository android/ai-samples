# Android AI Sample Catalog

![Android AI Sample Catalog screenshots](https://developer.android.com/static/ai/assets/images/ai_sample_catalog_1440.png)

This folder contains the Android AI Sample catalog, a stand alone application giving you access to 
individual self-contained samples illustrating some of the Generative AI capabilities unlocked by 
some of Google's models.

> **Note:** These samples are intended to showcase specific AI capabilities in isolation, and they may use
> simplified code. They are demo not intended to be used as production-ready code.
> For best practices follow our documentation and check
> [Now In Android](https://github.com/android/nowinandroid)

Browse the samples inside the `/samples` folder:

- **gemini-chatbot**: a simple chatbot using Gemini 2.0 Flash
- **gemini-multimodal**: a single screen application leveraging text+image to text generation with Gemini 2.0 Flash
- **genai-summarization**: a text summarization sample using Gemini Nano
- **genai-image-description**: an image description sample using Gemini Nano
- **genai-writing-assistance**: a proofreading and rewriting sample using Gemini Nano
- **imagen**: an image generation sample using Imagen
- **magic-selfie**: an sample using ML Kit subject segmentation and Imagen for image generation
- **gemini-video-summarization**: a video summarization sample using Gemini 2.0 Flash
- **gemini-live-todo**: a todo list app using Gemini Live
- More to come...

> **Requires Firebase setup** the samples relying on Google Cloud models (Gemini Pro, Gemini Flash, etc...) 
> require setting up a Firebase project and connecting the app to Firebase (read more [here](https://firebase.google.com/docs/ai-logic/get-started?platform=android&api=dev#set-up-firebase)).   

> 🚧 **Work-in-Progress:** we are working on bringing more samples into the application.

## How to run

1. Clone the repository
2. Open the whole project in Android Studio.
3. Set up a Firebase project and connect your app to Firebase by adding your Firebase configuration 
file (google-services.json) to the app. Read more [here](https://firebase.google.com/docs/ai-logic/get-started?platform=android&api=dev#set-up-firebase).
3. Sync & Run `app` configuration

The app will open with the samples list screen that allows you to navigate throughout the different 
available samples.

## Reporting issues

You can report [issues with the samples](https://github.com/android/ai-samples/issues) using
this repository. When doing so, make sure to specify which sample you are referring to.

## Contributions

We aren't open to contribution to this project at the moment.

## License

```
Copyright 2023 The Android Open Source Project
 
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
