package matos.csu.group3.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import matos.csu.group3.R;

public class HashtagAdapter extends RecyclerView.Adapter<HashtagAdapter.HashtagViewHolder> {

    private List<String> hashtags;
    private OnHashtagRemoveListener listener;

    public interface OnHashtagRemoveListener {
        void onHashtagRemoved(int position);
    }

    public HashtagAdapter(List<String> hashtags, OnHashtagRemoveListener listener) {
        this.hashtags = hashtags;
        this.listener = listener;
    }

    @NonNull
    @Override
    public HashtagViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_hashtags, parent, false);
        return new HashtagViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HashtagViewHolder holder, int position) {
        holder.textViewHashtag.setText("#" + hashtags.get(position));

        holder.buttonRemove.setOnClickListener(v -> {
            if (listener != null) {
                listener.onHashtagRemoved(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return hashtags != null ? hashtags.size() : 0;
    }

    public void updateHashtags(List<String> newHashtags) {
        this.hashtags = newHashtags;
        notifyDataSetChanged();
    }

    static class HashtagViewHolder extends RecyclerView.ViewHolder {
        TextView textViewHashtag;
        ImageButton buttonRemove;

        HashtagViewHolder(View itemView) {
            super(itemView);
            textViewHashtag = itemView.findViewById(R.id.textViewHashtag);
            buttonRemove = itemView.findViewById(R.id.buttonRemove);
        }
    }
}