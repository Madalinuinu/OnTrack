# OnTrack App Icon Setup

## Current Status
✅ Splash screen created with animation
✅ Icon saved at: `app/src/main/res/drawable/splash_icon.png`

## To Create App Launcher Icon

### Option 1: Android Studio (Recommended)
1. Open project in Android Studio
2. Right-click on `res` folder
3. Select **New → Image Asset**
4. Choose "Launcher Icons (Adaptive and Legacy)"
5. Select the source image: `C:\Users\Madalin\.cursor\projects\c-Users-Madalin-Desktop-OnTrack\assets\ontrack_app_icon.png`
6. Click "Next" and "Finish"

### Option 2: Online Tool
1. Visit: https://romannurik.github.io/AndroidAssetStudio/icons-launcher.html
2. Upload: `ontrack_app_icon.png`
3. Download the generated zip
4. Extract to `app/src/main/res/` folder

### Required Sizes
- **mdpi**: 48×48px
- **hdpi**: 72×72px
- **xhdpi**: 96×96px
- **xxhdpi**: 144×144px
- **xxxhdpi**: 192×192px

## Splash Screen
The splash screen is already configured with:
- Icon fade-in + scale animation (800ms)
- Text "OnTrack" fade-in with slide up (800ms, delayed 400ms)
- Total duration: ~2 seconds
- Professional black background
