package matos.csu.group3.ui.adapter;

import android.media.Image;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import matos.csu.group3.R;
import matos.csu.group3.data.local.entity.AlbumEntity;
import matos.csu.group3.data.local.entity.PhotoAlbum;
import matos.csu.group3.data.local.entity.PhotoEntity;
import matos.csu.group3.viewmodel.AlbumViewModel;
import matos.csu.group3.viewmodel.PhotoViewModel;

public class AlbumAdapter extends RecyclerView.Adapter<AlbumAdapter.AlbumViewHolder> {
    private List<AlbumEntity> albumList;
    private OnItemClickListener listener;
    private boolean isSelectionMode = false; // Mặc định là false
    private final AlbumViewModel viewModel;
    private final LifecycleOwner lifecycleOwner;

    // Constructor
    public AlbumAdapter(List<AlbumEntity> albumList, OnItemClickListener listener, AlbumViewModel viewModel, LifecycleOwner lifecycleOwner) {
        this.albumList = albumList;
        this.listener = listener;
        this.viewModel = viewModel;
        this.lifecycleOwner = lifecycleOwner;
    }

    // Phương thức để cập nhật trạng thái isSelectionMode
    public void setSelectionMode(boolean isSelectionMode) {
        this.isSelectionMode = isSelectionMode;
        notifyDataSetChanged(); // Cập nhật lại toàn bộ RecyclerView
    }

    // Phương thức để lấy danh sách các album đã được chọn
    public List<AlbumEntity> getSelected() {
        List<AlbumEntity> selectedAlbums = new ArrayList<>();
        for (AlbumEntity album : albumList) {
            if (album.isSelected()) {
                selectedAlbums.add(album);
            }
        }
        return selectedAlbums;
    }

    // Interface để xử lý sự kiện click
    public interface OnItemClickListener {
        void onItemClick(AlbumEntity album);
    }

    @NonNull
    @Override
    public AlbumViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_album, parent, false);
        return new AlbumViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlbumViewHolder holder, int position) {
        AlbumEntity album = albumList.get(position);
        holder.bind(album, listener, isSelectionMode); // Truyền isSelectionMode vào bind

        // Quan sát tất cả ảnh trong album từ PhotoViewModel
        PhotoViewModel photoViewModel = new ViewModelProvider((ViewModelStoreOwner) lifecycleOwner).get(PhotoViewModel.class);
        LiveData<List<PhotoAlbum>> photos = viewModel.getPhotosByAlbumId(album.getId());

        photos.observe(lifecycleOwner, photoAlbums -> {
            if (photoAlbums != null && !photoAlbums.isEmpty()) {
                // Lấy ảnh đầu tiên trong danh sách
                PhotoAlbum firstPhotoAlbum = photoAlbums.get(photoAlbums.size() - 1);

                // Lấy thông tin chi tiết của ảnh đầu tiên từ PhotoEntity
                photoViewModel.getPhotoById(firstPhotoAlbum.getPhotoId()).observe(lifecycleOwner, photoEntity -> {
                    if (photoEntity != null) {
                        // Load ảnh vào ImageView bằng Glide
                        Glide.with(holder.itemView.getContext())
                                .load(new File(photoEntity.getFilePath()))
                                .apply(RequestOptions.bitmapTransform(new CenterCrop())) // Crop ảnh trước khi bo góc
                                .apply(RequestOptions.bitmapTransform(new RoundedCorners(50))) // Bo góc sau khi đã crop
                                .placeholder(R.drawable.ic_album) // Ảnh mặc định
                                .into(holder.folderImageView);
                    } else {
                        // Nếu không có ảnh, hiển thị ảnh mặc định
                        holder.folderImageView.setImageResource(R.drawable.ic_album);
                    }
                });
            } else {
                // Nếu không có ảnh nào, hiển thị ảnh mặc định
                holder.folderImageView.setImageResource(R.drawable.ic_album);
            }
        });
    }


    @Override
    public int getItemCount() {
        return albumList.size();
    }

    // Phương thức cập nhật dữ liệu
    public void updateData(List<AlbumEntity> newAlbumList) {
        this.albumList = newAlbumList;
        notifyDataSetChanged();
    }

    // ViewHolder
    static class AlbumViewHolder extends RecyclerView.ViewHolder {
        private TextView albumNameTextView;
        private CheckBox checkBox; // Thêm CheckBox
        private ImageView folderImageView;

        public AlbumViewHolder(@NonNull View itemView) {
            super(itemView);
            albumNameTextView = itemView.findViewById(R.id.albumNameTextView);
            checkBox = itemView.findViewById(R.id.checkBox); // Ánh xạ CheckBox
            folderImageView = itemView.findViewById(R.id.folderImageView);
        }

        public void bind(final AlbumEntity album, final OnItemClickListener listener, boolean isSelectionMode) {
            String albumName = album.getName();
            if (albumName != null && !albumName.isEmpty()) {
                albumName = albumName.substring(0, 1).toUpperCase() + albumName.substring(1).toLowerCase();
            }
            albumNameTextView.setText(albumName);

            // Ẩn/hiện CheckBox dựa trên isSelectionMode
            if (isSelectionMode) {
                checkBox.setVisibility(View.VISIBLE);
                checkBox.setChecked(album.isSelected()); // Cập nhật trạng thái chọn
                checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    album.setSelected(isChecked); // Cập nhật trạng thái chọn của album
                });
            } else {
                checkBox.setVisibility(View.GONE);
            }

            // Xử lý sự kiện click
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(album);
                }
            });
        }
    }
    public List<AlbumEntity> getSelectedAlbums() {
        List<AlbumEntity> selectedAlbums = new ArrayList<>();
        for (AlbumEntity album : albumList) {
            if (album.isSelected()) {
                selectedAlbums.add(album);
            }
        }
        return selectedAlbums;
    }
}