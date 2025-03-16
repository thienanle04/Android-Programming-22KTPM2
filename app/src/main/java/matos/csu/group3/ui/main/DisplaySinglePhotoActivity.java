package matos.csu.group3.ui.main;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import matos.csu.group3.R;
import matos.csu.group3.data.local.entity.PhotoEntity;
import matos.csu.group3.ui.editor.CropAndRotateActivity;

public class DisplaySinglePhotoActivity extends AppCompatActivity {

    private ImageView imgSoloPhoto;
    private TextView txtSoloMsg;
    private Button btnSoloBack, btnEdit, btnShare;
    private PhotoEntity photo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_single_photo);

        // Ánh xạ các view
        imgSoloPhoto = findViewById(R.id.imgSoloPhoto);
        txtSoloMsg = findViewById(R.id.txtSoloMsg);
        btnSoloBack = findViewById(R.id.btnSoloBack);
        btnEdit = findViewById(R.id.btnEdit);
        btnShare = findViewById(R.id.btnShare);

        // Lấy dữ liệu từ Intent
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("photoEntity")) {
            photo = (PhotoEntity) intent.getSerializableExtra("photoEntity");

            // Cập nhật giao diện với dữ liệu ảnh
            if (photo != null) {
                txtSoloMsg.setText(photo.getDateTaken());
                Glide.with(this)
                        .load(new File(photo.getFilePath()))
                        .into(imgSoloPhoto);
            }
        }

        // Nút chỉnh sửa ảnh
        btnEdit.setOnClickListener(v -> {
            if (photo != null) {
                Intent cropIntent = new Intent(DisplaySinglePhotoActivity.this, CropAndRotateActivity.class);
                cropIntent.setData(Uri.fromFile(new File(photo.getFilePath())));
                cropIntent.putExtra("photoEntity", photo);
                startActivity(cropIntent);
            }
        });

        // Nút quay lại
        btnSoloBack.setOnClickListener(v -> finish());

        // Nút chia sẻ ảnh
        btnShare.setOnClickListener(v -> {
            if (photo != null) {
                // Create a list with the single photo
                List<PhotoEntity> photos = new ArrayList<>();
                photos.add(photo);

                // Call the sharePhotosViaMessenger method
                PhotoListOfAlbumActivity.sharePhotosViaPackage(DisplaySinglePhotoActivity.this, photos);
            } else {
                Toast.makeText(this, "Không có ảnh để chia sẻ", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
