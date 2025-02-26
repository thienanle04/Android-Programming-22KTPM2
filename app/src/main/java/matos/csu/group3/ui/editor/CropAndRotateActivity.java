package matos.csu.group3.ui.editor;

import matos.csu.group3.R;
import matos.csu.group3.data.local.AppDatabase;
import matos.csu.group3.data.local.dao.PhotoDao;
import matos.csu.group3.data.local.entity.PhotoEntity;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.yalantis.ucrop.UCrop;
import java.io.*;
import java.util.Objects;

public class CropAndRotateActivity extends AppCompatActivity {
    private Uri imageUri;
    private PhotoEntity photoEntity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop);

        Intent intent = getIntent();
        if (intent != null) {
            imageUri = intent.getData();

            if (intent.hasExtra("photoEntity")) {
                photoEntity = (PhotoEntity) intent.getSerializableExtra("photoEntity");

                if (photoEntity != null) {
                    if (imageUri != null) {
                        Log.d("CropActivity", "Starting crop for: " + photoEntity.getName());
                        startCrop(imageUri);
                    }
                    else {
                        Log.e("CropActivity", "Image URI is null");
                        Toast.makeText(this, "Image URI is missing", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                } else {
                    Log.e("CropActivity", "Missing photo data or URI");
                    Toast.makeText(this, "Missing photo data", Toast.LENGTH_SHORT).show();
                    finish();
                }
            } else {
                Log.e("CropActivity", "No photoEntity in intent");
                Toast.makeText(this, "No photo data provided", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else {
            Log.e("CropActivity", "Null intent received");
            Toast.makeText(this, "Error loading photo", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void startCrop(Uri uri) {
        Log.d("CropActivity", "Image URI: " + uri.toString());
        Log.d("CropActivity", "Image URI Path: " + uri.getPath());
        Log.d("CropActivity", "Image URI Scheme: " + uri.getScheme());
        Log.d("CropActivity", "Image URI String: " + uri.toString());

        File file = new File(Objects.requireNonNull(uri.getPath()));
        if (!file.exists()) {
            Log.e("CropActivity", "File does not exist: " + file.getAbsolutePath());
            Toast.makeText(this, "File not found!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String filename = "cropped_" + System.currentTimeMillis() + ".jpg";
        File destinationFile = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), filename);
        Uri destinationUri = Uri.fromFile(destinationFile);

        Log.d("CropActivity", "Destination File Path: " + destinationFile.getAbsolutePath());
        Log.d("CropActivity", "Destination URI Path: " + destinationUri.getPath());

        try {
            UCrop.of(uri, destinationUri)
                    .withMaxResultSize(1080, 1080)
                    .start(this);
        } catch (Exception e) {
            Log.e("CropActivity", "Error starting UCrop", e);
            Toast.makeText(this, "Error starting crop: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("CropActivity", "requestCode: " + requestCode + ", resultCode: " + resultCode);
        if (requestCode == UCrop.REQUEST_CROP) {
            if (resultCode == RESULT_OK) {
                final Uri resultUri = UCrop.getOutput(data);
                if (resultUri != null) {
                    Log.d("CropDone", resultUri.toString());
                    Log.d("CropActivity", "Cropped image created at: " + resultUri.getPath());
                    photoEntity.setFilePath(resultUri.getPath());
                    savePhotoEntity(photoEntity);

                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("photoEntity", photoEntity);
                    setResult(RESULT_OK, resultIntent);
                }
                else {
                    Log.e("CropActivity", "Crop result URI is null");
                    setResult(RESULT_CANCELED);
                }
            }
            else if (resultCode == UCrop.RESULT_ERROR) {
                final Throwable cropError = UCrop.getError(data);
                if (cropError != null) {
                    Log.e("CropActivity", "Crop error: " + cropError.getMessage());
                    Toast.makeText(this, "Error cropping image: " + cropError.getMessage(), Toast.LENGTH_SHORT).show();
                } else {
                    Log.e("CropActivity", "Unknown Crop error");
                    Toast.makeText(this, "Unknown error occured during cropping.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.d("CropActivity", "Crop cancelled by user");
                setResult(RESULT_CANCELED);
            }
            finish();
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

            try {
                File originalFile = new File(photoEntity.getFilePath());
                if (!originalFile.exists()) {
                    Log.e("savePhotoEntity", "Original file doesn't exist");
                    return;
                }

                File publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                File publicFile = new File(publicDir, "MyApp_" + originalFile.getName());
                FileInputStream inStream = new FileInputStream(originalFile);
                FileOutputStream outStream = new FileOutputStream(publicFile);
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inStream.read(buffer)) > 0) {
                    outStream.write(buffer, 0, length);
                }
                inStream.close();
                outStream.close();

                photoEntity.setFilePath(publicFile.getAbsolutePath());
                if (photoEntity.getId() > 0) {
                    photoDao.update(photoEntity);
                }

                // Scan the new file
                MediaScannerConnection.scanFile(
                        this,
                        new String[]{ publicFile.getAbsolutePath() },
                        new String[]{ "image/jpeg" },
                        (path, uri) -> Log.i("savePhotoEntity", "Scanned public file: " + path + " to " + uri)
                );
            } catch (Exception e) {
                Log.e("savePhotoEntity", "Failed to copy file: " + e.getMessage());
            }
        }).start();
    }
}
