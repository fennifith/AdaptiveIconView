package james.adaptiveiconsample;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RecyclerView recycler = findViewById(R.id.recycler);
        recycler.setLayoutManager(new GridLayoutManager(this, 2));

        List<ResolveInfo> infos = getPackageManager().queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), PackageManager.GET_META_DATA);
        Collections.sort(infos, new ResolveInfo.DisplayNameComparator(getPackageManager()));

        List<IconItem> icons = new ArrayList<>();
        for (ResolveInfo info : infos) {
            Drawable bgDrawable = null, fgDrawable = null;

            try {
                Resources resources = getPackageManager().getResourcesForApplication(info.activityInfo.packageName);
                Resources.Theme theme = resources.newTheme();

                try {
                    bgDrawable = ResourcesCompat.getDrawable(resources, resources.getIdentifier("ic_launcher_background", "mipmap", info.activityInfo.packageName), theme);
                } catch (Exception e) {
                    bgDrawable = ResourcesCompat.getDrawable(resources, resources.getIdentifier("ic_launcher_background", "drawable", info.activityInfo.packageName), theme);
                }

                try {
                    fgDrawable = ResourcesCompat.getDrawable(resources, resources.getIdentifier("ic_launcher_foreground", "mipmap", info.activityInfo.packageName), theme);
                } catch (Exception e) {
                    fgDrawable = ResourcesCompat.getDrawable(resources, resources.getIdentifier("ic_launcher_foreground", "drawable", info.activityInfo.packageName), theme);
                }
            } catch (Exception e) {
            }

            if (bgDrawable == null) {
                try {
                    bgDrawable = getPackageManager().getApplicationIcon(info.activityInfo.packageName);
                    fgDrawable = null;
                } catch (Exception e) {
                    continue;
                }
                continue;
            }

            icons.add(new IconItem(bgDrawable, fgDrawable));
        }

        recycler.setAdapter(new RecyclerAdapter(icons));
    }
}
