package matos.csu.group3.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

import matos.csu.group3.R;
import matos.csu.group3.data.local.entity.PhotoEntity;

public class PhotoSelectionAdapter extends RecyclerView.Adapter<PhotoSelectionAdapter.PhotoViewHolder> {

    private List<PhotoEntity> photoList;
    private final OnPhotoSelectedListener listener;
    private OnLongPressListener longPressListener;
    private OnSelectionChangeListener selectionChangeListener;
    private boolean isSelectionMode = false;
    private boolean showCheckBox;

    public PhotoSelectionAdapter(List<PhotoEntity> photoList, OnPhotoSelectedListener listener, boolean showCheckBox) {
        this.photoList = photoList != null ? photoList : new ArrayList<>();
        this.listener = listener;
        this.showCheckBox = showCheckBox;
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

        // Xử lý sự kiện long press
        holder.itemView.setOnLongClickListener(v -> {
            isSelectionMode = true; // Bật chế độ chọn ảnh
            notifyDataSetChanged(); // Cập nhật giao diện
            if (longPressListener != null) {
                longPressListener.onLongPress(); // Gọi callback
            }
            return true;
        });

        // Hiển thị/ẩn CheckBox dựa trên trạng thái chọn ảnh
        holder.checkBox.setVisibility(showCheckBox || isSelectionMode ? View.VISIBLE : View.GONE);
        holder.checkBox.setChecked(photo.isSelected());
        // Xử lý sự kiện khi CheckBox được chọn
        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            photo.setSelected(isChecked);
            listener.onPhotoSelected(photo, isChecked);
            if (selectionChangeListener != null) {
                selectionChangeListener.onSelectionChange(); // Gọi callback khi trạng thái chọn thay đổi
            }
        });
    }

    @Override
    public int getItemCount() {
        return photoList.size();
    }

    // Chọn tất cả hoặc bỏ chọn tất cả ảnh
    public void selectAll(boolean isSelected) {
        for (PhotoEntity photo : photoList) {
            photo.setSelected(isSelected);
        }
        notifyDataSetChanged(); // Cập nhật giao diện
        if (selectionChangeListener != null) {
            selectionChangeListener.onSelectionChange(); // Gọi callback
        }
    }

    // Kiểm tra xem tất cả ảnh đã được chọn chưa
    public boolean isAllSelected() {
        for (PhotoEntity photo : photoList) {
            if (!photo.isSelected()) {
                return false;
            }
        }
        return true;
    }

    // Lấy số lượng ảnh đã chọn
    public int getSelectedCount() {
        int count = 0;
        for (PhotoEntity photo : photoList) {
            if (photo.isSelected()) {
                count++;
            }
        }
        return count;
    }

    // Tắt chế độ chọn ảnh
    public void setSelectionMode(boolean isSelectionMode) {
        this.isSelectionMode = isSelectionMode;
        notifyDataSetChanged();
    }

    // Cập nhật dữ liệu
    public void updateData(List<PhotoEntity> newItems) {
        this.photoList = newItems != null ? newItems : new ArrayList<>();
        notifyDataSetChanged();
    }

    // Interface để lắng nghe sự kiện chọn ảnh
    public interface OnPhotoSelectedListener {
        void onPhotoSelected(PhotoEntity photo, boolean isSelected);
    }

    // Interface để lắng nghe sự kiện long press
    public interface OnLongPressListener {
        void onLongPress();
    }

    // Interface để lắng nghe sự kiện thay đổi trạng thái chọn
    public interface OnSelectionChangeListener {
        void onSelectionChange();
    }

    public void setOnLongPressListener(OnLongPressListener listener) {
        this.longPressListener = listener;
    }

    public void setOnSelectionChangeListener(OnSelectionChangeListener listener) {
        this.selectionChangeListener = listener;
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
}