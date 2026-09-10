package com.example.offlinechat;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private final List<ChatMessage> messageList;

    public ChatAdapter(List<ChatMessage> messageList) {
        this.messageList = messageList;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatMessage chatMessage = messageList.get(position);
        Context context = holder.itemView.getContext();

        // Layout Gravity & Background Color setup
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) holder.layoutContainer.getLayoutParams();

        if (chatMessage.isSentByMe()) {
            params.gravity = Gravity.END;
            holder.tvMessage.setBackgroundColor(ContextCompat.getColor(context, android.R.color.holo_blue_dark));
        } else {
            params.gravity = Gravity.START;
            holder.tvMessage.setBackgroundColor(ContextCompat.getColor(context, android.R.color.darker_gray));
        }
        holder.layoutContainer.setLayoutParams(params);

        // Content Display Logic
        if (chatMessage.getImageUri() != null) {
            holder.imgMessage.setVisibility(View.VISIBLE);
            holder.tvMessage.setVisibility(View.GONE);

            // Glide দিয়ে নিরাপদে ইমেজ লোড করা
            Glide.with(context)
                    .load(chatMessage.getImageUri())
                    .into(holder.imgMessage);

        } else {
            holder.tvMessage.setVisibility(View.VISIBLE);
            holder.imgMessage.setVisibility(View.GONE);
            holder.tvMessage.setText(chatMessage.getMessage());
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        LinearLayout layoutContainer;
        TextView tvMessage;
        ImageView imgMessage;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutContainer = itemView.findViewById(R.id.layoutContainer);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            imgMessage = itemView.findViewById(R.id.imgMessage);
        }
    }
}