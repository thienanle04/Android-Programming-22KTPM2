package matos.csu.group3.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.util.List;

import matos.csu.group3.R;
import matos.csu.group3.data.local.entity.PhotoEntity;
import matos.csu.group3.viewmodel.PhotoViewModel;

public class PhotoPagerAdapter extends RecyclerView.Adapter<PhotoPagerAdapter.PhotoViewHolder> {

    private List<Integer> photoIds;
    private PhotoViewModel photoViewModel;
    private LifecycleOwner lifecycleOwner;

    public PhotoPagerAdapter(List<Integer> photoIds, PhotoViewModel photoViewModel, LifecycleOwner lifecycleOwner) {
        this.photoIds = photoIds;
        this.photoViewModel = photoViewModel;
        this.lifecycleOwner = lifecycleOwner;
    }

    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_photo_single, parent, false);
        return new PhotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
        if (photoIds == null || position < 0 || position >= photoIds.size()) {
            return;
        }

        int photoId = photoIds.get(position);

        // Load ảnh từ ViewModel khi cần hiển thị
        photoViewModel.getPhotoById(photoId).observe(lifecycleOwner, photo -> {
            if (photo != null && holder.imageView != null) {
                Glide.with(holder.itemView.getContext())
                        .load(new File(photo.getFilePath()))
                        .into(holder.imageView);
            }
        });
    }

    @Override
    public int getItemCount() {
        return photoIds != null ? photoIds.size() : 0;
    }

    public void updatePhotoIds(List<Integer> newPhotoIds) {
        this.photoIds = newPhotoIds;
        notifyDataSetChanged();
    }

    static class PhotoViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.image_view);
        }
    }
}