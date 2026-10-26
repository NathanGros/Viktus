package com.bignat.viktus.ui.fragment;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bignat.viktus.R;
import com.bignat.viktus.ui.adapter.SupplyAdapter;
import com.bignat.viktus.data.AppViewModel;
import com.bignat.viktus.logic.ShoppingCalculator;
import com.bignat.viktus.model.Supply;

import java.util.ArrayList;
import java.util.List;

public class ShoppingFragment extends Fragment {

    private final List<Supply> items = new ArrayList<>();
    private SupplyAdapter adapter;
    private AppViewModel vm;
    private View emptyText;

    public ShoppingFragment() {
        super(R.layout.fragment_shopping);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        vm = new ViewModelProvider(requireActivity()).get(AppViewModel.class);
        emptyText = view.findViewById(R.id.shopping_empty);

        RecyclerView list = view.findViewById(R.id.shopping_list);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new SupplyAdapter(items, SupplyAdapter.Mode.NONE);
        list.setAdapter(adapter);

        // Recompute once the database has been read
        vm.loaded.observe(getViewLifecycleOwner(), ok -> refresh());
    }

    // With the pager, onResume runs each time you swipe to this page,
    // so the list always reflects the latest meals and supplies
    @Override
    public void onResume() {
        super.onResume();
        if (adapter != null) refresh();
    }

    private void refresh() {
        items.clear();
        items.addAll(ShoppingCalculator.compute(vm.meals, vm.supplies));
        adapter.notifyDataSetChanged();
        emptyText.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
    }
}