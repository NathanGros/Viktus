package com.bignat.viktus.ui.fragment;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bignat.viktus.R;
import com.bignat.viktus.model.Unit;
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
        view.findViewById(R.id.fab_copy_clipboard).setOnClickListener(v -> copyToClipboard());
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

    private static boolean isEmpty(Supply s) {
        return s.unit != Unit.UNSPECIFIED && s.quantity <= 1e-9;
    }

    private void copyToClipboard() {
        StringBuilder sb = new StringBuilder();

        for (Supply s : items) {
            if (isEmpty(s)) continue;   // skip supplies at 0 quantity

            if (sb.length() > 0) sb.append("\n");
            sb.append(s.name);

            // "Unspecified" supplies have no quantity, so only the name is written
            if (s.unit != Unit.UNSPECIFIED) {
                sb.append(":");
                String number = (s.quantity == Math.floor(s.quantity))
                        ? String.valueOf((long) s.quantity)   // 2.0 -> "2"
                        : String.valueOf(s.quantity);
                sb.append(" ").append(number).append(" ").append(getString(s.unit.labelRes));
            }
        }

        if (sb.length() == 0) {
            Toast.makeText(requireContext(), R.string.toast_nothing_to_copy,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        ClipboardManager clipboard = (ClipboardManager)
                requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("Supplies", sb.toString()));

        // Android 13+ already shows its own "Copied" confirmation
        if (Build.VERSION.SDK_INT < 33) {
            Toast.makeText(requireContext(), R.string.toast_copied, Toast.LENGTH_SHORT).show();
        }
    }
}