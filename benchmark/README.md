# Benchmarks

This module contains [Macrobenchmarks](https://developer.android.com/topic/performance/benchmarking/macrobenchmark-overview) for the application.

## What are Macrobenchmarks?

Macrobenchmarks allow you to measure the performance of your application in a realistic environment.
In this sample we rely on test for:
*   **Startup:** Measuring the time it takes for the app to launch.
*   **Frame Timing:** Measuring the jank and dropped frames during animations.

Macrobenchmarks run the app as a user would, interacting with the system and other apps.

## How to Run Benchmarks

To run the benchmarks, you can use the Gradle task or run them directly from Android Studio.

### Using Gradle

Run the following command in the terminal from the project root:

```bash
./gradlew :benchmark:connectedCheck
```

### Using Android Studio

1.  Open the `benchmark` module in the project view.
2.  Navigate to the benchmark class you want to run (usually in `src/main/kotlin`).
3.  Click the green run icon next to the class or test method.
4.  Select "Run '...'" to execute the benchmark on a connected device.

**Note:** For accurate results, run benchmarks on a physical device. Emulators will not provide
consistent performance metrics.

## Documentation

For more information on writing and running Macrobenchmarks, refer to the official documentation:
*   [Macrobenchmark Overview](https://developer.android.com/topic/performance/benchmarking/macrobenchmark-overview)
*   [Write a Macrobenchmark](https://developer.android.com/topic/performance/benchmarking/macrobenchmark-write)
