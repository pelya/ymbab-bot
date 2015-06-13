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
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.*;
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


    private MediaProjection mediaProjection = null;
    private VirtualDisplay virtualDisplay = null;
    private MediaProjectionManager mediaProjectionManager = null;
    private ImageReader captureReader;
    private Image capturedImage;

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

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        MainActivity.instance.runOnUiThread(new Runnable() {
            public void run() {
                MainActivity.instance.startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), REQUEST_MEDIA_PROJECTION);
            }
        });
    }

    public void initFromActivity(final int resultCode, final Intent resultData) {
        final Handler handler = new Handler(getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                initDrawOverlay(resultCode, resultData);
            }
        });
    }

    private void initDrawOverlay(final int resultCode, final Intent resultData) {

        Log.i(LOG, "initDrawOverlay");
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);

        view = new ImageView(this);

        bmp = Bitmap.createBitmap(PlayField.W, PlayField.H, Bitmap.Config.ARGB_4444);

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

        PlayField.aspectRatioCorrectionX =  (float)PlayField.H / metrics.heightPixels * metrics.widthPixels / PlayField.W;
        captureReader = ImageReader.newInstance((int)(PlayField.W * PlayField.aspectRatioCorrectionX), PlayField.H, PixelFormat.RGBA_8888, 2);

        mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        Log.i(LOG, "Starting screen capture: width " + captureReader.getWidth() + " height " + captureReader.getHeight() + " density " + metrics.densityDpi);

        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, resultData);
        virtualDisplay = mediaProjection.createVirtualDisplay("ScreenCapture",
                captureReader.getWidth(), captureReader.getHeight(), metrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                captureReader.getSurface(), null, null);

        /*
        final Handler handler = new Handler();
        final Runnable updateScreen = new Runnable() {
            @Override
            public void run() {
                if (destroyed) {
                    return;
                }
                if (PlayField.needCapture) {
                    if (capturedImage != null) {
                        capturedImage.close();
                    }
                    capturedImage = captureReader.acquireLatestImage();
                    if (capturedImage == null) {
                        Log.d(LOG, "Capture is null, skipped frame!");
                        return;
                    }
                }
                PlayField.updatePattern(bmp, draw, capturedImage);
                view.invalidate();
            }
        };
        */
        Thread t = new Thread(new Runnable() {
            public void run() {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_FOREGROUND);
                while (!destroyed) {
                    if (PlayField.needCapture) {
                        if (capturedImage != null) {
                            capturedImage.close();
                        }
                        capturedImage = captureReader.acquireLatestImage();
                        if (capturedImage == null) {
                            Log.d(LOG, "Capture is null, skipped frame!");
                            return;
                        }
                    }
                    PlayField.updatePlayField(bmp, draw, capturedImage);
                    view.postInvalidate();
                    //handler.post(updateScreen);
                    try {
                        Thread.sleep(PlayField.needCapture ? PlayField.TIMEOUT / 2: PlayField.TIMEOUT);
                        //Thread.sleep(PlayField.TIMEOUT);
                    } catch (Exception e) {
                    }
                }
            }
        });
        t.setPriority(Thread.MAX_PRIORITY);
        t.start();

        /*
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (destroyed) {
                    return;
                }
                long delta = SystemClock.uptimeMillis();
                if (PlayField.needCapture) {
                    if (capturedImage != null) {
                        capturedImage.close();
                    }
                    capturedImage = captureReader.acquireLatestImage();
                    if (capturedImage == null) {
                        Log.d(LOG, "Capture is null, skipped frame!");
                        handler.postDelayed(this, 30);
                        return;
                    }
                }
                PlayField.updatePattern(bmp, draw, capturedImage);
                view.invalidate();
                delta = SystemClock.uptimeMillis() - delta;
                if (delta > PlayField.TIMEOUT) {
                    delta = PlayField.TIMEOUT - 10;
                }
                if (!PlayField.needCapture) {
                    delta = 0;
                }
                handler.postDelayed(this, PlayField.TIMEOUT - delta);
            }
        }, PlayField.TIMEOUT);
        */
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
        System.exit(0); // Just kill the process
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
        if (capturedImage != null) {
            capturedImage.close();
            capturedImage = null;
        }
        if (captureReader != null) {
            captureReader.close();
            captureReader = null;
        }
        Log.d(LOG, "Service stopped");
        running = false;
    }

}
