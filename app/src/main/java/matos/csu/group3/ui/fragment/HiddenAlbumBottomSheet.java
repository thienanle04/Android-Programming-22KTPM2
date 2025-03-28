package matos.csu.group3.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import matos.csu.group3.R;
import matos.csu.group3.utils.PasswordHelper;

public class HiddenAlbumBottomSheet extends BottomSheetDialogFragment {

    private OnHiddenAlbumUnlockedListener listener;

    public interface OnHiddenAlbumUnlockedListener {
        void onUnlockSuccess();
    }

    public void setOnHiddenAlbumUnlockedListener(OnHiddenAlbumUnlockedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.hidden_album_bottom_sheet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        EditText etPassword = view.findViewById(R.id.etPassword);
        Button btnUnlock = view.findViewById(R.id.btnUnlock);

        btnUnlock.setOnClickListener(v -> {
            String password = etPassword.getText().toString();
            if (PasswordHelper.checkPassword(requireContext(), password)) {
                if (listener != null) {
                    listener.onUnlockSuccess();
                }
                dismiss();
            } else {
                Toast.makeText(requireContext(), "Mật khẩu không đúng", Toast.LENGTH_SHORT).show();
            }
        });
    }
}