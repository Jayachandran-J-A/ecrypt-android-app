package com.cryptosecurity.echocrypt.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cryptosecurity.echocrypt.R;
import com.cryptosecurity.echocrypt.activities.ChatActivity;
import com.cryptosecurity.echocrypt.models.User;

import java.util.List;

public class ContactListAdapter extends RecyclerView.Adapter<ContactListAdapter.ContactViewHolder> {

    private final List<User> userList;
    private final Context context;

    public ContactListAdapter(Context context, List<User> userList) {
        this.context = context;
        this.userList = userList;
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the layout for a single contact item
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_contact, parent, false);
        return new ContactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        // Get the user at the current position
        User user = userList.get(position);
        // Set the user's email in the TextView
        holder.textViewEmail.setText(user.getEmail());

        // Set a click listener for the entire item view
        holder.itemView.setOnClickListener(v -> {
            // When a contact is clicked, open the ChatActivity
            Intent intent = new Intent(context, ChatActivity.class);
            // Pass the details of the selected user to the ChatActivity
            intent.putExtra("USER_ID", user.getUid());
            intent.putExtra("USER_EMAIL", user.getEmail());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    // The ViewHolder class holds the UI elements for a single list item
    static class ContactViewHolder extends RecyclerView.ViewHolder {
        TextView textViewEmail;

        ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewEmail = itemView.findViewById(R.id.textViewEmail);
        }
    }
}
