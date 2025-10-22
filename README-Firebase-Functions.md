# SecureTrack - Firebase Cloud Functions Integration

This project now includes Firebase Cloud Functions for sending alarm notifications via FCM (Firebase Cloud Messaging).

## 🚀 Quick Start

### Prerequisites
- Node.js 18+ installed
- Firebase CLI installed: `npm install -g firebase-tools`
- Firebase project set up (already configured for `st10102025`)

### Deployment
1. **Login to Firebase**: `firebase login`
2. **Deploy Functions**: Run `deploy-functions.sh` (Linux/Mac) or `deploy-functions.bat` (Windows)
3. **Or manually**:
   ```bash
   cd functions
   npm install
   npm run build
   cd ..
   firebase deploy --only functions
   ```

## 📱 Android Integration

### New Dependencies Added
- `firebase-messaging`: For receiving FCM notifications
- `firebase-functions`: For calling Cloud Functions

### New Files Created
- `MyFirebaseMessagingService.java`: Handles incoming FCM messages
- `AlarmUtils.java`: Utility class for calling the sendAlarm function

### Usage Example
```java
// Send alarm to a user
AlarmUtils.sendAlarm("targetUserId123", new AlarmUtils.AlarmCallback() {
    @Override
    public void onSuccess(String messageId) {
        Log.d("Alarm", "Alarm sent successfully: " + messageId);
    }
    
    @Override
    public void onFailure(Exception exception) {
        Log.e("Alarm", "Failed to send alarm", exception);
    }
});
```

## 🔧 Cloud Function Details

### sendAlarm Function
- **Type**: Callable HTTPS function
- **Parameters**: `targetUserId` (string, required)
- **Authentication**: Required (can be disabled if needed)
- **Returns**: Success status and message ID

### Function Flow
1. Validates input parameters
2. Checks user authentication
3. Retrieves target user's FCM token from Firestore
4. Constructs high-priority FCM message with `action: 'RING_ALARM'`
5. Sends notification via FCM Admin SDK
6. Returns success/failure response

## 🗄️ Firestore Structure Required

Your Firestore should have users stored with this structure:
```
users/{userId}
  - fcmToken: string (required for FCM notifications)
  - email: string
  - name: string
  - ... other user fields
```

## 🔐 Security Features

- **Authentication Required**: Only authenticated users can call the function
- **Input Validation**: Validates required parameters
- **Error Handling**: Comprehensive error handling with proper HTTP status codes
- **Firestore Rules**: Secure rules for user data access

## 📋 Testing

### Test the Function
1. **Firebase Console**: Go to Functions tab and test with sample data
2. **Android App**: Use the `AlarmUtils.sendAlarm()` method
3. **cURL** (for testing):
   ```bash
   curl -X POST https://us-central1-st10102025.cloudfunctions.net/sendAlarm \
     -H "Content-Type: application/json" \
     -d '{"data": {"targetUserId": "testUserId"}}'
   ```

### Test FCM Reception
1. Ensure the target user has a valid FCM token in Firestore
2. Call the function with a valid user ID
3. Check device for notification
4. Check logs for any errors

## 🛠️ Troubleshooting

### Common Issues
1. **Function not found**: Ensure deployment completed successfully
2. **Authentication errors**: Check Firebase Auth setup
3. **FCM token missing**: Ensure user document has `fcmToken` field
4. **Notification not received**: Check device FCM registration

### Debug Steps
1. Check Firebase Console logs
2. Verify Firestore user document structure
3. Test FCM token validity
4. Check Android app FCM service registration

## 📚 Additional Resources

- [Firebase Cloud Functions Documentation](https://firebase.google.com/docs/functions)
- [FCM Documentation](https://firebase.google.com/docs/cloud-messaging)
- [Firestore Security Rules](https://firebase.google.com/docs/firestore/security/get-started)

## 🔄 Next Steps

1. **Deploy the functions** using the provided scripts
2. **Test the integration** with your Android app
3. **Customize the notification** appearance and behavior
4. **Add additional alarm types** if needed
5. **Implement user management** for FCM token updates
