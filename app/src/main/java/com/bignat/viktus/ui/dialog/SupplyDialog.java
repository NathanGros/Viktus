package com.bignat.viktus.ui.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.bignat.viktus.R;
import com.bignat.viktus.model.Supply;
import com.bignat.viktus.model.Unit;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class SupplyDialog extends DialogFragment {

    public static final String RESULT_KEY = "supply_dialog";
    public static final String KEY_POSITION = "position";
    public static final String KEY_NAME = "name";
    public static final String KEY_QUANTITY = "quantity";
    public static final String KEY_UNIT = "unit";       // Unit ordinal
    public static final String KEY_DELETED = "deleted";

    // Position value that means "this is a new supply, not an existing one"
    public static final int NEW_ITEM = -1;

    public static SupplyDialog newAdd() {
        Bundle args = new Bundle();
        args.putInt(KEY_POSITION, NEW_ITEM);

        SupplyDialog dialog = new SupplyDialog();
        dialog.setArguments(args);
        return dialog;
    }

    public static SupplyDialog newEdit(int position, Supply supply) {
        Bundle args = new Bundle();
        args.putInt(KEY_POSITION, position);
        args.putString(KEY_NAME, supply.name);
        args.putDouble(KEY_QUANTITY, supply.quantity);
        args.putInt(KEY_UNIT, supply.unit.ordinal());

        SupplyDialog dialog = new SupplyDialog();
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
        return inflater.inflate(R.layout.dialog_supply, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = requireArguments();
        int position = args.getInt(KEY_POSITION, NEW_ITEM);
        boolean isEdit = position != NEW_ITEM;
        String originalName = args.getString(KEY_NAME, "");

        TextView title = view.findViewById(R.id.dialog_title);
        MaterialButton btnConfirm = view.findViewById(R.id.btn_confirm);
        MaterialButton btnDelete = view.findViewById(R.id.btn_delete);
        TextInputLayout layoutName = view.findViewById(R.id.layout_name);
        TextInputLayout layoutQuantity = view.findViewById(R.id.layout_quantity);
        TextInputEditText inputName = view.findViewById(R.id.input_name);
        TextInputEditText inputQuantity = view.findViewById(R.id.input_quantity);
        Spinner spinnerUnit = view.findViewById(R.id.spinner_unit);

        // Mode-specific bits
        title.setText(isEdit ? getString(R.string.edit_title, originalName) : getString(R.string.add_supply));
        btnConfirm.setText(isEdit ? getString(R.string.done) : getString(R.string.add));
        btnDelete.setVisibility(isEdit ? View.VISIBLE : View.GONE);

        // Unit spinner
        Unit[] units = Unit.values();
        String[] labels = new String[units.length];
        for (int i = 0; i < units.length; i++) {
            labels[i] = getString(units[i].labelRes);
        }
        spinnerUnit.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, labels));

        // Pre-fill in edit mode (only on first creation, so rotation keeps the user's edits)
        if (isEdit && savedInstanceState == null) {
            double q = args.getDouble(KEY_QUANTITY);
            inputName.setText(originalName);
            inputQuantity.setText(q == Math.floor(q)
                    ? String.valueOf((long) q)   // 2.0 -> "2"
                    : String.valueOf(q));
            spinnerUnit.setSelection(args.getInt(KEY_UNIT));
        }

        view.findViewById(R.id.btn_close).setOnClickListener(v -> dismiss());
        view.findViewById(R.id.btn_cancel).setOnClickListener(v -> dismiss());

        // Add / Done: validate and send the values back
        btnConfirm.setOnClickListener(v -> {
            String name = String.valueOf(inputName.getText()).trim();
            String quantityText = String.valueOf(inputQuantity.getText()).trim();

            layoutName.setError(null);
            layoutQuantity.setError(null);

            if (name.isEmpty()) {
                layoutName.setError(getString(R.string.error_name));
                return;
            }

            double quantity;
            try {
                quantity = Double.parseDouble(quantityText.replace(',', '.'));
            } catch (NumberFormatException e) {
                layoutQuantity.setError(getString(R.string.error_number));
                return;
            }
            if (quantity <= 0) {
                layoutQuantity.setError(getString(R.string.error_positive));
                return;
            }

            Bundle result = new Bundle();
            result.putInt(KEY_POSITION, position);
            result.putString(KEY_NAME, name);
            result.putDouble(KEY_QUANTITY, quantity);
            result.putInt(KEY_UNIT, spinnerUnit.getSelectedItemPosition());
            result.putBoolean(KEY_DELETED, false);
            getParentFragmentManager().setFragmentResult(RESULT_KEY, result);
            dismiss();
        });

        // Delete (edit mode only): ask for confirmation first
        getChildFragmentManager().setFragmentResultListener(
                ConfirmDialog.RESULT_KEY, getViewLifecycleOwner(), (key, confirmResult) -> {
                    Bundle result = new Bundle();
                    result.putInt(KEY_POSITION, position);
                    result.putBoolean(KEY_DELETED, true);
                    getParentFragmentManager().setFragmentResult(RESULT_KEY, result);
                    dismiss();
                });

        btnDelete.setOnClickListener(v ->
                ConfirmDialog.newInstance(
                                getString(R.string.delete_title, originalName),
                                getString(R.string.delete_message, originalName),
                                getString(R.string.delete))
                        .show(getChildFragmentManager(), "confirm_delete"));
    }

    @Override
    public void onStart() {
        super.onStart();

        Window window = getDialog().getWindow();
        if (window == null) return;

        int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.9);
        int height = WindowManager.LayoutParams.WRAP_CONTENT;
        window.setLayout(width, height);
        window.setGravity(Gravity.CENTER);
        window.setBackgroundDrawableResource(R.drawable.bg_dialog);
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }
}