package ymbab.bot;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.logging.Handler;


public class MainActivity extends Activity {
    public static final String LOG = "#ymbab-bot";

    public static final String ymbab = "com.eightyeightgames.ymbab";

    public static MainActivity instance = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;
        Cfg.Init(this);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);


        if (!isAppInstalled(this, ymbab)) {
            setContentView(R.layout.activity_no_game);
            return;
        }
        setContentView(R.layout.activity_main);
    }

    public void onLaunch(View view) {
        startService(new Intent(this, FilterService.class));
    }

    public void onBuy(View view) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + ymbab)));
        } catch (android.content.ActivityNotFoundException anfe) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + ymbab)));
        }
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public static boolean isAppInstalled(Context context, String packageName) {
        try {
            context.getPackageManager().getApplicationInfo(packageName, 0);
            return true;
        }
        catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
    public static boolean openApp(Context context, String packageName) {
        PackageManager manager = context.getPackageManager();
        Intent i = manager.getLaunchIntentForPackage(packageName);
        if (i == null) {
            return false;
        }
        i.addCategory(Intent.CATEGORY_LAUNCHER);
        context.startActivity(i);
        return true;
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent resultData) {
        if (requestCode == FilterService.REQUEST_MEDIA_PROJECTION) {
            if (resultCode != Activity.RESULT_OK) {
                Log.i(LOG, "User cancelled");
                stopService(new Intent(this, FilterService.class));
                finish();
                return;
            }
            Log.i(LOG, "Starting screen capture");
            setContentView(R.layout.screen_capture);

            final android.os.Handler handler = new android.os.Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            SurfaceView surfaceView = (SurfaceView) findViewById(R.id.surfaceView);

                            MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

                            WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
                            DisplayMetrics metrics = new DisplayMetrics();
                            windowManager.getDefaultDisplay().getMetrics(metrics);
                            Log.i(LOG, "Starting screen capture: width " + surfaceView.getWidth() + " height " + surfaceView.getHeight() + " density " + metrics.densityDpi);

                            MediaProjection mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, resultData);
                            VirtualDisplay virtualDisplay = mediaProjection.createVirtualDisplay("ScreenCapture",
                                    surfaceView.getWidth(), surfaceView.getHeight(), metrics.densityDpi,
                                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                                    surfaceView.getHolder().getSurface(), null, null);

                            FilterService.instance.initFromActivity(mediaProjection, virtualDisplay, surfaceView);
                            openApp(MainActivity.this, ymbab);
                        }
                    });
                }
            }, 500);
        }
    }

}
