package com.example.securetrack;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for handling invitation codes and trusted contacts management
 */
public class InvitationUtils {
    
    // Characters used for generating invitation codes (alphanumeric only)
    private static final String ALPHANUMERIC_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    
    /**
     * Generates a secure, random, 8-character alphanumeric invitation code.
     * Uses SecureRandom for cryptographically strong random number generation.
     * 
     * @return A randomly generated 8-character alphanumeric string
     */
    public static String generateInvitationCode() {
        StringBuilder invitationCode = new StringBuilder(8);
        
        for (int i = 0; i < 8; i++) {
            int randomIndex = SECURE_RANDOM.nextInt(ALPHANUMERIC_CHARS.length());
            invitationCode.append(ALPHANUMERIC_CHARS.charAt(randomIndex));
        }
        
        return invitationCode.toString();
    }
    
    /**
     * Adds a new trusted contact to the user's trustedContacts sub-collection in Firestore.
     * 
     * @param userId The UID of the user adding the trusted contact
     * @param trustedContactUid The UID of the trusted contact being added
     * @param trustedContactName The name of the trusted contact
     * @param onSuccessListener Callback for successful addition
     * @param onFailureListener Callback for failed addition
     */
    public static void addTrustedContact(String userId, String trustedContactUid, 
                                       String trustedContactName,
                                       OnSuccessListener<Void> onSuccessListener,
                                       OnFailureListener onFailureListener) {
        
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        // Create a new document for the trusted contact
        Map<String, Object> trustedContact = new HashMap<>();
        trustedContact.put("uid", trustedContactUid);
        trustedContact.put("name", trustedContactName);
        trustedContact.put("addedAt", System.currentTimeMillis()); // Timestamp when added
        
        // Add to the sub-collection: /users/{userId}/trustedContacts/{trustedContactUid}
        db.collection("users")
          .document(userId)
          .collection("trustedContacts")
          .document(trustedContactUid)
          .set(trustedContact)
          .addOnSuccessListener(onSuccessListener)
          .addOnFailureListener(onFailureListener);
    }
    
    /**
     * Adds a new trusted contact to the current user's trustedContacts sub-collection.
     * Uses FirebaseAuth to get the current user's UID.
     * 
     * @param trustedContactUid The UID of the trusted contact being added
     * @param trustedContactName The name of the trusted contact
     * @param onSuccessListener Callback for successful addition
     * @param onFailureListener Callback for failed addition
     */
    public static void addTrustedContactForCurrentUser(String trustedContactUid, 
                                                     String trustedContactName,
                                                     OnSuccessListener<Void> onSuccessListener,
                                                     OnFailureListener onFailureListener) {
        
        FirebaseAuth auth = FirebaseAuth.getInstance();
        
        if (auth.getCurrentUser() != null) {
            String currentUserId = auth.getCurrentUser().getUid();
            addTrustedContact(currentUserId, trustedContactUid, trustedContactName, 
                            onSuccessListener, onFailureListener);
        } else {
            onFailureListener.onFailure(new Exception("No authenticated user found"));
        }
    }
    
    /**
     * Removes a trusted contact from the user's trustedContacts sub-collection.
     * 
     * @param userId The UID of the user removing the trusted contact
     * @param trustedContactUid The UID of the trusted contact to remove
     * @param onSuccessListener Callback for successful removal
     * @param onFailureListener Callback for failed removal
     */
    public static void removeTrustedContact(String userId, String trustedContactUid,
                                          OnSuccessListener<Void> onSuccessListener,
                                          OnFailureListener onFailureListener) {
        
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        db.collection("users")
          .document(userId)
          .collection("trustedContacts")
          .document(trustedContactUid)
          .delete()
          .addOnSuccessListener(onSuccessListener)
          .addOnFailureListener(onFailureListener);
    }
    
    /**
     * Generates multiple unique invitation codes.
     * Useful for bulk generation scenarios.
     * 
     * @param count The number of invitation codes to generate
     * @return Array of unique invitation codes
     */
    public static String[] generateMultipleInvitationCodes(int count) {
        String[] codes = new String[count];
        
        for (int i = 0; i < count; i++) {
            codes[i] = generateInvitationCode();
        }
        
        return codes;
    }
}
