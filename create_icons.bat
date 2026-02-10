@echo off
echo Creating app icon in different resolutions...

REM Define paths
set SOURCE=C:\Users\Madalin\.cursor\projects\c-Users-Madalin-Desktop-OnTrack\assets\ontrack_app_icon.png
set RES=c:\Users\Madalin\Desktop\OnTrack\app\src\main\res

REM Note: You need to manually resize the icon or use an online tool
REM Recommended sizes:
REM mdpi: 48x48
REM hdpi: 72x72
REM xhdpi: 96x96
REM xxhdpi: 144x144
REM xxxhdpi: 192x192

echo.
echo Icon source: %SOURCE%
echo Target directories: %RES%\mipmap-*
echo.
echo Please use one of these methods to create launcher icons:
echo 1. Android Studio: Right-click res folder -^> New -^> Image Asset
echo 2. Online tool: https://romannurik.github.io/AndroidAssetStudio/icons-launcher.html
echo 3. Or manually resize splash_icon.png to above dimensions
echo.
pause
