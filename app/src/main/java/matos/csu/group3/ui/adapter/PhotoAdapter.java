package matos.csu.group3.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

import matos.csu.group3.R;
import matos.csu.group3.data.local.entity.PhotoEntity;

public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder> {

    private List<PhotoEntity> photos;  // List of photos (no longer final)
    private final OnItemClickListener listener;  // Listener for item click events

    public PhotoAdapter(List<PhotoEntity> photos, OnItemClickListener listener) {
        this.photos = photos;
        this.listener = listener;
    }

    // Method to update the photos list
    public void setPhotos(List<PhotoEntity> newPhotos) {
        this.photos.clear();
        this.photos.addAll(newPhotos);
        notifyItemRangeInserted(0, newPhotos.size()); // Ensures smooth UI updates
    }


    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Inflate the item layout
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_photo, parent, false);
        return new PhotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(PhotoViewHolder holder, int position) {
        // Get the current photo
        PhotoEntity currentPhoto = photos.get(position);

        // Load image using Glide
        Glide.with(holder.itemView.getContext())
                .load(currentPhoto.getFilePath())  // Photo file path
                .into(holder.imageView);

        // Handle click event on the photo item
        holder.itemView.setOnClickListener(v -> listener.onItemClick(currentPhoto));
    }

    @Override
    public int getItemCount() {
        return photos.size(); // No need for null check, as photos is initialized in the constructor
    }

    // ViewHolder for individual photo items
    public static class PhotoViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageView;

        public PhotoViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.image_view);
        }
    }

    // Interface to handle item click events
    public interface OnItemClickListener {
        void onItemClick(PhotoEntity photo);
    }
}