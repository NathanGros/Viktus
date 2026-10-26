package com.bignat.viktus.ui.dialog;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bignat.viktus.R;
import com.bignat.viktus.ui.adapter.SupplyAdapter;
import com.bignat.viktus.data.AppViewModel;
import com.bignat.viktus.logic.StockChecker;
import com.bignat.viktus.model.Meal;
import com.bignat.viktus.model.Supply;
import com.bignat.viktus.model.Unit;
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MealDialog extends DialogFragment {

    public static final String RESULT_KEY = "meal_dialog";
    public static final String KEY_POSITION = "position";
    public static final String KEY_ACTION = "action";
    public static final String KEY_MEAL = "meal";

    public static final String ACTION_SAVE = "save";
    public static final String ACTION_DELETE = "delete";
    public static final String ACTION_EATEN = "eaten";
    public static final String ACTION_PLAN = "plan";
    public static final String ACTION_BOOKMARK = "bookmark";
    public static final String ACTION_UNSAVE = "unsave";

    public static final int NEW_ITEM = -1;

    private static final String ARG_MODE = "mode";
    private static final String STATE_INGREDIENTS = "state_ingredients";
    private static final String STATE_IMAGE = "state_image";

    private AppViewModel vm;

    private enum DialogMode { ADD, EDIT, VIEW, HISTORY, SAVED }

    public static MealDialog newAdd() {
        return create(DialogMode.ADD, NEW_ITEM, null);
    }

    public static MealDialog newEdit(int position, Meal meal) {
        return create(DialogMode.EDIT, position, meal);
    }

    public static MealDialog newView(int position, Meal meal) {
        return create(DialogMode.VIEW, position, meal);
    }

    public static MealDialog newHistory(Meal meal) {
        return create(DialogMode.HISTORY, NEW_ITEM, meal);
    }

    public static MealDialog newSaved(int position, Meal meal) {
        return create(DialogMode.SAVED, position, meal);
    }

    private static MealDialog create(DialogMode mode, int position, @Nullable Meal meal) {
        Bundle args = new Bundle();
        args.putString(ARG_MODE, mode.name());
        args.putInt(KEY_POSITION, position);
        if (meal != null) args.putSerializable(KEY_MEAL, meal);

        MealDialog dialog = new MealDialog();
        dialog.setArguments(args);
        return dialog;
    }

    private DialogMode mode;
    private int position;
    private Meal original;                                   // null in Add mode
    private final List<Supply> ingredients = new ArrayList<>();  // working copy
    private String imageUri;                                 // working copy
    private SupplyAdapter ingredientAdapter;
    private ActivityResultLauncher<PickVisualMediaRequest> pickImage;

    private ImageView imageView;
    private View imageCard;
    private View btnRemoveImage;
    private View emptyIngredients;

    @SuppressWarnings("unchecked")
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = requireArguments();
        mode = DialogMode.valueOf(args.getString(ARG_MODE));
        position = args.getInt(KEY_POSITION, NEW_ITEM);
        original = (Meal) args.getSerializable(KEY_MEAL);

        if (savedInstanceState != null) {
            // Rotation: restore what the user had already changed
            List<Supply> saved = (List<Supply>) savedInstanceState.getSerializable(STATE_INGREDIENTS);
            if (saved != null) ingredients.addAll(saved);
            imageUri = savedInstanceState.getString(STATE_IMAGE);
        } else if (original != null) {
            // Work on copies so Cancel leaves the original meal untouched
            for (Supply s : original.supplies) {
                ingredients.add(new Supply(s.name, s.quantity, s.unit, s.checked));
            }
            imageUri = original.imageUri;
        }

        // System photo picker: no permission needed
        pickImage = registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(), uri -> {
                    if (uri == null) return;   // user backed out
                    String saved = copyToAppStorage(uri);
                    if (saved != null) {
                        imageUri = saved;
                        refreshImage();
                    }
                });
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
        return inflater.inflate(R.layout.dialog_meal, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        vm = new ViewModelProvider(requireActivity()).get(AppViewModel.class);

        boolean editable = isEditable();

        TextView title = view.findViewById(R.id.dialog_title);
        TextInputLayout layoutName = view.findViewById(R.id.layout_name);
        TextInputLayout layoutDescription = view.findViewById(R.id.layout_description);
        TextInputEditText inputName = view.findViewById(R.id.input_name);
        TextInputEditText inputDescription = view.findViewById(R.id.input_description);
        TextView textDescription = view.findViewById(R.id.text_description);
        MaterialButton btnAddIngredient = view.findViewById(R.id.btn_add_ingredient);
        MaterialButton btnConfirm = view.findViewById(R.id.btn_confirm);
        MaterialButton btnDelete = view.findViewById(R.id.btn_delete);
        RecyclerView ingredientsList = view.findViewById(R.id.ingredients_list);
        imageCard = view.findViewById(R.id.image_card);
        imageView = view.findViewById(R.id.meal_image);
        btnRemoveImage = view.findViewById(R.id.btn_remove_image);
        emptyIngredients = view.findViewById(R.id.empty_ingredients);

        // Title
        switch (mode) {
            case ADD:  title.setText(getString(R.string.add_meal)); break;
            case EDIT: title.setText(getString(R.string.edit_title, original.name)); break;
            default:   title.setText(original.name); break;
        }

        // Show the right parts for the mode
        layoutName.setVisibility(editable ? View.VISIBLE : View.GONE);
        layoutDescription.setVisibility(editable ? View.VISIBLE : View.GONE);
        btnAddIngredient.setVisibility(editable ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.footer_edit).setVisibility(editable ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.footer_view).setVisibility(mode == DialogMode.VIEW ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.footer_history).setVisibility(mode == DialogMode.HISTORY || mode == DialogMode.SAVED ? View.VISIBLE : View.GONE);
        btnDelete.setVisibility(mode == DialogMode.EDIT ? View.VISIBLE : View.GONE);
        btnConfirm.setText(mode == DialogMode.EDIT ? getString(R.string.done) : getString(R.string.add));

        if (!editable) {
            boolean hasDescription = original.description != null && !original.description.isEmpty();
            textDescription.setText(original.description);
            textDescription.setVisibility(hasDescription ? View.VISIBLE : View.GONE);
        }

        // Pre-fill in Edit mode (first creation only, so rotation keeps the user's edits)
        if (mode == DialogMode.EDIT && savedInstanceState == null) {
            inputName.setText(original.name);
            inputDescription.setText(original.description);
        }

        // Image
        if (editable) {
            imageCard.setOnClickListener(v -> pickImage.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build()));
            btnRemoveImage.setOnClickListener(v -> {
                imageUri = null;
                refreshImage();
            });
        }
        refreshImage();

        // Ingredients list
        ingredientAdapter = new SupplyAdapter(ingredients, SupplyAdapter.Mode.STATUS);
        ingredientsList.setLayoutManager(new LinearLayoutManager(requireContext()));
        ingredientsList.setAdapter(ingredientAdapter);
        refreshStatus();
        refreshEmptyState();

        if (editable) {
            new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                    ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {

                @Override
                public boolean onMove(@NonNull RecyclerView rv,
                                      @NonNull RecyclerView.ViewHolder from,
                                      @NonNull RecyclerView.ViewHolder to) {
                    ingredientAdapter.moveItem(from.getAdapterPosition(),
                            to.getAdapterPosition());
                    return true;
                }

                @Override
                public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int direction) {
                    // swipe disabled
                }
            }).attachToRecyclerView(ingredientsList);

            // Reuse the supply popup to add / edit an ingredient
            btnAddIngredient.setOnClickListener(v ->
                    SupplyDialog.newAdd().show(getChildFragmentManager(), "ingredient_dialog"));

            ingredientAdapter.setOnItemClickListener((supply, pos) ->
                    SupplyDialog.newEdit(pos, supply)
                            .show(getChildFragmentManager(), "ingredient_dialog"));

            getChildFragmentManager().setFragmentResultListener(
                    SupplyDialog.RESULT_KEY, getViewLifecycleOwner(), (key, result) -> {
                        int pos = result.getInt(SupplyDialog.KEY_POSITION);

                        if (result.getBoolean(SupplyDialog.KEY_DELETED)) {
                            ingredientAdapter.removeSupply(pos);
                        } else {
                            String name = result.getString(SupplyDialog.KEY_NAME);
                            double quantity = result.getDouble(SupplyDialog.KEY_QUANTITY);
                            Unit unit = Unit.values()[result.getInt(SupplyDialog.KEY_UNIT)];

                            if (pos == SupplyDialog.NEW_ITEM) {
                                ingredientAdapter.addSupply(new Supply(name, quantity, unit, false));
                            } else {
                                ingredientAdapter.updateSupply(pos, name, quantity, unit);
                            }
                        }
                        refreshStatus();
                        refreshEmptyState();
                    });
        }

        // Close / Cancel
        view.findViewById(R.id.btn_close).setOnClickListener(v -> dismiss());
        view.findViewById(R.id.btn_cancel).setOnClickListener(v -> dismiss());

        // Add / Done
        btnConfirm.setOnClickListener(v -> {
            String name = String.valueOf(inputName.getText()).trim();
            layoutName.setError(null);

            if (name.isEmpty()) {
                layoutName.setError(getString(R.string.error_name));
                return;
            }

            String description = String.valueOf(inputDescription.getText()).trim();
            Meal meal = new Meal(name, description, imageUri, new ArrayList<>(ingredients));
            sendResult(ACTION_SAVE, meal);
        });

        // Delete (Edit footer and View footer): confirm first
        if (original != null) {
            getChildFragmentManager().setFragmentResultListener(
                    ConfirmDialog.RESULT_KEY, getViewLifecycleOwner(),
                    (key, result) -> sendResult(
                            mode == DialogMode.SAVED ? ACTION_UNSAVE : ACTION_DELETE, null));

            View.OnClickListener askDelete = v -> ConfirmDialog.newInstance(
                            getString(R.string.delete_title, original.name),
                            getString(R.string.delete_message, original.name),
                            getString(R.string.delete))
                    .show(getChildFragmentManager(), "confirm_delete");

            btnDelete.setOnClickListener(askDelete);
            view.findViewById(R.id.btn_view_delete).setOnClickListener(askDelete);
        }

        // Eaten (does the same as delete for now)
        view.findViewById(R.id.btn_eaten).setOnClickListener(v -> sendResult(ACTION_EATEN, null));

        // Edit (View mode): swap this popup for the Edit one
        view.findViewById(R.id.btn_edit).setOnClickListener(v -> {
            androidx.fragment.app.FragmentManager fm = getParentFragmentManager();
            dismiss();
            MealDialog.newEdit(position, original).show(fm, "meal_dialog");
        });

        // History: put a copy of this meal back in the planned meals
        view.findViewById(R.id.btn_add_meal).setOnClickListener(v ->
                sendResult(ACTION_PLAN, original.copy()));

        MaterialButton btnHistorySave = view.findViewById(R.id.btn_history_save);
        btnHistorySave.setText(mode == DialogMode.SAVED ? getString(R.string.unsave) : getString(R.string.save));
        if (mode == DialogMode.SAVED) {
            btnHistorySave.setTextColor(android.graphics.Color.parseColor("#C62828"));
        }

        btnHistorySave.setOnClickListener(v -> {
            if (mode == DialogMode.SAVED) {
                ConfirmDialog.newInstance(
                                getString(R.string.unsave_title, original.name),
                                getString(R.string.unsave_message, original.name),
                                getString(R.string.unsave))
                        .show(getChildFragmentManager(), "confirm_unsave");
            } else {
                sendResult(ACTION_BOOKMARK, original);
            }
        });
    }

    private boolean isEditable() {
        return mode == DialogMode.ADD || mode == DialogMode.EDIT;
    }

    // Checkmark = enough in stock, cross = missing or not enough
    private void refreshStatus() {
        for (Supply s : ingredients) {
            s.checked = StockChecker.hasEnough(s, vm.supplies);
        }
        ingredientAdapter.notifyDataSetChanged();
    }

    private void sendResult(String action, @Nullable Meal meal) {
        Bundle result = new Bundle();
        result.putInt(KEY_POSITION, position);
        result.putString(KEY_ACTION, action);
        if (meal != null) result.putSerializable(KEY_MEAL, meal);
        getParentFragmentManager().setFragmentResult(RESULT_KEY, result);
        dismiss();
    }

    private void refreshImage() {
        boolean hasImage = imageUri != null;
        boolean editable = isEditable();

        if (hasImage) {
            Glide.with(this).load(Uri.parse(imageUri)).centerCrop().into(imageView);
        } else {
            Glide.with(this).clear(imageView);
        }
        // In View mode there is nothing to show without an image
        imageCard.setVisibility(editable || hasImage ? View.VISIBLE : View.GONE);
        btnRemoveImage.setVisibility(editable && hasImage ? View.VISIBLE : View.GONE);
    }

    private void refreshEmptyState() {
        emptyIngredients.setVisibility(ingredients.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Nullable
    private String copyToAppStorage(Uri source) {
        File dir = new File(requireContext().getFilesDir(), "meal_images");
        if (!dir.exists() && !dir.mkdirs()) return null;
        File target = new File(dir, UUID.randomUUID() + ".jpg");

        try {
            if (Build.VERSION.SDK_INT >= 28) {
                // Also applies the photo's rotation correctly
                Bitmap bitmap = ImageDecoder.decodeBitmap(
                        ImageDecoder.createSource(requireContext().getContentResolver(), source),
                        (decoder, info, src) -> {
                            int w = info.getSize().getWidth();
                            int h = info.getSize().getHeight();
                            float scale = Math.min(1f, 1280f / Math.max(w, h));
                            decoder.setTargetSize(Math.round(w * scale), Math.round(h * scale));
                        });
                try (OutputStream out = new FileOutputStream(target)) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out);
                }
            } else {
                // Older phones: plain copy
                try (InputStream in = requireContext().getContentResolver().openInputStream(source);
                     OutputStream out = new FileOutputStream(target)) {
                    if (in == null) return null;
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
                }
            }
            return Uri.fromFile(target).toString();
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(STATE_INGREDIENTS, new ArrayList<>(ingredients));
        outState.putString(STATE_IMAGE, imageUri);
    }

    @Override
    public void onStart() {
        super.onStart();

        Window window = getDialog().getWindow();
        if (window == null) return;

        int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.9);
        int height = (int) (getResources().getDisplayMetrics().heightPixels * 0.85);
        window.setLayout(width, height);
        window.setGravity(Gravity.CENTER);
        window.setBackgroundDrawableResource(R.drawable.bg_dialog);
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }
}