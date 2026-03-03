package com.example.labelmaker;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class RateListAdapter extends RecyclerView.Adapter<RateListAdapter.RateViewHolder> {

    private List<RateItem> rateItems;

    public RateListAdapter() {
        this.rateItems = new ArrayList<>();
    }

    @NonNull
    @Override
    public RateViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.rate_list_item, parent, false);
        return new RateViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RateViewHolder holder, int position) {
        RateItem item = rateItems.get(position);
        holder.productName.setText(item.getProductName());
        holder.oldRate.setText(item.getOldRate());
        holder.newRate.setText(item.getNewRate());

        // Alternating row colors for spreadsheet look
        if (position % 2 == 0) {
            holder.itemView.setBackgroundColor(android.graphics.Color.parseColor("#FFFFFF")); // White
        } else {
            holder.itemView.setBackgroundColor(android.graphics.Color.parseColor("#F5F8F8")); // Light background from Stitch
        }
    }

    @Override
    public int getItemCount() {
        return rateItems.size();
    }

    public void addItem(RateItem item) {
        rateItems.add(item);
        notifyItemInserted(rateItems.size() - 1);
    }

    public void clearItems() {
        int size = rateItems.size();
        rateItems.clear();
        notifyItemRangeRemoved(0, size);
    }

    public List<RateItem> getItems() {
        return rateItems;
    }

    static class RateViewHolder extends RecyclerView.ViewHolder {
        TextView productName;
        TextView oldRate;
        TextView newRate;

        public RateViewHolder(@NonNull View itemView) {
            super(itemView);
            productName = itemView.findViewById(R.id.product_name);
            oldRate = itemView.findViewById(R.id.old_rate);
            newRate = itemView.findViewById(R.id.new_rate);
        }
    }
}
