# Upload JetPacker to Google Play Console as a connected app

These scripts build and sign a JetPacker Android App Bundle (`.aab`) for Google Play Console internal testing, allowing Gemini and Android to call JetPacker's AppFunctions as a connected app.

To run the complete demo flow, see [DEMO.md](DEMO.md).

## Prerequisites

- Android SDK installed and configured.
- A Google Play developer account with Internal App Sharing or Internal Testing access.
- Test device running Android 16 or later (API 36+).
- Default store listing images and metadata (located in `store_listing/`).

## Quick Start

### 1. Create an upload signing key

Create an upload key once with the helper script:

```shell
./jetpacker/play/create_upload_key.sh
```

By default, this writes `~/keys/jetpacker-upload.jks`. Keep this file and its password secure.

### 2. Configure a demo profile (Optional)

Copy the profile template to set your package name, keystore path, and developer account:

```shell
cp jetpacker/play/profiles/example.conf.example jetpacker/play/profiles/my-profile.conf
```

Edit `profiles/my-profile.conf` to set your desired `APPLICATION_ID` and `KEYSTORE` location.

### 3. Build and sign the bundle

Run the build script with your profile:

```shell
./jetpacker/play/build_play_bundle.sh --profile my-profile
```

Or pass flags directly:

```shell
./jetpacker/play/build_play_bundle.sh \
    --keystore ~/keys/jetpacker-upload.jks \
    --application-id com.yourname.jetpacker \
    --universal-apk ~/jetpacker-play.apk
```

The script builds `:app:bundleRelease` with Gradle, signs it with your upload key, and verifies the AppFunctions metadata assets.

## Profile Configuration Format

Profiles in `profiles/*.conf` allow storing repeatable build settings:

| Key | Description |
| :--- | :--- |
| `APPLICATION_ID` | Package name (e.g., `com.yourname.jetpacker`). |
| `KEYSTORE` | Path to upload keystore (starts with `/` or `~/`). |
| `UNIVERSAL_APK` | Optional path to output a universal test APK. |
| `PLAY_CONSOLE` | Google Play Console URL (`https://play.google.com/console`). |
| `DEVELOPER_ID` | Numerical developer account ID. |

Notice: Never commit private keystores or passwords to source control. Local `*.conf` files are git-ignored.
