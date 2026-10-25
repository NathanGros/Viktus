package com.bignat.viktus;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MealsFragment extends Fragment {

    private MealAdapter adapter;
    private AppViewModel vm;
    private View emptyText;

    public MealsFragment() {
        super(R.layout.fragment_meals);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView list = view.findViewById(R.id.meals_list);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));

        vm = new ViewModelProvider(requireActivity()).get(AppViewModel.class);
        emptyText = view.findViewById(R.id.meals_empty);

        adapter = new MealAdapter(vm.meals);
        list.setAdapter(adapter);

        vm.loaded.observe(getViewLifecycleOwner(), ok -> {
            adapter.notifyDataSetChanged();
            refreshEmptyState();
        });

        // Long press + drag to reorder
        ItemTouchHelper helper = new ItemTouchHelper(
                new ItemTouchHelper.SimpleCallback(
                        ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {

                    @Override
                    public boolean onMove(@NonNull RecyclerView rv,
                                          @NonNull RecyclerView.ViewHolder from,
                                          @NonNull RecyclerView.ViewHolder to) {
                        adapter.moveItem(from.getAdapterPosition(),
                                to.getAdapterPosition());
                        return true;
                    }

                    @Override
                    public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int direction) {
                        // swipe disabled
                    }

                    @Override
                    public void clearView(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh) {
                        super.clearView(rv, vh);
                        vm.saveMeals();
                    }
                });
        helper.attachToRecyclerView(list);

        // Single listener for add, edit, delete and eaten
        getChildFragmentManager().setFragmentResultListener(
                MealDialog.RESULT_KEY, getViewLifecycleOwner(), (key, result) -> {
                    int position = result.getInt(MealDialog.KEY_POSITION);
                    String action = result.getString(MealDialog.KEY_ACTION);

                    if (MealDialog.ACTION_DELETE.equals(action) || MealDialog.ACTION_EATEN.equals(action)) {
                        adapter.removeMeal(position);
                        vm.saveMeals();
                        refreshEmptyState();
                        return;
                    }

                    Meal meal = (Meal) result.getSerializable(MealDialog.KEY_MEAL);
                    if (position == MealDialog.NEW_ITEM) {
                        adapter.addMeal(meal);
                    } else {
                        adapter.updateMeal(position, meal);
                    }
                    vm.saveMeals();
                    refreshEmptyState();
                });

        // "+" button: add mode
        view.findViewById(R.id.fab_add_meal).setOnClickListener(v ->
                MealDialog.newAdd().show(getChildFragmentManager(), "meal_dialog"));

        // Row tap: view mode
        adapter.setOnItemClickListener((meal, position) ->
                MealDialog.newView(position, meal).show(getChildFragmentManager(), "meal_dialog"));
    }

    private void refreshEmptyState() {
        // Wait for the database to be read, otherwise the text flashes at startup
        boolean loaded = Boolean.TRUE.equals(vm.loaded.getValue());
        emptyText.setVisibility(loaded && vm.meals.isEmpty() ? View.VISIBLE : View.GONE);
    }
}