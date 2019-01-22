package james.adaptiveiconsample;

import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import james.adaptiveicon.AdaptiveIcon;
import james.adaptiveicon.AdaptiveIconView;

public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.ViewHolder> {

    private List<AdaptiveIcon> icons;
    private int path = AdaptiveIconView.PATH_CIRCLE;

    public RecyclerAdapter(List<AdaptiveIcon> icons) {
        this.icons = icons;
    }

    public void setPath(int path) {
        this.path = path;
        notifyDataSetChanged();
    }

    public int getPathShape() {
        return path;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_icon, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        AdaptiveIcon icon = icons.get(position);
        holder.iconView.setIcon(icon);
        holder.iconView.setPath(path);
    }

    @Override
    public int getItemCount() {
        return icons.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public AdaptiveIconView iconView;

        public ViewHolder(View itemView) {
            super(itemView);
            iconView = itemView.findViewById(R.id.icon);
        }
    }
}
