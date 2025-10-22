#!/bin/bash

# Firebase Cloud Functions Deployment Script
# This script deploys the sendAlarm function to Firebase

echo "🚀 Starting Firebase Cloud Functions deployment..."

# Check if Firebase CLI is installed
if ! command -v firebase &> /dev/null; then
    echo "❌ Firebase CLI is not installed. Please install it first:"
    echo "npm install -g firebase-tools"
    exit 1
fi

# Check if user is logged in
if ! firebase projects:list &> /dev/null; then
    echo "❌ Not logged in to Firebase. Please run: firebase login"
    exit 1
fi

# Navigate to functions directory
cd functions

# Install dependencies
echo "📦 Installing dependencies..."
npm install

# Build TypeScript
echo "🔨 Building TypeScript..."
npm run build

# Navigate back to project root
cd ..

# Deploy functions
echo "🚀 Deploying functions to Firebase..."
firebase deploy --only functions

echo "✅ Deployment complete!"
echo ""
echo "📋 Next steps:"
echo "1. Update your Android app to use the new function"
echo "2. Test the function using the Firebase Console"
echo "3. Ensure your Firestore has the correct user structure with fcmToken field"
echo ""
echo "🔗 Function URL: https://us-central1-st10102025.cloudfunctions.net/sendAlarm"
