package matos.csu.group3.ui.editor;

import matos.csu.group3.R;
import matos.csu.group3.data.local.AppDatabase;
import matos.csu.group3.data.local.dao.PhotoDao;
import matos.csu.group3.data.local.entity.PhotoEntity;
import android.content.Intent;
import android.os.Bundle;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.yalantis.ucrop.UCrop;
import java.io.File;

public class CropActivity extends AppCompatActivity {
    private Uri imageUri;
    private PhotoEntity photoEntity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop);

        imageUri = getIntent().getData();
        photoEntity = (PhotoEntity) getIntent().getSerializableExtra("photoEntity");

        startCrop(imageUri);
    }

    private void startCrop(Uri uri) {
        Uri destinationUri = Uri.fromFile(new File(getCacheDir(), "cropped_image.jpg"));
        UCrop.of(uri, destinationUri)
                .withMaxResultSize(1080, 1080)
                .start(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            final Uri resultUri = UCrop.getOutput(data);
            if (resultUri != null) {
                photoEntity.setFilePath(resultUri.getPath());
                savePhotoEntity(photoEntity);
            }
        }
        else if (resultCode == UCrop.RESULT_ERROR) {
            final Throwable cropError = UCrop.getError(data);
            if (cropError != null) {
                Log.e("CropActivity", "Crop error: " + cropError.getMessage());
                Toast.makeText(this, "Error cropping image: " + cropError.getMessage(), Toast.LENGTH_SHORT).show();
            }
            else {
                Toast.makeText(this, "Unknown error occured during cropping.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void savePhotoEntity(PhotoEntity photoEntity) {
        AppDatabase db = AppDatabase.getInstance(this);
        PhotoDao photoDao = db.photoDao();

        new Thread(() -> {
            if (photoEntity.getId() > 0) {
                photoDao.update(photoEntity);
            }
            else {
                photoDao.insert(photoEntity);
            }
        }).start();
    }
}
