# ShopPay Android Studio Project

A simple offline ShopPay app wrapped in an Android WebView. The existing ShopPay page is stored in `app/src/main/assets/index.html`.

## Build APK with GitHub Actions (phone-friendly)
1. Create a new GitHub repository, for example `ShopPay-Android`.
2. Upload all files and folders from this project to the repository root (not the ZIP itself).
3. Open the **Actions** tab and enable Actions if GitHub asks.
4. Run **Build ShopPay Android APK** using **Run workflow**, or push to `main`/`master`.
5. Open the completed workflow run, scroll to **Artifacts**, and download `ShopPay-debug-apk`.
6. Extract the artifact ZIP to get `app-debug.apk`, then install it on your Android phone.

## Open in Android Studio
Open this folder as a project. Android Studio can sync using the Gradle files. The GitHub workflow installs Gradle 8.9 and Android SDK platform 35 automatically.

## Data storage
The app enables JavaScript and DOM storage in WebView. Records are stored on the device by the HTML app; uninstalling or clearing app storage may remove them.
