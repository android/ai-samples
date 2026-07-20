# Contributing to JetPacker

We'd love to accept your patches! Before we can take them, we have to jump a couple of legal hurdles.

## Contributor License Agreements

Please fill out either the individual or corporate Contributor License Agreement (CLA).

  * If you are an individual writing original source code and you're sure you own the intellectual property, then you'll need to sign an [individual CLA](https://developers.google.com/open-source/cla/individual).
  * If you work for a company that wants to allow you to contribute your work, then you'll need to sign a [corporate CLA](https://developers.google.com/open-source/cla/corporate).

Follow either of the two links above to access the appropriate CLA and instructions for how to sign and return it. Once we receive it, we'll be able to accept your pull requests.

***Note***: Only original source code from you and other people who have signed the CLA can be accepted into the main repository.

## Contributing Code

We welcome pull requests for bug fixes and small improvements. If you want to contribute a new feature or make a significant change, please open an issue first to discuss it with the maintainers.

To submit a contribution:
1. Fork the repository.
2. Create a new branch for your changes (`git checkout -b feature/my-new-feature`).
3. Make your changes and ensure they follow the project's code style and guidelines.
4. Run all tests to make sure everything still works.
5. Commit your changes with a clear commit message.
6. Push to your fork and submit a pull request to the `main` branch.

## Community Guidelines

This project follows the [Google Open Source Community Guidelines](https://opensource.google/conduct/).

## Setting Up the Development Environment

If you wish to run, explore, or modify the code locally:

### Prerequisites
1. **Android Studio**: Make sure you have the latest version of Android Studio.
2. **Android SDK**: Android SDK with API Level 36.
3. **Java Development Kit (JDK)**: JDK 17 is used for compile and target compatibility.

### Building the Project
1. Clone the repository:
   ```bash
   git clone https://github.com/android/ai-samples.git
   ```
2. Open the project in Android Studio.
3. Gradle will automatically sync and resolve dependencies.
4. Run the `:app` module on an emulator or connected device.
