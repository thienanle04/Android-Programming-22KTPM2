package matos.csu.group3.service;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.preference.PreferenceManager;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;
import net.openid.appauth.ResponseTypeValues;
import net.openid.appauth.TokenRequest;
import net.openid.appauth.TokenResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import matos.csu.group3.BuildConfig;
import matos.csu.group3.data.local.dao.PhotoDao;
import matos.csu.group3.data.local.entity.PhotoEntity;

public class GoogleSignInService {
    private static final String TAG = "GoogleSignInService";
    private static final String CLIENT_ID = BuildConfig.GOOGLE_CLIENT_ID;
    private static final String REDIRECT_URI = "matos.csu.group3:/oauth2redirect";
    private AuthorizationService authService;
    private AuthState authState;
    private Context context;
    private PhotoDao photoDao; // Add PhotoDao

    public GoogleSignInService(Context context, PhotoDao photoDao) {
        this.context = context;
        this.authService = new AuthorizationService(context);
        this.authState = new AuthState();
        this.photoDao = photoDao; // Initialize PhotoDao
    }

    public void signIn(ActivityResultLauncher<Intent> launcher) {
        AuthorizationServiceConfiguration serviceConfig =
                new AuthorizationServiceConfiguration(
                        Uri.parse("https://accounts.google.com/o/oauth2/auth"),
                        Uri.parse("https://oauth2.googleapis.com/token")
                );

        Map<String, String> additionalParams = new HashMap<>();
        additionalParams.put("access_type", "offline"); // Request a refresh token

        AuthorizationRequest authRequest = new AuthorizationRequest.Builder(
                serviceConfig,
                CLIENT_ID,
                ResponseTypeValues.CODE,
                Uri.parse(REDIRECT_URI))
                .setScopes("openid", "profile", "email", "https://www.googleapis.com/auth/drive.file")
                .setPrompt("consent") // Use setPrompt instead of additionalParams
                .setAdditionalParameters(additionalParams)
                .build();

        Intent authIntent = authService.getAuthorizationRequestIntent(authRequest);
        launcher.launch(authIntent);
    }

    public void handleAuthResponse(Intent data, AuthResultCallback callback) {
        AuthorizationResponse resp = AuthorizationResponse.fromIntent(data);
        if (resp == null) {
            callback.onError(new Exception("Authorization response is null"));
            return;
        }

        TokenRequest tokenRequest = resp.createTokenExchangeRequest();

        authService.performTokenRequest(tokenRequest, (TokenResponse response, net.openid.appauth.AuthorizationException ex) -> {
            if (response != null) {
                authState.update(response, ex);
                String accessToken = response.accessToken;

                if (accessToken != null) {
                    callback.onSuccess(response);
                    getUserInfoAsync(accessToken);
                } else {
                    callback.onError(new Exception("Access token is null"));
                }
            } else {
                callback.onError(ex);
            }
        });
    }

