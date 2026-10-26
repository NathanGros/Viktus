package com.bignat.viktus.ui.fragment;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bignat.viktus.R;
import com.bignat.viktus.logic.AvailableCalculator;
import com.bignat.viktus.ui.adapter.SupplyAdapter;
import com.bignat.viktus.data.AppViewModel;
import com.bignat.viktus.model.Supply;
import com.bignat.viktus.model.Unit;
import com.bignat.viktus.ui.dialog.SupplyDialog;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class SuppliesFragment extends Fragment {
    private SupplyAdapter adapter;
    private AppViewModel vm;
    private View emptyText;
    private RecyclerView list;
    private View fabAdd;
    private ExtendedFloatingActionButton fabUsed;
    private SupplyAdapter hiddenAdapter;
    private final List<Supply> hiddenItems = new ArrayList<>();
    private boolean hideUsed = false;

    public SuppliesFragment() {
        super(R.layout.fragment_supplies);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        list = view.findViewById(R.id.supplies_list);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));

        vm = new ViewModelProvider(requireActivity()).get(AppViewModel.class);
        emptyText = view.findViewById(R.id.supplies_empty);

        adapter = new SupplyAdapter(vm.supplies, SupplyAdapter.Mode.NONE);
        list.setAdapter(adapter);

        // Second adapter for the "hidden" view: a computed list with no click listener
        hiddenAdapter = new SupplyAdapter(hiddenItems, SupplyAdapter.Mode.NONE);

        fabAdd = view.findViewById(R.id.fab_add_supply);
        fabUsed = view.findViewById(R.id.fab_used);
        fabUsed.setOnClickListener(v -> {
            hideUsed = !hideUsed;
            applyMode();
        });

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
                    public boolean isLongPressDragEnabled() {
                        return !hideUsed;
                    }

                    @Override
                    public void clearView(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh) {
                        super.clearView(rv, vh);
                        vm.saveSupplies();
                    }
                });
        helper.attachToRecyclerView(list);

        // Single listener for add, edit and delete
        getChildFragmentManager().setFragmentResultListener(
                SupplyDialog.RESULT_KEY, getViewLifecycleOwner(), (key, result) -> {
                    int position = result.getInt(SupplyDialog.KEY_POSITION);

                    if (result.getBoolean(SupplyDialog.KEY_DELETED)) {
                        adapter.removeSupply(position);
                        vm.saveSupplies();
                        refreshEmptyState();
                        return;
                    }

                    String name = result.getString(SupplyDialog.KEY_NAME);
                    double quantity = result.getDouble(SupplyDialog.KEY_QUANTITY);
                    Unit unit = Unit.values()[result.getInt(SupplyDialog.KEY_UNIT)];

                    if (position == SupplyDialog.NEW_ITEM) {
                        adapter.addSupply(new Supply(name, quantity, unit, false));
                    } else {
                        adapter.updateSupply(position, name, quantity, unit);
                    }
                    vm.saveSupplies();
                    refreshEmptyState();
                });

        view.findViewById(R.id.fab_add_supply).setOnClickListener(v ->
                SupplyDialog.newAdd().show(getChildFragmentManager(), "supply_dialog"));

        adapter.setOnItemClickListener((supply, position) ->
                SupplyDialog.newEdit(position, supply)
                        .show(getChildFragmentManager(), "supply_dialog"));
    }

    private void refreshEmptyState() {
        // Wait for the database to be read, otherwise the text flashes at startup
        boolean loaded = Boolean.TRUE.equals(vm.loaded.getValue());
        emptyText.setVisibility(loaded && vm.supplies.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void applyMode() {
        fabUsed.setText(hideUsed ? "Used: hidden" : "Used: shown");
        fabAdd.setVisibility(hideUsed ? View.GONE : View.VISIBLE);

        if (hideUsed) {
            refreshHidden();
            list.setAdapter(hiddenAdapter);
        } else {
            adapter.notifyDataSetChanged();
            list.setAdapter(adapter);
        }
    }

    private void refreshHidden() {
        hiddenItems.clear();
        hiddenItems.addAll(AvailableCalculator.compute(vm.supplies, vm.meals));
        hiddenAdapter.notifyDataSetChanged();
    }

    // Planned meals may have changed while you were on another page
    @Override
    public void onResume() {
        super.onResume();
        if (hiddenAdapter != null && hideUsed) refreshHidden();
    }
}
