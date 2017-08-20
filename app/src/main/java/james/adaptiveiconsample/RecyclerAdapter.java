package james.adaptiveiconsample;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import james.adaptiveicon.AdaptiveIconView;

public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.ViewHolder> {

    private List<IconItem> icons;

    public RecyclerAdapter(List<IconItem> icons) {
        this.icons = icons;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_icon, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        IconItem icon = icons.get(position);
        holder.iconView.setDrawable(icon.bgDrawable, icon.fgDrawable);
        holder.iconView.setAdaptive(icon.fgDrawable != null);
    }

    @Override
    public int getItemCount() {
        return icons.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private AdaptiveIconView iconView;

        public ViewHolder(View itemView) {
            super(itemView);
            iconView = itemView.findViewById(R.id.icon);
        }
    }
}
