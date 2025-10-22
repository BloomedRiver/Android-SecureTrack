package com.example.securetrack;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

/**
 * Example usage of InvitationUtils class
 * This demonstrates how to use the invitation code generation and trusted contacts functionality
 */
public class InvitationExample extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Example 1: Generate a single invitation code
        generateInvitationCodeExample();
        
        // Example 2: Add a trusted contact
        addTrustedContactExample();
        
        // Example 3: Generate multiple invitation codes
        generateMultipleCodesExample();
    }
    
    /**
     * Example of generating a single invitation code
     */
    private void generateInvitationCodeExample() {
        String invitationCode = InvitationUtils.generateInvitationCode();
        Toast.makeText(this, "Generated invitation code: " + invitationCode, Toast.LENGTH_LONG).show();
        
        // Log the code for debugging (remove in production)
        System.out.println("Generated invitation code: " + invitationCode);
    }
    
    /**
     * Example of adding a trusted contact to the current user's trustedContacts sub-collection
     */
    private void addTrustedContactExample() {
        String trustedContactUid = "example_trusted_contact_uid";
        String trustedContactName = "John Doe";
        
        InvitationUtils.addTrustedContactForCurrentUser(
            trustedContactUid, 
            trustedContactName,
            new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Toast.makeText(InvitationExample.this, 
                        "Trusted contact added successfully!", Toast.LENGTH_SHORT).show();
                }
            },
            new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(InvitationExample.this, 
                        "Failed to add trusted contact: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        );
    }
    
    /**
     * Example of generating multiple invitation codes
     */
    private void generateMultipleCodesExample() {
        String[] codes = InvitationUtils.generateMultipleInvitationCodes(5);
        
        StringBuilder codesList = new StringBuilder("Generated codes:\n");
        for (String code : codes) {
            codesList.append(code).append("\n");
        }
        
        Toast.makeText(this, codesList.toString(), Toast.LENGTH_LONG).show();
        
        // Log all codes for debugging (remove in production)
        for (String code : codes) {
            System.out.println("Generated code: " + code);
        }
    }
}
