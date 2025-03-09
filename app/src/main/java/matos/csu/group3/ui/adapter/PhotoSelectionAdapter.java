package matos.csu.group3.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

import matos.csu.group3.R;
import matos.csu.group3.data.local.entity.ListItem;
import matos.csu.group3.data.local.entity.PhotoEntity;

public class PhotoSelectionAdapter extends RecyclerView.Adapter<PhotoSelectionAdapter.PhotoViewHolder> {

    private List<PhotoEntity> photoList;
    private final OnPhotoSelectedListener listener;

    public PhotoSelectionAdapter(List<PhotoEntity> photoList, OnPhotoSelectedListener listener) {
        this.photoList = photoList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_photo_selection, parent, false);
        return new PhotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
        PhotoEntity photo = photoList.get(position);
        holder.bind(photo, listener);
    }

    @Override
    public int getItemCount() {
        if(photoList != null)
            return photoList.size();
        else
            return 0;
    }

    static class PhotoViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageView;
        private final CheckBox checkBox;

        public PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
            checkBox = itemView.findViewById(R.id.checkBox);
        }

        public void bind(PhotoEntity photo, OnPhotoSelectedListener listener) {
            // Load hình ảnh bằng Glide
            Glide.with(itemView.getContext())
                    .load(photo.getFilePath())
                    .into(imageView);

            // Xử lý sự kiện chọn hình ảnh
            checkBox.setOnCheckedChangeListener(null); // Đảm bảo không có sự kiện cũ
            checkBox.setChecked(photo.isSelected());

            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                photo.setSelected(isChecked);
                listener.onPhotoSelected(photo, isChecked);
            });
        }
    }

    public interface OnPhotoSelectedListener {
        void onPhotoSelected(PhotoEntity photo, boolean isSelected);
    }
    public void updateData(List<PhotoEntity> newItems) {
        this.photoList = newItems;
        notifyDataSetChanged();
    }
}
