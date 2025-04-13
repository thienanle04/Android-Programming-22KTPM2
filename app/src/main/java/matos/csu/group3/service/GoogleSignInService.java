package matos.csu.group3.service;

import matos.csu.group3.BuildConfig;
import matos.csu.group3.ui.fragment.SettingsFragment;

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
import androidx.appcompat.app.AppCompatActivity;
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GoogleSignInService {
    private static final String TAG = "GoogleSignInService";
    private static final String CLIENT_ID = BuildConfig.GOOGLE_CLIENT_ID;
    private static final String REDIRECT_URI = "matos.csu.group3:/oauth2redirect";
    private AuthorizationService authService;
    private AuthState authState;
    private Context context;

    public GoogleSignInService(Context context) {
        this.context = context;
        this.authService = new AuthorizationService(context);
        this.authState = new AuthState();
    }

    public void signIn(ActivityResultLauncher<Intent> launcher) {
        AuthorizationServiceConfiguration serviceConfig =
                new AuthorizationServiceConfiguration(
                        Uri.parse("https://accounts.google.com/o/oauth2/auth"), // Auth endpoint
                        Uri.parse("https://oauth2.googleapis.com/token")  // Token endpoint
                );

        AuthorizationRequest authRequest = new AuthorizationRequest.Builder(
                serviceConfig,
                CLIENT_ID,
                ResponseTypeValues.CODE,
                Uri.parse(REDIRECT_URI))
                .setScopes("openid", "profile", "email", "https://www.googleapis.com/auth/drive.file") // Add Drive scope
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
                // Ensure UI updates are done on the main thread
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

    public interface AuthResultCallback {
        void onSuccess(TokenResponse response);
        void onError(Exception e);
    }

    // Class to hold image information
    private static class ImageInfo {
        Uri uri;
        String name;
        String mimeType;

        ImageInfo(Uri uri, String name, String mimeType) {
            this.uri = uri;
            this.name = name;
            this.mimeType = mimeType;
        }
    }

    private void notifyUser(String message) {
        new Handler(Looper.getMainLooper()).post(() -> {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        });
    }
    public void syncPhotosToDrive() {
        Log.d(TAG, "Starting photo sync to Google Drive");
        authState.performActionWithFreshTokens(authService, (accessToken, idToken, ex) -> {
            if (ex != null) {
                Log.e(TAG, "Failed to get fresh tokens: " + ex.getMessage(), ex);
                notifyUser("Sync failed: Unable to authenticate");
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

                    // Get existing files in the photos folder
                    Set<String> existingFileNames = getExistingFileNames(accessToken, folderId);
                    Log.d(TAG, "Found " + existingFileNames.size() + " existing files in photos folder");

                    List<ImageInfo> images = getDeviceImages();
                    Log.d(TAG, "Found " + images.size() + " images on device");
                    if (images.isEmpty()) {
                        notifyUser("No images found to sync");
                        return;
                    }

                    List<ImageInfo> imagesToUpload = new ArrayList<>();
                    for (ImageInfo image : images) {
                        if (!existingFileNames.contains(image.name)) {
                            imagesToUpload.add(image);
                        } else {
                            Log.d(TAG, "Skipping image (already exists): " + image.name);
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
                                uploadImageToDrive(accessToken, folderId, image);
                                Log.d(TAG, "Successfully uploaded image: " + image.name);
                            } catch (IOException e) {
                                Log.e(TAG, "Error uploading image: " + image.name + ", error: " + e.getMessage(), e);
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

    private Set<String> getExistingFileNames(String accessToken, String folderId) throws IOException, JSONException {
        Log.d(TAG, "Querying existing files in folder: " + folderId);
        Set<String> fileNames = new HashSet<>();
        String url = "https://www.googleapis.com/drive/v3/files?q='" + folderId + "'+in+parents+and+trashed=false";
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);
        conn.setRequestMethod("GET");

        int responseCode = conn.getResponseCode();
        Log.d(TAG, "File query response code: " + responseCode);
        if (responseCode == 200) {
            InputStream is = conn.getInputStream();
            String response = new Scanner(is).useDelimiter("\\A").next();
            JSONObject json = new JSONObject(response);
            JSONArray files = json.getJSONArray("files");
            for (int i = 0; i < files.length(); i++) {
                String name = files.getJSONObject(i).getString("name");
                fileNames.add(name);
                Log.d(TAG, "Found existing file: " + name);
            }
        } else {
            Log.e(TAG, "File query failed with response code: " + responseCode);
        }
        return fileNames;
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
            JSONObject json = new JSONObject(response);
            JSONArray files = json.getJSONArray("files");
            if (files.length() > 0) {
                String folderId = files.getJSONObject(0).getString("id");
                Log.d(TAG, "Found existing photos folder with ID: " + folderId);
                return folderId;
            }
        } else {
            Log.e(TAG, "Folder query failed with response code: " + responseCode);
        }

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
            JSONObject json = new JSONObject(response);
            String folderId = json.getString("id");
            Log.d(TAG, "Created new photos folder with ID: " + folderId);
            return folderId;
        } else {
            Log.e(TAG, "Failed to create folder, response code: " + responseCode);
            InputStream errorStream = conn.getErrorStream();
            String errorResponse = errorStream != null ? new Scanner(errorStream).useDelimiter("\\A").next() : "";
            Log.e(TAG, "Error response: " + errorResponse);
            return null;
        }
    }


    private List<ImageInfo> getDeviceImages() {
        Log.d(TAG, "Querying device images");
        List<ImageInfo> images = new ArrayList<>();
        ContentResolver resolver = context.getContentResolver();
        Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.MIME_TYPE
        };

        Cursor cursor = resolver.query(uri, projection, null, null, null);
        if (cursor != null) {
            Log.d(TAG, "Found " + cursor.getCount() + " images in MediaStore");
            while (cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID));
                String name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME));
                String mimeType = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE));
                Uri imageUri = ContentUris.withAppendedId(uri, id);
                images.add(new ImageInfo(imageUri, name, mimeType));
            }
            cursor.close();
        } else {
            Log.e(TAG, "MediaStore cursor is null");
        }
        return images;
    }

    private void uploadImageToDrive(String accessToken, String folderId, ImageInfo image) throws IOException {
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

        InputStream is = context.getContentResolver().openInputStream(image.uri);
        if (is == null) {
            Log.e(TAG, "Failed to open InputStream for image: " + image.name);
            return;
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
        if (responseCode == 200) {
            Log.d(TAG, "Uploaded image: " + image.name + " successfully");
        } else {
            InputStream errorStream = conn.getErrorStream();
            String errorResponse = errorStream != null ? new Scanner(errorStream).useDelimiter("\\A").next() : "";
            Log.e(TAG, "Failed to upload image: " + image.name + ", response code: " + responseCode + ", error: " + errorResponse);
        }
    }
}
