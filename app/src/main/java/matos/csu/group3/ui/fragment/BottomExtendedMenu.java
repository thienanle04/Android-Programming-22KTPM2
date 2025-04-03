package matos.csu.group3.ui.fragment;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import matos.csu.group3.R;
import matos.csu.group3.data.local.AppDatabase;
import matos.csu.group3.data.local.dao.AlbumDao;
import matos.csu.group3.data.local.entity.AlbumEntity;
import matos.csu.group3.ui.main.PhotoListOfAlbumActivity;
import matos.csu.group3.ui.main.SettingsActivity;

public class BottomExtendedMenu extends DialogFragment {

    private static final int REQUEST_CODE_PHOTO_LIST = 1002;
    private AlbumDao albumDao;
    private AlbumEntity album;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_extended_menu, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        AppDatabase database = AppDatabase.getInstance(requireContext().getApplicationContext());
        albumDao = database.albumDao();

        // Setup click listeners for buttons
        view.findViewById(R.id.btnFavorites).setOnClickListener(v -> {
            Toast.makeText(getContext(), "Favorite clicked", Toast.LENGTH_SHORT).show();
            new Thread(() -> {
                AlbumEntity favoriteAlbum = albumDao.getAlbumByNameSync("Favorite");

                // Switch back to main thread to update UI
                requireActivity().runOnUiThread(() -> {
                    if (favoriteAlbum != null) {
                        openAlbum(favoriteAlbum);
                    } else {
                        Toast.makeText(getContext(), "Favorite album not found", Toast.LENGTH_SHORT).show();
                    }
                    dismiss();
                });
            }).start();
            dismiss();
        });

        view.findViewById(R.id.btnLocation).setOnClickListener(v -> {
            Toast.makeText(getContext(), "Location clicked", Toast.LENGTH_SHORT).show();
            dismiss();
        });

        view.findViewById(R.id.btnTrash).setOnClickListener(v -> {
            Toast.makeText(getContext(), "Trash clicked", Toast.LENGTH_SHORT).show();
            new Thread(() -> {
                AlbumEntity trashAlbum = albumDao.getAlbumByNameSync("Trash");

                // Switch back to main thread to update UI
                requireActivity().runOnUiThread(() -> {
                    if (trashAlbum != null) {
                        openAlbum(trashAlbum);
                    } else {
                        Toast.makeText(getContext(), "Trash album not found", Toast.LENGTH_SHORT).show();
                    }
                    dismiss();
                });
            }).start();
            dismiss();
        });

        view.findViewById(R.id.btnSettings).setOnClickListener(v -> {
            try {
                Intent settingsIntent = new Intent(requireActivity(), SettingsActivity.class);
                settingsIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(settingsIntent);
            } catch (RuntimeException e) {
                Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
            }
            dismiss();
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            // Set the background to transparent
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            // Set width to 96% of the screen and position at the bottom
            getDialog().getWindow().setLayout((int) (getResources().getDisplayMetrics().widthPixels * 0.96), ViewGroup.LayoutParams.WRAP_CONTENT);
            getDialog().getWindow().setGravity(Gravity.BOTTOM);
        }
    }

    public static void show(FragmentManager fragmentManager) {
        BottomExtendedMenu fragment = (BottomExtendedMenu) fragmentManager.findFragmentByTag("BottomExtendedMenu");
        if (fragment == null) {
            fragment = new BottomExtendedMenu();
        }
        if (!fragment.isAdded()) {
            fragment.show(fragmentManager, "BottomExtendedMenu");
        }
    }

    private void openAlbum(AlbumEntity album) {
        Intent intent = new Intent(getContext(), PhotoListOfAlbumActivity.class);
        intent.putExtra("ALBUM_ID", album.getId());
        startActivityForResult(intent, REQUEST_CODE_PHOTO_LIST);
    }
}