package com.ensias.essudatingapp;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MatchAdapter extends RecyclerView.Adapter<MatchAdapter.MatchViewHolder> {

    private Context context;
    private List<Match> matches;
    private OnMatchClickListener listener;

    public interface OnMatchClickListener {
        void onMatchClick(int position);
    }

    public MatchAdapter(Context context, List<Match> matches, OnMatchClickListener listener) {
        this.context = context;
        this.matches = matches;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MatchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_match, parent, false);
        return new MatchViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MatchViewHolder holder, int position) {
        Match match = matches.get(position);
        User otherUser = match.getOtherUser();

        // Set user info
        holder.nameTextView.setText(otherUser.getFirstName() + " " + otherUser.getLastName());

        // Set profile image
        if (otherUser.getProfileImage() != null && !otherUser.getProfileImage().isEmpty()) {
            Glide.with(context)
                    .load(otherUser.getProfileImage())
                    .placeholder(R.drawable.default_profile)
                    .into(holder.profileImageView);
        } else {
            holder.profileImageView.setImageResource(R.drawable.default_profile);
        }

        // Set last message
        if (match.getLastMessage() != null) {
            holder.lastMessageTextView.setText(match.getLastMessage());

            // Format timestamp
            if (match.getLastMessageTimestamp() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
                String formattedDate = sdf.format(new Date(match.getLastMessageTimestamp()));
                holder.timestampTextView.setText(formattedDate);
                holder.timestampTextView.setVisibility(View.VISIBLE);
            } else {
                holder.timestampTextView.setVisibility(View.GONE);
            }
        } else {
            holder.lastMessageTextView.setText("No messages yet. Start chatting!");
            holder.timestampTextView.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return matches.size();
    }

    public class MatchViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        ImageView profileImageView;
        TextView nameTextView, lastMessageTextView, timestampTextView;

        public MatchViewHolder(@NonNull View itemView) {
            super(itemView);

            profileImageView = itemView.findViewById(R.id.profile_image_view);
            nameTextView = itemView.findViewById(R.id.name_text_view);
            lastMessageTextView = itemView.findViewById(R.id.last_message_text_view);
            timestampTextView = itemView.findViewById(R.id.timestamp_text_view);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (listener != null) {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onMatchClick(position);
                }
            }
        }
    }
}