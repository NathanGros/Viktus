package com.bignat.viktus;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class SuppliesFragment extends Fragment {
    private SupplyAdapter adapter;

    public SuppliesFragment() {
        super(R.layout.fragment_supplies);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView list = view.findViewById(R.id.supplies_list);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));

        AppViewModel vm = new ViewModelProvider(requireActivity()).get(AppViewModel.class);

        adapter = new SupplyAdapter(vm.supplies, SupplyAdapter.Mode.NONE);
        list.setAdapter(adapter);
        // Refresh once the database has been read
        vm.loaded.observe(getViewLifecycleOwner(), ok -> adapter.notifyDataSetChanged());

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
                });

        view.findViewById(R.id.fab_add_supply).setOnClickListener(v ->
                SupplyDialog.newAdd().show(getChildFragmentManager(), "supply_dialog"));

        adapter.setOnItemClickListener((supply, position) ->
                SupplyDialog.newEdit(position, supply)
                        .show(getChildFragmentManager(), "supply_dialog"));
    }
}
