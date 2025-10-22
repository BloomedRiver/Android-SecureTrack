# Firebase Cloud Functions - SecureTrack

This directory contains Firebase Cloud Functions for the SecureTrack application.

## Functions

### sendAlarm
Sends a high-priority FCM notification to a target user with an alarm action.

**Parameters:**
- `targetUserId` (string, required): The ID of the user to send the alarm to

**Returns:**
- `success` (boolean): Whether the operation was successful
- `messageId` (string): The FCM message ID if successful
- `error` (string): Error message if failed

## Setup and Deployment

### Prerequisites
1. Install Node.js (version 18 or higher)
2. Install Firebase CLI: `npm install -g firebase-tools`
3. Login to Firebase: `firebase login`

### Installation
1. Navigate to the functions directory: `cd functions`
2. Install dependencies: `npm install`

### Deployment
1. From the project root directory, run: `firebase deploy --only functions`
2. The function will be deployed to your Firebase project

### Local Testing
1. Install Firebase emulator: `firebase init emulators`
2. Start the emulator: `firebase emulators:start --only functions`
3. Test the function locally using the Firebase Functions emulator

## Usage from Android App

```java
// Get Firebase Functions instance
FirebaseFunctions functions = FirebaseFunctions.getInstance();

// Create the callable function
Task<String> task = functions.getHttpsCallable("sendAlarm")
    .call(Map.of("targetUserId", "user123"))
    .continueWith(task -> {
        if (task.isSuccessful()) {
            Map<String, Object> result = (Map<String, Object>) task.getResult().getData();
            return (String) result.get("messageId");
        } else {
            throw task.getException();
        }
    });
```

## Firestore Structure

The function expects users to be stored in Firestore with the following structure:

```
users/{userId}
  - fcmToken: string (required)
  - ... other user data
```

## Security

- The function requires authentication by default
- Users can only send alarms to other users
- FCM tokens are retrieved securely from Firestore
- All errors are properly handled and logged
