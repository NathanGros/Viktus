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
import com.bignat.viktus.ui.dialog.ConfirmDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class StockDialog extends DialogFragment {

    public static final String RESULT_KEY = "stock_dialog";
    public static final String KEY_ACTION = "action";
    public static final String KEY_INDEX = "index";       // index in the supplies list
    public static final String KEY_NAME = "name";
    public static final String KEY_QUANTITY = "quantity";
    public static final String KEY_UNIT = "unit";         // Unit ordinal

    public static final String ACTION_CREATE = "create";
    public static final String ACTION_UPDATE = "update";
    public static final String ACTION_DELETE = "delete";

    private static final int NEW_ITEM = -1;

    public static StockDialog newCreate() {
        Bundle args = new Bundle();
        args.putInt(KEY_INDEX, NEW_ITEM);
        StockDialog d = new StockDialog();
        d.setArguments(args);
        return d;
    }

    public static StockDialog newEdit(int index, Supply supply) {
        Bundle args = new Bundle();
        args.putInt(KEY_INDEX, index);
        args.putString(KEY_NAME, supply.name);
        args.putDouble(KEY_QUANTITY, supply.quantity);
        args.putInt(KEY_UNIT, supply.unit.ordinal());
        StockDialog d = new StockDialog();
        d.setArguments(args);
        return d;
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
        return inflater.inflate(R.layout.dialog_stock, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = requireArguments();
        int index = args.getInt(KEY_INDEX, NEW_ITEM);
        boolean isEdit = index != NEW_ITEM;
        String originalName = args.getString(KEY_NAME, "");
        Unit originalUnit = Unit.values()[args.getInt(KEY_UNIT, 0)];
        // "Unspecified" supplies have no quantity to edit
        boolean unspecifiedEdit = isEdit && originalUnit == Unit.UNSPECIFIED;

        TextView title = view.findViewById(R.id.dialog_title);
        TextInputLayout layoutName = view.findViewById(R.id.layout_name);
        TextInputLayout layoutQuantity = view.findViewById(R.id.layout_quantity);
        TextInputEditText inputName = view.findViewById(R.id.input_name);
        TextInputEditText inputQuantity = view.findViewById(R.id.input_quantity);
        TextView unitText = view.findViewById(R.id.unit_selection);
        Spinner spinnerUnit = view.findViewById(R.id.spinner_unit);
        MaterialButton btnDelete = view.findViewById(R.id.btn_delete);
        MaterialButton btnConfirm = view.findViewById(R.id.btn_confirm);
        MaterialButtonToggleGroup toggle = view.findViewById(R.id.toggle_available);

        // Which parts are visible in which mode
        title.setText(isEdit ? originalName : getString(R.string.add_supply));
        layoutName.setVisibility(isEdit ? View.GONE : View.VISIBLE);
        unitText.setVisibility(isEdit ? View.GONE : View.VISIBLE);
        spinnerUnit.setVisibility(isEdit ? View.GONE : View.VISIBLE);
        btnDelete.setVisibility(isEdit ? View.VISIBLE : View.GONE);
        layoutQuantity.setVisibility(isEdit && !unspecifiedEdit ? View.VISIBLE : View.GONE);
        toggle.setVisibility(unspecifiedEdit ? View.VISIBLE : View.GONE);
        btnConfirm.setVisibility(View.VISIBLE);
        btnConfirm.setContentDescription(getString(isEdit ? R.string.done : R.string.add));

        if (unspecifiedEdit && savedInstanceState == null) {
            toggle.check(args.getDouble(KEY_QUANTITY) > 1e-9 ? R.id.btn_yes : R.id.btn_no);
        }

        // Unit spinner (new mode)
        Unit[] units = Unit.values();
        String[] labels = new String[units.length];
        for (int i = 0; i < units.length; i++) {
            labels[i] = getString(units[i].labelRes);
        }
        ArrayAdapter<String> unitAdapter = new ArrayAdapter<>(
                requireContext(), R.layout.spinner_item, labels);
        unitAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerUnit.setAdapter(unitAdapter);

        // Quantity (edit mode): the unit is a fixed suffix
        if (isEdit && !unspecifiedEdit) {
            layoutQuantity.setSuffixText(getString(originalUnit.labelRes));
            if (savedInstanceState == null) {
                double q = args.getDouble(KEY_QUANTITY);
                inputQuantity.setText(q == Math.floor(q)
                        ? String.valueOf((long) q)
                        : String.valueOf(q));
            }
        }

        view.findViewById(R.id.btn_close).setOnClickListener(v -> dismiss());
        view.findViewById(R.id.btn_cancel).setOnClickListener(v -> dismiss());

        btnConfirm.setOnClickListener(v -> {
            Bundle result = new Bundle();
            result.putInt(KEY_INDEX, index);

            if (!isEdit) {
                String name = String.valueOf(inputName.getText()).trim();
                layoutName.setError(null);
                if (name.isEmpty()) {
                    layoutName.setError(getString(R.string.error_name));
                    return;
                }
                result.putString(KEY_ACTION, ACTION_CREATE);
                result.putString(KEY_NAME, name);
                result.putInt(KEY_UNIT, spinnerUnit.getSelectedItemPosition());
            } else {
                double quantity;
                if (unspecifiedEdit) {
                    quantity = toggle.getCheckedButtonId() == R.id.btn_yes ? 1 : 0;
                } else {
                    layoutQuantity.setError(null);
                    try {
                        quantity = Double.parseDouble(
                                String.valueOf(inputQuantity.getText()).trim().replace(',', '.'));
                    } catch (NumberFormatException e) {
                        layoutQuantity.setError(getString(R.string.error_number));
                        return;
                    }
                }
                result.putString(KEY_ACTION, ACTION_UPDATE);
                result.putDouble(KEY_QUANTITY, quantity);
            }
            send(result);
        });

        // Delete: confirm first
        getChildFragmentManager().setFragmentResultListener(
                ConfirmDialog.RESULT_KEY, getViewLifecycleOwner(), (key, r) -> {
                    Bundle result = new Bundle();
                    result.putString(KEY_ACTION, ACTION_DELETE);
                    result.putInt(KEY_INDEX, index);
                    send(result);
                });

        btnDelete.setOnClickListener(v -> ConfirmDialog.newInstance(
                        getString(R.string.delete_title, originalName),
                        getString(R.string.delete_message, originalName),
                        getString(R.string.delete))
                .show(getChildFragmentManager(), "confirm_delete"));
    }

    private void send(Bundle result) {
        getParentFragmentManager().setFragmentResult(RESULT_KEY, result);
        dismiss();
    }

    @Override
    public void onStart() {
        super.onStart();
        Window window = getDialog().getWindow();
        if (window == null) return;

        int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.9);
        window.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT);
        window.setGravity(Gravity.CENTER);
        window.setBackgroundDrawableResource(R.drawable.bg_dialog);
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }
}
