package ymbab.bot;

import android.app.Activity;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Handler;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;


public class FilterService extends Service  {
    public static final String LOG = "#ymbab-bot";

    public static boolean DEBUG = false; // true;

    private WindowManager windowManager;
    private ImageView view;
    private Bitmap bmp;
    private Canvas draw;
    //private Timer timer;
    private boolean destroyed = false;
    private boolean intentProcessed = false;
    public static boolean running = false;

    public static final int W = 480;
    public static final int H = 800;
    public static final int TIMEOUT = 300;
    public static float playLeft = 0.020f * W;
    public static float playRight = 0.022f * W;
    public static float playTop = 0.24f * H;
    public static float playBottom = 0.046f * H;
    public static int playCountW = 6;
    public static int playCountH = 8;
    public static float playBoxW = (W - playLeft - playRight) / (float)playCountW;
    public static float playBoxH = (H - playTop - playBottom) / (float)playCountH;

    private SurfaceView surfaceView = null;
    private MediaProjection mediaProjection = null;
    private VirtualDisplay virtualDisplay = null;
    private MediaProjectionManager mediaProjectionManager = null;

    public static final int REQUEST_MEDIA_PROJECTION = 333;

    public static FilterService instance = null;

    @Override
    public IBinder onBind(Intent intent) {
    return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        running = true;

        instance = this;

        Log.d(LOG, "Service started");
        Cfg.Init(this);

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        MainActivity.instance.runOnUiThread(new Runnable() {
            public void run() {
                MainActivity.instance.startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), REQUEST_MEDIA_PROJECTION);
            }
        });
    }

    public void initFromActivity(MediaProjection mediaProjection, VirtualDisplay virtualDisplay, SurfaceView surfaceView) {
        this.mediaProjection = mediaProjection;
        this.virtualDisplay = virtualDisplay;
        this.surfaceView = surfaceView;
        final Handler handler = new Handler(getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                initDrawOverlay();
            }
        });
    }

    private void initDrawOverlay() {

        Log.i(LOG, "initDrawOverlay");
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);

        view = new ImageView(this);

        bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_4444);

        view.setImageBitmap(bmp);
        view.setScaleType(ImageView.ScaleType.FIT_XY);
        draw = new Canvas(bmp);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY, //WindowManager.LayoutParams.TYPE_SYSTEM_ERROR, //WindowManager.LayoutParams.TYPE_SYSTEM_ALERT, //WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                WindowManager.LayoutParams.FLAG_FULLSCREEN |
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                //WindowManager.LayoutParams.FLAG_DIM_BEHIND,
                PixelFormat.RGBA_8888
        );

        windowManager.addView(view, params);

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                updatePattern();
                view.invalidate();
                if (!destroyed) {
                    handler.postDelayed(this, TIMEOUT);
                }
            }
        }, TIMEOUT);
    }

    @Override
    public int onStartCommand (Intent intent, int flags, int startId) {
        if (intent != null && (Intent.ACTION_DELETE.equals(intent.getAction()) ||
                Intent.ACTION_INSERT.equals(intent.getAction()) && intentProcessed)) {
            Log.d(LOG, "Service got shutdown intent");
            Ntf.show(this, false);
            stopSelf();
            intentProcessed = true;
            return START_NOT_STICKY;
        }

        Ntf.show(this, true);
        intentProcessed = true;
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        destroyed = true;
        if (view != null) {
            windowManager.removeView(view);
        }
        Ntf.show(this, false);
        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
        }
        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
        }
        surfaceView = null;
        MainActivity.instance.runOnUiThread(new Runnable() {
            public void run() {
                MainActivity.instance.finish();
            }
        });
        Log.d(LOG, "Service stopped");
        running = false;
    }

    private int logoTicks = 6;

    void updatePattern() {
        if (logoTicks > 0) {
            logoTicks--;
            updatePatternShowLogo();
        } else {
            updatePatternShowArrows();
        }
    }

    private static Paint textPaint = new Paint();
    static {
        textPaint.setTextSize(30);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.YELLOW);
    }
    void updatePatternShowLogo() {
        bmp.eraseColor(Color.TRANSPARENT);
        if (logoTicks % 2 == 0) {
            draw.drawText("Bot is active", W / 2, H / 8, textPaint);
        }
    }

    private static Paint yellow = new Paint();
    static {
        yellow.setColor(Color.YELLOW);
        yellow.setTextSize(12);
        yellow.setTextAlign(Paint.Align.CENTER);
    }
    void updatePatternShowArrows() {
        bmp.eraseColor(Color.TRANSPARENT);
        for (int x = 0; x < playBoxW; x++) {
            draw.drawLine(playLeft + x * playBoxW, playTop, playLeft + x * playBoxW, H - playBottom, yellow);
        }
        for (int y = 0; y < playBoxH; y++) {
            draw.drawLine(playLeft, playTop + y * playBoxH, W - playRight, playTop + y * playBoxH, yellow);
        }
    }
}
