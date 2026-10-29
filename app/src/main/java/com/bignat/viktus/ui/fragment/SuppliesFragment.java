package com.bignat.viktus.ui.fragment;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bignat.viktus.R;
import com.bignat.viktus.data.AppViewModel;
import com.bignat.viktus.logic.AvailableCalculator;
import com.bignat.viktus.model.Supply;
import com.bignat.viktus.model.Unit;
import com.bignat.viktus.ui.adapter.SupplyAdapter;
import com.bignat.viktus.ui.dialog.StockDialog;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SuppliesFragment extends Fragment {

    private AppViewModel vm;
    private SupplyAdapter adapter;
    private final List<Supply> shown = new ArrayList<>();   // filtered + sorted view of the stock
    private TextInputEditText inputSearch;
    private View emptyText;
    private View btnAdd;
    private ExtendedFloatingActionButton fabUsed;
    private boolean hideUsed = false;

    public SuppliesFragment() {
        super(R.layout.fragment_supplies);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        vm = new ViewModelProvider(requireActivity()).get(AppViewModel.class);
        inputSearch = view.findViewById(R.id.input_search);
        emptyText = view.findViewById(R.id.supplies_empty);
        btnAdd = view.findViewById(R.id.btn_add_supply);
        fabUsed = view.findViewById(R.id.fab_used);

        RecyclerView list = view.findViewById(R.id.supplies_list);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new SupplyAdapter(shown, SupplyAdapter.Mode.NONE);
        list.setAdapter(adapter);

        // Search as you type
        inputSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) { refresh(); }
        });

        // "+": new supply
        btnAdd.setOnClickListener(v ->
                StockDialog.newCreate().show(getChildFragmentManager(), "stock_dialog"));

        // Tap a row: edit its quantity
        adapter.setOnItemClickListener((supply, position) -> {
            int index = vm.supplies.indexOf(supply);   // same object, not a copy
            if (index < 0) return;                     // "hidden" view rows are computed copies
            StockDialog.newEdit(index, supply).show(getChildFragmentManager(), "stock_dialog");
        });

        getChildFragmentManager().setFragmentResultListener(
                StockDialog.RESULT_KEY, getViewLifecycleOwner(), (key, result) -> {
                    String action = result.getString(StockDialog.KEY_ACTION);
                    int index = result.getInt(StockDialog.KEY_INDEX);

                    if (StockDialog.ACTION_CREATE.equals(action)) {
                        Unit unit = Unit.values()[result.getInt(StockDialog.KEY_UNIT)];
                        String name = result.getString(StockDialog.KEY_NAME);
                        // New supplies start empty; "unspecified" ones have no quantity,
                        // so they start as "present"
                        double quantity = unit == Unit.UNSPECIFIED ? 1 : 0;
                        vm.supplies.add(new Supply(name, quantity, unit, false));
                    } else if (index >= 0 && index < vm.supplies.size()) {
                        if (StockDialog.ACTION_DELETE.equals(action)) {
                            vm.supplies.remove(index);
                        } else {
                            vm.supplies.get(index).quantity =
                                    result.getDouble(StockDialog.KEY_QUANTITY);
                        }
                    }
                    vm.saveSupplies();
                    refresh();
                });

        // "Used: shown / hidden"
        fabUsed.setOnClickListener(v -> {
            hideUsed = !hideUsed;
            fabUsed.setText(hideUsed ? R.string.used_hidden : R.string.used_shown);
            btnAdd.setVisibility(hideUsed ? View.GONE : View.VISIBLE);   // hidden view is read-only
            refresh();
        });

        vm.loaded.observe(getViewLifecycleOwner(), ok -> refresh());
    }

    // Stock may have changed on another page (Eaten, clear data...)
    @Override
    public void onResume() {
        super.onResume();
        if (adapter != null) refresh();
    }

    private void refresh() {
        List<Supply> source = hideUsed
                ? AvailableCalculator.compute(vm.supplies, vm.meals)
                : vm.supplies;
        String query = String.valueOf(inputSearch.getText()).trim().toLowerCase(Locale.ROOT);

        // In stock first, empty ones at the bottom; each group keeps its own order
        List<Supply> inStock = new ArrayList<>();
        List<Supply> empty = new ArrayList<>();
        for (Supply s : source) {
            if (!query.isEmpty() && !s.name.toLowerCase(Locale.ROOT).contains(query)) continue;
            if (isEmpty(s)) empty.add(s); else inStock.add(s);
        }
        shown.clear();
        shown.addAll(inStock);
        shown.addAll(empty);
        adapter.notifyDataSetChanged();

        boolean loaded = Boolean.TRUE.equals(vm.loaded.getValue());
        emptyText.setVisibility(loaded && shown.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private static boolean isEmpty(Supply s) {
        return s.unit != Unit.UNSPECIFIED && s.quantity <= 1e-9;
    }
}