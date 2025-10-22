package com.example.securetrack;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TrustedContactsAdapter extends RecyclerView.Adapter<TrustedContactsAdapter.TrustedContactViewHolder> {

    private final Context context;
    private final String currentUserId;
    private final FirebaseFirestore db;
    private final FirebaseFunctions functions;

    private final List<TrustedContact> contacts = new ArrayList<>();
    private ListenerRegistration registration;

    public TrustedContactsAdapter(Context context, String currentUserId) {
        this.context = context;
        this.currentUserId = currentUserId;
        this.db = FirebaseFirestore.getInstance();
        this.functions = FirebaseFunctions.getInstance();
    }

    public void startListening() {
        stopListening();

        registration = db.collection("users")
                .document(currentUserId)
                .collection("trustedContacts")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Toast.makeText(context, "Failed to load contacts: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            return;
                        }
                        contacts.clear();
                        if (snapshots != null) {
                            for (DocumentSnapshot doc : snapshots.getDocuments()) {
                                TrustedContact contact = TrustedContact.from(doc);
                                if (contact != null) {
                                    contacts.add(contact);
                                }
                            }
                        }
                        notifyDataSetChanged();
                    }
                });
    }

    public void stopListening() {
        if (registration != null) {
            registration.remove();
            registration = null;
        }
        contacts.clear();
        notifyDataSetChanged();
    }

    @Override
    public TrustedContactViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_trusted_contact, parent, false);
        return new TrustedContactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(TrustedContactViewHolder holder, int position) {
        final TrustedContact contact = contacts.get(position);
        holder.nameTextView.setText(contact.name != null ? contact.name : "Unknown");

        holder.ringAlarmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ringAlarm(contact.uid);
            }
        });
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    private void ringAlarm(String targetUserId) {
        if (targetUserId == null || targetUserId.isEmpty()) {
            Toast.makeText(context, "Invalid contact UID", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("targetUserId", targetUserId);

        functions
                .getHttpsCallable("sendAlarm")
                .call(data)
                .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<HttpsCallableResult>() {
                    @Override
                    public void onSuccess(HttpsCallableResult httpsCallableResult) {
                        Toast.makeText(context, "Alarm sent", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(context, "Failed to send alarm: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    static class TrustedContactViewHolder extends RecyclerView.ViewHolder {
        final TextView nameTextView;
        final Button ringAlarmButton;

        TrustedContactViewHolder(View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.tv_contact_name);
            ringAlarmButton = itemView.findViewById(R.id.btn_ring_alarm);
        }
    }

    static class TrustedContact {
        final String uid;
        final String name;

        TrustedContact(String uid, String name) {
            this.uid = uid;
            this.name = name;
        }

        static @Nullable TrustedContact from(DocumentSnapshot doc) {
            if (doc == null) return null;
            // Prefer the explicit field; fall back to alternative or document ID
            String uid = doc.getString("uid");
            if (uid == null || uid.isEmpty()) {
                uid = doc.getString("userId"); // in case other code writes "userId"
            }
            if (uid == null || uid.isEmpty()) {
                uid = doc.getId();
            }
            String name = doc.getString("name");
            return new TrustedContact(uid, name);
        }
    }
}