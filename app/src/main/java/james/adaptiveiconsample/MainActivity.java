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
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import james.adaptiveicon.AdaptiveIcon;
import james.adaptiveicon.utils.ConversionUtils;

public class MainActivity extends AppCompatActivity {

    private RecyclerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RecyclerView recycler = findViewById(R.id.recycler);
        recycler.setLayoutManager(new GridLayoutManager(this, 4));

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

        new ItemTouchHelper(new ItemTouchHelper.Callback() {
            int prevX;
            int prevY;

            @Override
            public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                return makeFlag(ItemTouchHelper.ACTION_STATE_DRAG, ItemTouchHelper.DOWN | ItemTouchHelper.UP | ItemTouchHelper.START | ItemTouchHelper.END);
            }

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {

            }

            @Override
            public RecyclerView.ViewHolder chooseDropTarget(RecyclerView.ViewHolder selected, List<RecyclerView.ViewHolder> dropTargets, int curX, int curY) {
                if (selected instanceof RecyclerAdapter.ViewHolder) {
                    int xDiff = curX - prevX;
                    int yDiff = curY - prevY;
                    int pixels = ConversionUtils.dpToPx(48);

                    xDiff = xDiff > 0 ? Math.min(xDiff, pixels) : Math.max(xDiff, -pixels);
                    yDiff = yDiff > 0 ? Math.min(yDiff, pixels) : Math.max(yDiff, -pixels);

                    ((RecyclerAdapter.ViewHolder) selected).iconView.onMovement((float) xDiff / pixels, (float) yDiff / pixels);
                    Log.d("Movement", (float) xDiff / pixels + ", " + (float) yDiff / pixels);
                }

                prevX = curX;
                prevY = curY;

                return super.chooseDropTarget(selected, dropTargets, curX, curY);
            }
        }).attachToRecyclerView(recycler);
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
