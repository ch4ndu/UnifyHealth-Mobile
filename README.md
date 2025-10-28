# SparkyFitness-Mobile

**SparkyFitness-Mobile** is a Compose Multiplatform application for iOS and Android designed to collect health and fitness data from your device and upload it to your preferred services. As long as the target service supports our specified API format, you can integrate it with SparkyFitness-Mobile.

## Key Features

*   **Cross-Platform:** Built with Compose Multiplatform, supporting both Android and iOS with a shared codebase.
*   **Comprehensive Data Collection:** Gathers a wide range of health metrics from the underlying platform (HealthKit on iOS, Health Connect on Android).
*   **Flexible Data Export:** Allows users to configure and upload their health data to any service that implements the SparkyFitness-Mobile API format.
*   **User-Centric:** Puts you in control of where your health data goes.

## Supported Health Data Types

SparkyFitness-Mobile can collect the following types of health data (availability may vary by platform and user permissions):

*   **Activity:**
    *   Steps
    *   Distance
    *   Floors Climbed
    *   Active Energy Burned
    *   Basal Energy Burned
    *   Move Minutes
    *   Exercise
*   **Body Measurements:**
    *   Weight
    *   Height
    *   Body Fat Percentage
    *   Lean Body Mass
    *   Body Mass Index
    *   Body Temperature
*   **Vitals:**
    *   Heart Rate
    *   Heart Rate Variability
    *   Blood Pressure
    *   Blood Glucose
    *   Oxygen Saturation
    *   Respiratory Rate
    *   VO2 Max
*   **Sleep & Nutrition:**
    *   Sleep
    *   Water
    *   Nutrition
*   **Cycle Tracking:**
    *   Menstruation
    *   Ovulation Test
    *   Cervical Mucus
    *   Sexual Activity
    *   Intermenstrual Bleeding


## How It Works

1.  **Data Collection:** The app requests permissions to access health data from the respective platform APIs (HealthKit on iOS, Health Connect on Android).
2.  **User Configuration:** Users can specify the endpoint and credentials for the service(s) they wish to upload data to.
3.  **Data Upload:** The app periodically (or manually, based on settings) sends the collected data to the configured services using a defined API format.
4.  **Privacy:** Data is handled locally on the device until the user initiates an upload to their chosen service.


## API Specification

The application uploads health data to a backend service. For step count data, the app expects a dedicated endpoint (e.g., `/api/data/steps` or similar) that accepts a `POST` request with the following JSON payload.

<details>
<summary>Sample JSON Format for Steps Data API</summary>

This JSON payload is expected when uploading steps data.
- The `dataType` ("steps") is inferred from the API endpoint used.

The `metadata` field can contain additional information associated with the step records. See comments below for details on each field:
```json
{
  "records": [
    {
      "startTimestamp": "2024-05-21T00:00:00Z", // ISO 8601 timestamp for the beginning of the period
      "endTimestamp": "2024-05-21T23:59:59Z",   // ISO 8601 timestamp for the end of the period
      "value": 7500,                     // The number of steps
      "unit": "count",
      "metadata": {                       // All fields within metadata are optional; services should handle their absence.
                                          // This object can be extended with other relevant key-value pairs.
        "manualEntry": false,             // Was this data manually entered by the user? (Optional)
        "distance": 5.25,                 // Distance covered during these steps. (Optional)
        "distanceUnit": "km",             // Unit for distance ("km", "mi", "meters"). (Required if distance is present)
        "activeEnergyBurned": 150.5,      // Active calories burned during these steps. (Optional)
        "activeEnergyBurnedUnit": "kcal", // Unit for energy ("kcal", "kJ"). (Required if activeEnergyBurned is present)
        "activeDurationMinutes": 65,      // Duration of active movement for these steps. (Optional)
        "floorsClimbed": 3,               // Floors climbed, if available and associated. (Optional)
        "description": "Afternoon walk in the park" // User-added note or context. (Optional)
      }
    }
  ],
  "batchUploadTimestamp": "2024-05-23T10:30:00Z" // Timestamp for when this batch was prepared/uploaded.
}
```
**Important Notes for Service Providers:**
*   **Content-Type:** The request should use `Content-Type: application/json`.
*   **Error Handling:** Service providers should define how errors are communicated (e.g., standard HTTP status codes and error messages in the response body).
*   **Optional Fields:** Services should gracefully handle missing optional fields within `metadata`.

</details>


## Supported Platforms

*   **Android**
*   **iOS**

## Getting Started

### Build Instructions

1.  Clone the repository:
    ```bash
    git clone https://github.com/your-username/SparkyFitness-Mobile.git
    cd SparkyFitness-Mobile
    ```
2.  Open the project in Android Studio.
3.  Build the project using Gradle:
    *   For Android: `./gradlew assembleDebug` (or `assembleRelease`)
    *   For iOS: Open the `iosApp` in Xcode(16.0) and build, or use Gradle tasks like `./gradlew :composeApp:iosX64Binaries` (adjust for your specific build needs).


## Contributing

Contributions are welcome! If you'd like to contribute to SparkyFitness-Mobile, please follow these general guidelines:

1.  **Fork the Repository:** Start by forking the main repository to your own GitHub account.
2.  **Create a Branch:** For any new feature or bug fix, create a new branch from `main` (or the relevant development branch) in your fork.
    *   Example: `git checkout -b feature/my-new-feature` or `git checkout -b fix/issue-tracker-bug-fix`
3.  **Make Changes:** Implement your feature or fix the bug. Ensure your code adheres to the project's coding style and conventions.
4.  **Test Your Changes:** Thoroughly test your changes to ensure they work as expected and don't introduce regressions.
5.  **Commit Your Changes:** Write clear and concise commit messages.
6.  **Push to Your Fork:** Push your changes to your forked repository.
    *   Example: `git push origin feature/my-new-feature`
7.  **Submit a Pull Request (PR):** Open a pull request from your feature/bugfix branch in your fork to the `main` branch (or the designated development branch) of the original SparkyFitness-Mobile repository.
    *   Provide a clear title and description for your PR, explaining the changes and why they are being made.
    *   Reference any relevant issues.



## License

SparkyFitness-Mobile is released under the MIT License.

Copyright (c) [YEAR] [COPYRIGHT_HOLDER_NAME]

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
