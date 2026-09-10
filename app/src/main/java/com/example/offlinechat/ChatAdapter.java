package com.example.offlinechat;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private final List<ChatMessage> messageList;

    public ChatAdapter(List<ChatMessage> messageList) {
        this.messageList = messageList;
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = messageList.get(position);
        if (message.isSentByMe()) {
            return VIEW_TYPE_SENT;
        } else {
            return VIEW_TYPE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SENT) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_sent, parent, false);
            return new SentMessageViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_received, parent, false);
            return new ReceivedMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messageList.get(position);

        if (holder instanceof SentMessageViewHolder) {
            ((SentMessageViewHolder) holder).bind(message);
        } else if (holder instanceof ReceivedMessageViewHolder) {
            ((ReceivedMessageViewHolder) holder).bind(message);
        }
    }

    @Override
    public int getItemCount() {
        return messageList != null ? messageList.size() : 0;
    }

    // ==========================================
    // SENT MESSAGE VIEWHOLDER
    // ==========================================
    static class SentMessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;
        ImageView ivImage;

        SentMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            ivImage = itemView.findViewById(R.id.ivImage);
        }

        void bind(ChatMessage message) {
            if (message.isImage()) {
                if (tvMessage != null) tvMessage.setVisibility(View.GONE);
                if (ivImage != null) {
                    ivImage.setVisibility(View.VISIBLE);
                    Glide.with(itemView.getContext())
                            .load(Uri.parse(message.getMessage()))
                            .into(ivImage);
                }
            } else {
                if (ivImage != null) ivImage.setVisibility(View.GONE);
                if (tvMessage != null) {
                    tvMessage.setVisibility(View.VISIBLE);
                    tvMessage.setText(message.getMessage());
                }
            }
        }
    }

    // ==========================================
    // RECEIVED MESSAGE VIEWHOLDER
    // ==========================================
    static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;
        ImageView ivImage;

        ReceivedMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            ivImage = itemView.findViewById(R.id.ivImage);
        }

        void bind(ChatMessage message) {
            if (message.isImage()) {
                if (tvMessage != null) tvMessage.setVisibility(View.GONE);
                if (ivImage != null) {
                    ivImage.setVisibility(View.VISIBLE);
                    Glide.with(itemView.getContext())
                            .load(Uri.parse(message.getMessage()))
                            .into(ivImage);
                }
            } else {
                if (ivImage != null) ivImage.setVisibility(View.GONE);
                if (tvMessage != null) {
                    tvMessage.setVisibility(View.VISIBLE);
                    tvMessage.setText(message.getMessage());
                }
            }
        }
    }
}