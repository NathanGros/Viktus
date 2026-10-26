package com.bignat.viktus.ui.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.bignat.viktus.R;

public class ConfirmDialog extends DialogFragment {

    public static final String RESULT_KEY = "confirm_dialog";
    public static final String KEY_CONFIRMED = "confirmed";

    private static final String ARG_TITLE = "title";
    private static final String ARG_MESSAGE = "message";
    private static final String ARG_CONFIRM_TEXT = "confirm_text";

    public static ConfirmDialog newInstance(String title, String message, String confirmText) {
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_MESSAGE, message);
        args.putString(ARG_CONFIRM_TEXT, confirmText);

        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setArguments(args);
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_confirm, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = requireArguments();
        ((TextView) view.findViewById(R.id.confirm_title)).setText(args.getString(ARG_TITLE));
        ((TextView) view.findViewById(R.id.confirm_message)).setText(args.getString(ARG_MESSAGE));

        TextView ok = view.findViewById(R.id.btn_confirm_ok);
        ok.setText(args.getString(ARG_CONFIRM_TEXT, "OK"));

        view.findViewById(R.id.btn_confirm_cancel).setOnClickListener(v -> dismiss());

        ok.setOnClickListener(v -> {
            Bundle result = new Bundle();
            result.putBoolean(KEY_CONFIRMED, true);
            getParentFragmentManager().setFragmentResult(RESULT_KEY, result);
            dismiss();
        });
    }

    @Override
    public void onStart() {
        super.onStart();

        Window window = getDialog().getWindow();
        if (window == null) return;

        // Same width and background as the supply dialog, but height fits the content
        int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.9);
        window.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT);
        window.setGravity(Gravity.CENTER);
        window.setBackgroundDrawableResource(R.drawable.bg_dialog);
    }
}