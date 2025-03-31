package matos.csu.group3.ui.adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import matos.csu.group3.R;

public class HashtagChecklistAdapter extends RecyclerView.Adapter<HashtagChecklistAdapter.ViewHolder> {
    private final List<String> hashtags;
    private final Set<String> selectedHashtags;
    private final OnHashtagSelectionChanged listener;

    public interface OnHashtagSelectionChanged {
        void onSelectionChanged(Set<String> newSelections);
    }

    public HashtagChecklistAdapter(List<String> hashtags, OnHashtagSelectionChanged listener) {
        this.hashtags = hashtags != null ? hashtags : new ArrayList<>();
        this.selectedHashtags = new HashSet<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate a custom item layout with a CheckBox, or create a CheckBox programmatically
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_hashtags_checkbox, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String hashtag = hashtags.get(position);
        holder.checkBox.setText(hashtag);
        holder.checkBox.setChecked(selectedHashtags.contains(hashtag));

        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedHashtags.add(hashtag);
            } else {
                selectedHashtags.remove(hashtag);
            }
            listener.onSelectionChanged(selectedHashtags);
        });
    }

    @Override
    public int getItemCount() {
        return hashtags.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        CheckBox checkBox;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.cbHashtag); // see item_hashtag_checkbox.xml
        }
    }

    public void updateData(List<String> newHashtags) {
        hashtags.clear();
        if (newHashtags != null) {
            hashtags.addAll(newHashtags);
        }
        notifyDataSetChanged();
    }
}