    private JSONObject getUserInfo(String accessToken) {
        try {
            URL url = new URL("https://www.googleapis.com/oauth2/v3/userinfo");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);
            conn.setRequestMethod("GET");

            InputStream response = conn.getInputStream();
            Scanner scanner = new Scanner(response);
            scanner.useDelimiter("\\A");
            String responseBody = scanner.hasNext() ? scanner.next() : "";

            return new JSONObject(responseBody);
        } catch (Exception e) {
            Log.e(TAG, "Error fetching user info", e);
            return null;
        }
    }

    private void getUserInfoAsync(String accessToken) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            JSONObject userInfo = getUserInfo(accessToken);
            if (userInfo != null) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    saveUserInfoToSharedPreferences(userInfo, accessToken);
                });
            } else {
                Log.e(TAG, "Failed to retrieve user info.");
            }
        });
    }

    private void saveUserInfoToSharedPreferences(JSONObject userInfo, String accessToken) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();

        try {
            editor.putBoolean("is_logged_in", true);
            editor.putString("access_token", accessToken);
            editor.putString("user_id", userInfo.optString("sub", ""));
            editor.putString("user_name", userInfo.optString("name", ""));
            editor.putString("user_email", userInfo.optString("email", ""));
            editor.putString("user_picture", userInfo.optString("picture", ""));
            editor.putString("user_given_name", userInfo.optString("given_name", ""));
            editor.putString("user_family_name", userInfo.optString("family_name", ""));
            editor.putString("user_locale", userInfo.optString("locale", ""));

            editor.apply();
        } catch (Exception e) {
            Log.e(TAG, "Error saving user info to SharedPreferences", e);
        }
    }

    public void signOut() {
        authState = new AuthState();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove("is_logged_in");
        editor.remove("access_token");
        editor.remove("user_id");
        editor.remove("user_name");
        editor.remove("user_email");
        editor.remove("user_picture");
        editor.remove("user_given_name");
        editor.remove("user_family_name");
        editor.remove("user_locale");
        editor.apply();
        Log.d(TAG, "Signed out and cleared auth state");
    }

    public interface AuthResultCallback {
        void onSuccess(TokenResponse response);
        void onError(Exception e);
    }

    private static class ImageInfo {
        Uri uri;
        String name;
        String mimeType;
        PhotoEntity photoEntity; // Add reference to PhotoEntity

        ImageInfo(Uri uri, String name, String mimeType, PhotoEntity photoEntity) {
            this.uri = uri;
            this.name = name;
            this.mimeType = mimeType;
            this.photoEntity = photoEntity;
        }
    }

    private void notifyUser(String message) {
        new Handler(Looper.getMainLooper()).post(() -> {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        });
    }

    public void syncPhotosToDrive(ActivityResultLauncher<Intent> launcher) {
        Log.d(TAG, "Starting photo sync to Google Drive");
        authState.performActionWithFreshTokens(authService, (accessToken, idToken, ex) -> {
            if (ex != null) {
                Log.e(TAG, "Failed to get fresh tokens: " + ex.getMessage(), ex);
                notifyUser("Authentication expired. Please sign in again.");
                new Handler(Looper.getMainLooper()).post(() -> signIn(launcher));
                return;
            }
            Log.d(TAG, "Obtained fresh access token");

            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute(() -> {
                try {
                    String folderId = getOrCreatePhotosFolder(accessToken);
                    if (folderId == null) {
                        Log.e(TAG, "Failed to get or create photos folder");
                        notifyUser("Sync failed: Could not create photos folder");
                        return;
                    }
                    Log.d(TAG, "Photos folder ID: " + folderId);

                    List<ImageInfo> images = getDeviceImages();
                    Log.d(TAG, "Found " + images.size() + " images to process");
                    if (images.isEmpty()) {
                        notifyUser("No images found to sync");
                        return;
                    }

                    List<ImageInfo> imagesToUpload = new ArrayList<>();
                    for (ImageInfo image : images) {
                        if (!image.photoEntity.isUploaded()) {
                            imagesToUpload.add(image);
                        } else {
                            Log.d(TAG, "Skipping image (already uploaded): " + image.name);
                        }
                    }
                    Log.d(TAG, "Selected " + imagesToUpload.size() + " new images to upload");

                    if (imagesToUpload.isEmpty()) {
                        notifyUser("All images are already synced");
                        return;
                    }

                    ExecutorService uploadExecutor = Executors.newFixedThreadPool(5);
                    for (ImageInfo image : imagesToUpload) {
                        uploadExecutor.execute(() -> {
                            try {
                                Log.d(TAG, "Uploading image: " + image.name);
                                String googleDriveId = uploadImageToDrive(accessToken, folderId, image);
                                if (googleDriveId != null) {
                                    Log.d(TAG, "Successfully uploaded image: " + image.name);
                                    // Update the PhotoEntity in the database
                                    PhotoEntity photo = image.photoEntity;
                                    photo.setUploaded(true);
                                    photo.setGoogleDriveId(googleDriveId);
                                    photoDao.updateUploadStatus(photo.getId(), true);
                                    Log.d(TAG, "Updated isUploaded for photo ID: " + photo.getId());
                                }
                            } catch (IOException e) {
                                Log.e(TAG, "Error uploading image: " + image.name + ", error: " + e.getMessage(), e);
                            } catch (JSONException e) {
                                throw new RuntimeException(e);
                            }
                        });
                    }
                    uploadExecutor.shutdown();
                    try {
                        uploadExecutor.awaitTermination(Long.MAX_VALUE, java.util.concurrent.TimeUnit.NANOSECONDS);
                        Log.d(TAG, "All image uploads completed");
                        notifyUser("Photo sync completed: " + imagesToUpload.size() + " new images uploaded");
                    } catch (InterruptedException e) {
                        Log.e(TAG, "Upload executor interrupted", e);
                        notifyUser("Sync interrupted");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error during sync: " + e.getMessage(), e);
                    notifyUser("Sync failed: " + e.getMessage());
                }
            });
        });
    }

    private String getOrCreatePhotosFolder(String accessToken) throws IOException, JSONException {
        Log.d(TAG, "Checking for existing photos folder");
        String url = "https://www.googleapis.com/drive/v3/files?q=mimeType='application/vnd.google-apps.folder'+and+name='photos'+and+trashed=false";
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);
        conn.setRequestMethod("GET");

        int responseCode = conn.getResponseCode();
        Log.d(TAG, "Folder query response code: " + responseCode);
        if (responseCode == 200) {
            InputStream is = conn.getInputStream();
            String response = new Scanner(is).useDelimiter("\\A").next();
            Log.d(TAG, "Folder query response: " + response);
            JSONObject json = new JSONObject(response);
            JSONArray files = json.getJSONArray("files");
            if (files.length() > 0) {
                String folderId = files.getJSONObject(0).getString("id");
                Log.d(TAG, "Found existing photos folder with ID: " + folderId);
                conn.disconnect();
                return folderId;
            }
        } else {
            Log.e(TAG, "Folder query failed with response code: " + responseCode);
            InputStream errorStream = conn.getErrorStream();
            String errorResponse = errorStream != null ? new Scanner(errorStream).useDelimiter("\\A").next() : "";
            Log.e(TAG, "Error response: " + errorResponse);
        }
        conn.disconnect();

        Log.d(TAG, "Creating new photos folder");
        url = "https://www.googleapis.com/drive/v3/files";
        conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);

        String body = "{\"name\": \"photos\", \"mimeType\": \"application/vnd.google-apps.folder\"}";
        OutputStream os = conn.getOutputStream();
        os.write(body.getBytes());
        os.flush();

        responseCode = conn.getResponseCode();
        Log.d(TAG, "Folder creation response code: " + responseCode);
        if (responseCode == 200) {
            InputStream is = conn.getInputStream();
            String response = new Scanner(is).useDelimiter("\\A").next();
            Log.d(TAG, "Folder creation response: " + response);
            JSONObject json = new JSONObject(response);
            String folderId = json.getString("id");
            Log.d(TAG, "Created new photos folder with ID: " + folderId);
            conn.disconnect();
            return folderId;
        } else {
            Log.e(TAG, "Failed to create folder, response code: " + responseCode);
            InputStream errorStream = conn.getErrorStream();
            String errorResponse = errorStream != null ? new Scanner(errorStream).useDelimiter("\\A").next() : "";
            Log.e(TAG, "Error response: " + errorResponse);
            conn.disconnect();
            return null;
        }
    }

    private List<ImageInfo> getDeviceImages() {
        Log.d(TAG, "Querying device images from database");
        List<ImageInfo> images = new ArrayList<>();

        List<PhotoEntity> photos = photoDao.getAllNonDeletedPhotos();
        Log.d(TAG, "Found " + photos.size() + " photos in database");

        for (PhotoEntity photo : photos) {
            String filePath = photo.getFilePath();
            Uri uri = Uri.fromFile(new File(filePath)); // Still create Uri, but not used in upload
            String name = photo.getName();
            String mimeType = photo.getFileFormat() != null ? "image/" + photo.getFileFormat() : "image/jpeg";
            images.add(new ImageInfo(uri, name, mimeType, photo));
        }

        return images;
    }

    private String uploadImageToDrive(String accessToken, String folderId, ImageInfo image) throws IOException, JSONException {
        if (folderId == null) {
            Log.e(TAG, "Cannot upload image: folderId is null");
            return null;
        }

        String url = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart";
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);

        String boundary = "foo_bar_baz";
        conn.setRequestProperty("Content-Type", "multipart/related; boundary=" + boundary);

        OutputStream os = conn.getOutputStream();
        String metadata = "--" + boundary + "\r\n" +
                "Content-Type: application/json; charset=UTF-8\r\n\r\n" +
                "{\"name\": \"" + image.name + "\", \"parents\": [\"" + folderId + "\"]}\r\n";
        os.write(metadata.getBytes());

        String mediaHeader = "--" + boundary + "\r\n" +
                "Content-Type: " + image.mimeType + "\r\n\r\n";
        os.write(mediaHeader.getBytes());

        // Use FileInputStream instead of ContentResolver
        File imageFile = new File(image.photoEntity.getFilePath());
        InputStream is = new FileInputStream(imageFile);
        if (is == null) {
            Log.e(TAG, "Failed to open InputStream for image: " + image.name);
            conn.disconnect();
            return null;
        }
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = is.read(buffer)) != -1) {
            os.write(buffer, 0, bytesRead);
        }
        is.close();

        String end = "\r\n--" + boundary + "--\r\n";
        os.write(end.getBytes());
        os.flush();

        int responseCode = conn.getResponseCode();
        String fileId = null;
        if (responseCode == 200) {
            InputStream isResponse = conn.getInputStream();
            String response = new Scanner(isResponse).useDelimiter("\\A").next();
            JSONObject json = new JSONObject(response);
            fileId = json.getString("id");
            Log.d(TAG, "Uploaded image: " + image.name + " successfully with fileId: " + fileId);
        } else {
            InputStream errorStream = conn.getErrorStream();
            String errorResponse = errorStream != null ? new Scanner(errorStream).useDelimiter("\\A").next() : "";
            Log.e(TAG, "Failed to upload image: " + image.name + ", response code: " + responseCode + ", error: " + errorResponse);
        }
        conn.disconnect();
        return fileId;
    }
}