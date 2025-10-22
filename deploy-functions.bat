@echo off
REM Firebase Cloud Functions Deployment Script for Windows
REM This script deploys the sendAlarm function to Firebase

echo 🚀 Starting Firebase Cloud Functions deployment...

REM Check if Firebase CLI is installed
firebase --version >nul 2>&1
if errorlevel 1 (
    echo ❌ Firebase CLI is not installed. Please install it first:
    echo npm install -g firebase-tools
    pause
    exit /b 1
)

REM Check if user is logged in
firebase projects:list >nul 2>&1
if errorlevel 1 (
    echo ❌ Not logged in to Firebase. Please run: firebase login
    pause
    exit /b 1
)

REM Navigate to functions directory
cd functions

REM Install dependencies
echo 📦 Installing dependencies...
npm install

REM Build TypeScript
echo 🔨 Building TypeScript...
npm run build

REM Navigate back to project root
cd ..

REM Deploy functions
echo 🚀 Deploying functions to Firebase...
firebase deploy --only functions

echo ✅ Deployment complete!
echo.
echo 📋 Next steps:
echo 1. Update your Android app to use the new function
echo 2. Test the function using the Firebase Console
echo 3. Ensure your Firestore has the correct user structure with fcmToken field
echo.
echo 🔗 Function URL: https://us-central1-st10102025.cloudfunctions.net/sendAlarm
pause
