package matos.csu.group3.ui.fragment;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.io.File;

import matos.csu.group3.R;
import matos.csu.group3.ui.main.SettingsActivity;

public class BottomExtendedMenu extends DialogFragment {

    private static BottomExtendedMenu instance = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_extended_menu, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Setup click listeners for buttons
        view.findViewById(R.id.btnFavorites).setOnClickListener(v -> {
            Toast.makeText(getContext(), "Favorite clicked", Toast.LENGTH_SHORT).show();
            dismiss();
        });

        view.findViewById(R.id.btnLocation).setOnClickListener(v -> {
            Toast.makeText(getContext(), "Location clicked", Toast.LENGTH_SHORT).show();
            dismiss();
        });

        view.findViewById(R.id.btnTrash).setOnClickListener(v -> {
            Toast.makeText(getContext(), "Trash clicked", Toast.LENGTH_SHORT).show();
            dismiss();
        });

        view.findViewById(R.id.btnSettings).setOnClickListener(v -> {
            try {
                Intent settingsIntent = new Intent(requireActivity(), SettingsActivity.class);
                settingsIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(settingsIntent);
            } catch (RuntimeException e) {
                Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
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

    // Static method to ensure only one instance
    public static void show(FragmentManager fragmentManager) {
        if (instance == null) {
            instance = new BottomExtendedMenu();
        }
        if (!instance.isAdded()) {
            instance.show(fragmentManager, "BottomExtendedMenu");
        }
    }
}
