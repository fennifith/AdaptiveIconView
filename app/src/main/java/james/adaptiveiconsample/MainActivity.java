package james.adaptiveiconsample;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import james.adaptiveicon.AdaptiveIcon;
import james.adaptiveicon.utils.ConversionUtils;

public class MainActivity extends AppCompatActivity {

    private RecyclerAdapter adapter;
    private GridLayoutManager layoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RecyclerView recycler = findViewById(R.id.recycler);
        layoutManager = new GridLayoutManager(this, 4);
        recycler.setLayoutManager(layoutManager);

        List<ResolveInfo> infos = getPackageManager().queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), PackageManager.GET_META_DATA);
        Collections.sort(infos, new ResolveInfo.DisplayNameComparator(getPackageManager()));

        List<AdaptiveIcon> icons = new ArrayList<>();
        for (ResolveInfo info : infos) {
            AdaptiveIcon icon = new AdaptiveIcon.Loader()
                    .with(this)
                    .fallback(new AdaptiveIcon.Loader.LegacyIconFallback()
                            .withBackgroundColor(Color.GRAY))
                    .load(info);
            if (icon != null)
                icons.add(icon);
        }

        adapter = new RecyclerAdapter(icons);
        recycler.setAdapter(adapter);
        recycler.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                for (int i = layoutManager.findFirstVisibleItemPosition(); i <= layoutManager.findLastVisibleItemPosition(); i++) {
                    RecyclerAdapter.ViewHolder viewHolder = (RecyclerAdapter.ViewHolder) recyclerView.findViewHolderForAdapterPosition(i);
                    viewHolder.iconView.setOffset(0, Math.min(1, (float) dy / ConversionUtils.dpToPx(24)));
                }
                super.onScrolled(recyclerView, dx, dy);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuPathShape:
                new AlertDialog.Builder(this)
                        .setTitle(R.string.title_path_shape)
                        .setSingleChoiceItems(new CharSequence[]{
                                "Circle",
                                "Squircle",
                                "Rounded Square",
                                "Square",
                                "Teardrop"
                        }, adapter.getPathShape(), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                adapter.setPath(i);
                                dialogInterface.dismiss();
                            }
                        })
                        .show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
