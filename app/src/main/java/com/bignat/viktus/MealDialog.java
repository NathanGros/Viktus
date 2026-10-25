package com.bignat.viktus;

import android.app.Dialog;
import android.net.Uri;
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
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

    public static final int NEW_ITEM = -1;

    private static final String ARG_MODE = "mode";
    private static final String STATE_INGREDIENTS = "state_ingredients";
    private static final String STATE_IMAGE = "state_image";

    private enum DialogMode { ADD, EDIT, VIEW }

    public static MealDialog newAdd() {
        return create(DialogMode.ADD, NEW_ITEM, null);
    }

    public static MealDialog newEdit(int position, Meal meal) {
        return create(DialogMode.EDIT, position, meal);
    }

    public static MealDialog newView(int position, Meal meal) {
        return create(DialogMode.VIEW, position, meal);
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

        boolean editable = mode != DialogMode.VIEW;

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
            case ADD:  title.setText("Add meal"); break;
            case EDIT: title.setText("Edit " + original.name); break;
            default:   title.setText(original.name); break;
        }

        // Show the right parts for the mode
        layoutName.setVisibility(editable ? View.VISIBLE : View.GONE);
        layoutDescription.setVisibility(editable ? View.VISIBLE : View.GONE);
        btnAddIngredient.setVisibility(editable ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.footer_edit).setVisibility(editable ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.footer_view).setVisibility(editable ? View.GONE : View.VISIBLE);
        btnDelete.setVisibility(mode == DialogMode.EDIT ? View.VISIBLE : View.GONE);
        btnConfirm.setText(mode == DialogMode.EDIT ? "Done" : "Add");

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
                layoutName.setError("Enter a name");
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
                    (key, result) -> sendResult(ACTION_DELETE, null));

            View.OnClickListener askDelete = v -> ConfirmDialog.newInstance(
                            "Delete " + original.name + " ?",
                            "Are you sure you want to delete " + original.name + " ?",
                            "Delete")
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
        boolean editable = mode != DialogMode.VIEW;

        imageView.setImageURI(hasImage ? Uri.parse(imageUri) : null);
        // In View mode there is nothing to show without an image
        imageCard.setVisibility(editable || hasImage ? View.VISIBLE : View.GONE);
        btnRemoveImage.setVisibility(editable && hasImage ? View.VISIBLE : View.GONE);
    }

    private void refreshEmptyState() {
        emptyIngredients.setVisibility(ingredients.isEmpty() ? View.VISIBLE : View.GONE);
    }

    // The picker only gives temporary access to the photo, so keep our own copy
    @Nullable
    private String copyToAppStorage(Uri source) {
        File dir = new File(requireContext().getFilesDir(), "meal_images");
        if (!dir.exists() && !dir.mkdirs()) return null;

        File target = new File(dir, UUID.randomUUID() + ".jpg");
        try (InputStream in = requireContext().getContentResolver().openInputStream(source);
             OutputStream out = new FileOutputStream(target)) {
            if (in == null) return null;
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
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