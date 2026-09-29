# DepthLock – AI Depth Effect Wallpaper & Lock Screen

DepthLock is a complete, native Android application built with **Jetpack Compose**, **Material 3**, and on-device **AI Computer Vision** that creates an authentic iOS-style depth effect for wallpapers. Users can select a custom photograph, isolate a foreground subject, and compose a wallpaper with a clock and subject layers. Android's native lock-screen clock is controlled by the system; a normal wallpaper app cannot place its image layer above that clock. The editor can preview a custom clock composition, but the native lock screen may render its own clock over the wallpaper.

---

## 🎯 Target Platform & Hardware Profile
- **Target Device**: Motorola Moto G85 5G (calibrated for 1080 × 2400 20:9 OLED display)
- **Target Android Version**: Android 16 (API Level 36)
- **Minimum Android Version**: Android 10 (API Level 29)
- **Package Name**: `com.aditya.depthlock`
- **Architecture**: MVVM with Repository Pattern, Room local database, StateFlow, Kotlin Coroutines

---

## 🚀 Key Features

### 1. Custom Image Import & Framing
- **Android Photo Picker**: Zero-permission media selection compliant with Google Play policy (`PickVisualMedia`).
- **Framing & Aspect Ratio**: Tailored 20:9 phone frame preview with pinch-to-zoom, pan, 90° rotation, and real-time clock overlap placement guides.
- **Ready Presets**: Pre-seeded depth scenes (*Golden Peak Hiker*, *Neon Skyline Wanderer*) for immediate testing without needing a gallery photo.

### 2. On-Device AI Depth Segmentation (`ImageSegmentationHelper.kt`)
- **Tri-Engine Computer Vision Architecture**:
  1. **Google MediaPipe Tasks Vision (`ImageSegmenter`)**: Hardware-accelerated inference supporting DeepLabV3 (20+ categories: person, dog, cat, bird, car, horse, chair, etc.) and Selfie Multiclass (face, hair, body, clothes).
  2. **Google ML Kit Selfie Segmentation**: High-precision neural network running locally on device for humans and portraits.
  3. **Adaptive Saliency Segmentation Engine**: Edge-aware, center-weighted, and border contrast color-clustering detector for pets, cars, monuments, plants, and objects when no human is detected.
- **Model Download & Bundling**:
  - Download official MediaPipe models:
    - *DeepLabV3*: `https://storage.googleapis.com/mediapipe-models/image_segmenter/deeplab_v3/float32/1/deeplab_v3.tflite`
    - *Selfie Multiclass*: `https://storage.googleapis.com/mediapipe-models/image_segmenter/selfie_multiclass_256x256/float32/latest/selfie_multiclass_256x256.tflite`
  - Place `.tflite` files into `app/src/main/assets/models/` for automatic detection.
  - Automatic fallback ensures the app operates 100% offline out-of-the-box even without external downloads.
- **Manual Mask Refinement Tool**:
  - Interactive touch canvas with Paint (draw foreground) and Eraser (remove background) brush modes.
  - Brush radius slider (8px to 80px).
  - Mask Invert tool.
  - Full Undo / Redo history stack.
  - "Retry AI" and "Auto-Smooth Edges" operations.

### 3. iOS-Style Depth Wallpaper Editor (`WallpaperEditorScreen.kt`)
- **3D Depth Overlap**: Subject is layered in front of the clock while the remaining background layer sits behind it.
- **Depth Shadow**: Realistic soft Gaussian drop shadow cast by the foreground subject onto the clock digits.
- **Comprehensive Clock Customization**:
  - **7 Font Typography Styles**: Modern Round, Ultra Heavy, Serif Elegant, Tech Thin, Cyber Mono, Compact Bold, Stacked Poster (with Android typography fallbacks).
  - **Font Weights**: Thin (100), Regular (400), Medium (500), Bold (700), Heavy (900).
  - **Dynamic Size Slider**: 60sp to 140sp with real-time text scaling.
  - **Precise 2D Positioning**: Interactive direct dragging on canvas, vertical height slider (-150dp to +150dp), horizontal slider (-100dp to +100dp), and quick alignment buttons (Left, Center, Right).
  - **Material Design Color Picker**: Curated Material 3 color chips plus custom color picker with Hue, Saturation, Value, and Hex input.
  - **Readability & Aesthetics**: Built-in drop shadow layer, backdrop blur (0px to 25px), and dimming slider (0% to 65%) to ensure clock legibility against any background.
- **Date & Widgets**:
  - Customizable date formats (`EEE, d MMM`, `d MMMM`, etc.).
  - Position: Above Clock or Below Clock.
  - Lock screen widgets: Weather (☀️ 72° Sunny), Battery (🔋 88%), Calendar (📅 2:30 PM), Steps (👟 6,420).
- **Backdrop Controls**:
  - Background Gaussian blur slider (0px to 25px).
  - Background dimming slider (0% to 65%) to enhance clock legibility.
  - Subject scale (0.7x to 1.5x) and drag-to-reposition.

### 4. Wallpaper Export & Android System Integration
- **High-Resolution Compositor**: Renders the complete 3-layer composition (background, clock, subject, and shadow) at 1080 × 2400 device resolution.
- **Native WallpaperManager API**:
  - Apply to **Lock Screen** (`FLAG_LOCK`).
  - Apply to **Home Screen** (`FLAG_SYSTEM`).
  - Apply to **Both Screens** (`FLAG_SYSTEM or FLAG_LOCK`).
- **Gallery Export**: Saves high-resolution PNG to device gallery under `Pictures/DepthLock` using Android's Scoped Storage `MediaStore` API.
- **Room Database Persistence**: Saves depth wallpaper configurations, allowing users to re-open, edit, or delete projects anytime.

### 5. Custom Lock Screen Simulation Mode
- An interactive fullscreen lock screen simulation inside the app.
- Dynamic ticking clock, lock icon, simulated flashlight and camera quick shortcuts, and "Swipe up to unlock" affordance.
- Clearly distinguished as a preview mode adhering to Android platform security guidelines.

---

## 🔒 Privacy & Performance
- **On-device processing goal**: Segmentation and compositing are intended to run locally. Confirm the selected segmentation model and fallback path are available before relying on offline operation.
- **Zero Data Collection**: No analytics SDKs, trackers, or external API keys required.
- **Memory Optimized**: Employs bitmap downsampling during editing to avoid OutOfMemory (OOM) errors on mid-range devices (such as Motorola Moto G85 5G), rendering at full resolution only during final export.

---

## 🛠️ Build & Installation

### Build with Gradle
```bash
./gradlew :app:assembleDebug
```
The output APK will be generated at:
```
app/build/outputs/apk/debug/app-debug.apk
```

### GitHub Actions CI/CD
A GitHub Actions workflow is pre-configured at `.github/workflows/build.yml` to automatically build and publish debug APK artifacts on push and pull requests.
