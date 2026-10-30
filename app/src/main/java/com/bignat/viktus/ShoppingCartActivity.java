package com.bignat.viktus;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bignat.viktus.R;
import com.bignat.viktus.model.Supply;
import com.bignat.viktus.model.Unit;
import com.bignat.viktus.ui.dialog.StockDialog;
import com.bignat.viktus.ui.adapter.SupplyAdapter;

import java.util.ArrayList;
import java.util.List;

public class ShoppingCartActivity extends AppCompatActivity {

    public static final String EXTRA_ITEMS = "items";

    private final List<Supply> items = new ArrayList<>();   // working copy
    private SupplyAdapter adapter;
    private View emptyText;

    @SuppressWarnings({"unchecked", "deprecation"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shopping_cart);

        // Rotation / process death keeps the edits, otherwise start from the shopping list
        List<Supply> start = (List<Supply>) (savedInstanceState != null
                ? savedInstanceState.getSerializable(EXTRA_ITEMS)
                : getIntent().getSerializableExtra(EXTRA_ITEMS));
        if (start != null) items.addAll(start);

        emptyText = findViewById(R.id.shopping_cart_empty);

        RecyclerView list = findViewById(R.id.shopping_cart);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SupplyAdapter(items, SupplyAdapter.Mode.NONE);
        list.setAdapter(adapter);

        // Back arrow: cancel everything
        findViewById(R.id.btn_back).setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        // Tap a row: edit its quantity (or delete it)
        adapter.setOnItemClickListener((supply, position) ->
                StockDialog.newEdit(position, supply)
                        .show(getSupportFragmentManager(), "stock_dialog"));

        // "+": new supply
        findViewById(R.id.fab_add).setOnClickListener(v ->
                StockDialog.newCreate().show(getSupportFragmentManager(), "stock_dialog"));

        getSupportFragmentManager().setFragmentResultListener(
                StockDialog.RESULT_KEY, this, (key, result) -> {
                    String action = result.getString(StockDialog.KEY_ACTION);
                    int index = result.getInt(StockDialog.KEY_INDEX);

                    if (StockDialog.ACTION_CREATE.equals(action)) {
                        Unit unit = Unit.values()[result.getInt(StockDialog.KEY_UNIT)];
                        // Starts at 1 here (not 0), since 0 would be ignored when buying
                        items.add(new Supply(result.getString(StockDialog.KEY_NAME), 1, unit, false));
                    } else if (index >= 0 && index < items.size()) {
                        if (StockDialog.ACTION_DELETE.equals(action)) {
                            items.remove(index);
                        } else {
                            items.get(index).quantity = result.getDouble(StockDialog.KEY_QUANTITY);
                        }
                    }
                    refresh();
                });

        // Done: send everything back to be added to the supplies
        findViewById(R.id.fab_done).setOnClickListener(v -> {
            Intent data = new Intent();
            data.putExtra(EXTRA_ITEMS, new ArrayList<>(items));
            setResult(RESULT_OK, data);
            finish();
        });

        refresh();
    }

    private void refresh() {
        adapter.notifyDataSetChanged();
        emptyText.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(EXTRA_ITEMS, new ArrayList<>(items));
    }
}