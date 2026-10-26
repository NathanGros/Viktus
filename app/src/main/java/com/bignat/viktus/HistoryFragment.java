package com.bignat.viktus;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class HistoryFragment extends Fragment {

    private MealAdapter adapter;
    private AppViewModel vm;
    private View emptyText;

    public HistoryFragment() {
        super(R.layout.fragment_history);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        vm = new ViewModelProvider(requireActivity()).get(AppViewModel.class);
        emptyText = view.findViewById(R.id.history_empty);

        RecyclerView list = view.findViewById(R.id.history_list);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new MealAdapter(vm.history);
        list.setAdapter(adapter);

        // Tap a row: read-only popup (no drag to reorder here)
        adapter.setOnItemClickListener((meal, position) ->
                MealDialog.newHistory(meal).show(getChildFragmentManager(), "history_dialog"));

        vm.loaded.observe(getViewLifecycleOwner(), ok -> refresh());

        getChildFragmentManager().setFragmentResultListener(
                MealDialog.RESULT_KEY, getViewLifecycleOwner(), (key, result) -> {
                    if (MealDialog.ACTION_PLAN.equals(result.getString(MealDialog.KEY_ACTION))) {
                        Meal meal = (Meal) result.getSerializable(MealDialog.KEY_MEAL);
                        vm.meals.add(meal);
                        vm.saveMeals();
                        android.widget.Toast.makeText(requireContext(),
                                meal.name + " added to planned meals",
                                android.widget.Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Runs each time you swipe to this page, so new Eaten meals show up
    @Override
    public void onResume() {
        super.onResume();
        if (adapter != null) refresh();
    }

    private void refresh() {
        adapter.notifyDataSetChanged();
        boolean loaded = Boolean.TRUE.equals(vm.loaded.getValue());
        emptyText.setVisibility(loaded && vm.history.isEmpty() ? View.VISIBLE : View.GONE);
    }
}