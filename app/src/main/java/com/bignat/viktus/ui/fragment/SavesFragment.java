package com.bignat.viktus.ui.fragment;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bignat.viktus.ui.adapter.MealAdapter;
import com.bignat.viktus.R;
import com.bignat.viktus.data.AppViewModel;
import com.bignat.viktus.model.Meal;
import com.bignat.viktus.ui.dialog.MealDialog;

public class SavesFragment extends Fragment {

    private MealAdapter adapter;
    private AppViewModel vm;
    private View emptyText;

    public SavesFragment() {
        super(R.layout.fragment_saves);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        vm = new ViewModelProvider(requireActivity()).get(AppViewModel.class);
        emptyText = view.findViewById(R.id.saves_empty);

        RecyclerView list = view.findViewById(R.id.saves_list);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new MealAdapter(vm.saved);
        list.setAdapter(adapter);

        // Long press + drag to reorder
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {

            @Override
            public boolean onMove(@NonNull RecyclerView rv,
                                  @NonNull RecyclerView.ViewHolder from,
                                  @NonNull RecyclerView.ViewHolder to) {
                adapter.moveItem(from.getAdapterPosition(), to.getAdapterPosition());
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int direction) { }

            @Override
            public void clearView(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh) {
                super.clearView(rv, vh);
                vm.saveSaved();   // the drag ended, store the new order
            }
        }).attachToRecyclerView(list);

        // Tap a row: popup with Unsave / Add to planned meals
        adapter.setOnItemClickListener((meal, position) ->
                MealDialog.newSaved(position, meal).show(getChildFragmentManager(), "saved_dialog"));

        getChildFragmentManager().setFragmentResultListener(
                MealDialog.RESULT_KEY, getViewLifecycleOwner(), (key, result) -> {
                    String action = result.getString(MealDialog.KEY_ACTION);

                    if (MealDialog.ACTION_UNSAVE.equals(action)) {
                        adapter.removeMeal(result.getInt(MealDialog.KEY_POSITION));
                        vm.saveSaved();
                        refreshEmptyState();
                    } else if (MealDialog.ACTION_PLAN.equals(action)) {
                        Meal meal = (Meal) result.getSerializable(MealDialog.KEY_MEAL);
                        vm.meals.add(meal);
                        vm.saveMeals();
                        Toast.makeText(requireContext(),
                                meal.name + " added to planned meals",
                                Toast.LENGTH_SHORT).show();
                    }
                });

        vm.loaded.observe(getViewLifecycleOwner(), ok -> {
            adapter.notifyDataSetChanged();
            refreshEmptyState();
        });
    }

    // Runs each time you swipe to this page, so meals saved from History show up
    @Override
    public void onResume() {
        super.onResume();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
            refreshEmptyState();
        }
    }

    private void refreshEmptyState() {
        boolean loaded = Boolean.TRUE.equals(vm.loaded.getValue());
        emptyText.setVisibility(loaded && vm.saved.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
