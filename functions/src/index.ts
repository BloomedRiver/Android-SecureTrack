import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';

// Initialize Firebase Admin SDK
admin.initializeApp();

/**
 * Cloud Function to send alarm notification to a target user
 * @param {string} targetUserId - The ID of the user to send the alarm to
 * @returns {Promise<{success: boolean, messageId?: string, error?: string}>}
 */
export const sendAlarm = functions.https.onCall(async (data, context) => {
  try {
    // Validate input
    const { targetUserId } = data;
    
    if (!targetUserId) {
      throw new functions.https.HttpsError(
        'invalid-argument',
        'targetUserId is required'
      );
    }

    // Validate authentication (optional - remove if you want to allow unauthenticated calls)
    if (!context.auth) {
      throw new functions.https.HttpsError(
        'unauthenticated',
        'The function must be called while authenticated'
      );
    }

    // Get Firestore instance
    const db = admin.firestore();

    // Retrieve the target user's FCM token from Firestore
    const userDoc = await db.collection('users').doc(targetUserId).get();
    
    if (!userDoc.exists) {
      throw new functions.https.HttpsError(
        'not-found',
        `User with ID ${targetUserId} not found`
      );
    }

    const userData = userDoc.data();
    const fcmToken = userData?.fcmToken;

    if (!fcmToken) {
      throw new functions.https.HttpsError(
        'failed-precondition',
        `User ${targetUserId} does not have an FCM token`
      );
    }

    // Construct the high-priority data message
    const message = {
      token: fcmToken,
      data: {
        action: 'RING_ALARM',
        timestamp: Date.now().toString(),
        targetUserId: targetUserId
      },
      android: {
        priority: 'high',
        notification: {
          title: 'Emergency Alert',
          body: 'You have received an emergency alarm!',
          sound: 'default',
          priority: 'high'
        }
      },
      apns: {
        headers: {
          'apns-priority': '10'
        },
        payload: {
          aps: {
            alert: {
              title: 'Emergency Alert',
              body: 'You have received an emergency alarm!'
            },
            sound: 'default',
            badge: 1
          }
        }
      }
    };

    // Send the FCM message
    const response = await admin.messaging().send(message);

    console.log('Successfully sent alarm message:', response);
    
    return {
      success: true,
      messageId: response,
      targetUserId: targetUserId
    };

  } catch (error) {
    console.error('Error sending alarm:', error);
    
    if (error instanceof functions.https.HttpsError) {
      throw error;
    }
    
    throw new functions.https.HttpsError(
      'internal',
      'An error occurred while sending the alarm',
      error
    );
  }
});
