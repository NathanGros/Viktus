package com.bignat.viktus.ui.adapter;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bignat.viktus.R;
import com.bignat.viktus.model.Meal;
import com.bumptech.glide.Glide;

import java.util.List;

public class MealAdapter extends RecyclerView.Adapter<MealAdapter.ViewHolder> {

    private final List<Meal> meals;

    public interface OnItemClickListener {
        void onItemClick(Meal meal, int position);
    }

    private OnItemClickListener clickListener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.clickListener = listener;
    }

    public MealAdapter(List<Meal> meals) {
        this.meals = meals;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final View imageCard;
        final ImageView image;
        final TextView name;

        ViewHolder(View itemView) {
            super(itemView);
            imageCard = itemView.findViewById(R.id.meal_image_card);
            image = itemView.findViewById(R.id.meal_image);
            name = itemView.findViewById(R.id.meal_name);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_meal, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Meal meal = meals.get(position);
        holder.name.setText(meal.name);

        if (meal.imageUri != null) {
            holder.imageCard.setVisibility(View.VISIBLE);
            Glide.with(holder.itemView)
                    .load(Uri.parse(meal.imageUri))
                    .centerCrop()
                    .into(holder.image);
        } else {
            holder.imageCard.setVisibility(View.GONE);
            Glide.with(holder.itemView).clear(holder.image);   // rows are recycled
        }
        holder.itemView.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (clickListener != null && pos != RecyclerView.NO_POSITION) {
                clickListener.onItemClick(meals.get(pos), pos);
            }
        });
    }

    @Override
    public int getItemCount() {
        return meals.size();
    }

    public void moveItem(int from, int to) {
        Meal meal = meals.remove(from);
        meals.add(to, meal);
        notifyItemMoved(from, to);
    }

    public void addMeal(Meal meal) {
        meals.add(meal);
        notifyItemInserted(meals.size() - 1);
    }

    public void updateMeal(int position, Meal meal) {
        meals.set(position, meal);
        notifyItemChanged(position);
    }

    public void removeMeal(int position) {
        meals.remove(position);
        notifyItemRemoved(position);
    }
}