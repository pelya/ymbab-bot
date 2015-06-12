package ymbab.bot;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.Image;
import android.util.Log;

public class PlayField {
    public static final String LOG = "#ymbab-bot";

    // Bigger W/H will result in less pixelated oveerlay graphics, but will eat more CPU
    public static final int W = 480;
    public static final int H = 800;

    public static final int TIMEOUT = 700;

    // Borders of the playing field
    public static float playLeft = 0.050f * W;
    public static float playRight = 0.050f * W;
    public static float playTop = 0.240f * H;
    public static float playBottom = 0.042f * H;

    // Tile count
    public static int tileCountW = 6;
    public static int tileCountH = 8;

    // Tile size
    public static float tileBoxW = (W - playLeft - playRight) / (float)tileCountW;
    public static float tileBoxH = (H - playTop - playBottom) / (float)tileCountH;

    public static int grid[][] = new int[tileCountH][tileCountW];
    public static int match[][] = new int[tileCountH][tileCountW];
    //public static int matches[tileCountH * tileCountW * 2][tileCountH][tileCountW];
    public static int matchesRank[] = new int[tileCountH * tileCountW * 2];
    //public static int matchesRankSorted[tileCountH * tileCountW * 2];

    private static int logoTicks = 0;

    public static boolean needCapture = false;

    public static void updatePattern(Bitmap bmp, Canvas draw, Image capture) {
        if (logoTicks > 0) {
            logoTicks--;
            showLogo(bmp, draw);
        } else {
            showPlayField(bmp, draw, capture);
        }
    }

    private static Paint textPaint = new Paint();
    static {
        textPaint.setTextSize(30);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.YELLOW);
    }
    public static void showLogo(Bitmap bmp, Canvas draw) {
        needCapture = false;
        bmp.eraseColor(Color.TRANSPARENT);
        if (logoTicks % 2 == 0) {
            textPaint.setColor(Color.YELLOW);
        } else {
            textPaint.setColor(Color.BLUE);
        }
        draw.drawText("Bot is active", W / 2, H / 8, textPaint);
    }

    private static Paint yellow = new Paint();
    static {
        yellow.setColor(Color.YELLOW);
        yellow.setTextSize(30);
        yellow.setTextAlign(Paint.Align.CENTER);
    }
    public static void showGrid(Bitmap bmp, Canvas draw) {
        for (int x = 0; x <= tileCountW; x++) {
            draw.drawLine(playLeft + x * tileBoxW, playTop, playLeft + x * tileBoxW, H - playBottom, yellow);
        }
        for (int y = 0; y <= tileCountH; y++) {
            draw.drawLine(playLeft, playTop + y * tileBoxH, W - playRight, playTop + y * tileBoxH, yellow);
        }
    }

    private static int playTicks = 0;
    //private static byte[] captureArray = new byte[0];

    public static void showPlayField(Bitmap bmp, Canvas draw, Image capture) {
        playTicks++;
        if (playTicks % 2 == 0) {
            // Capture current screen state
            bmp.eraseColor(Color.TRANSPARENT);
            needCapture = true;
        } else {
            needCapture = false;
            //showGrid(bmp, draw);
            if (capture == null) {
                Log.d(LOG, "Capture is null, skipped frame!");
                return;
            }
            //Log.d(LOG, "Capture pixelformat: " + capture.getFormat() + " planes amount " + capture.getPlanes().length + " buffer size " + capture.getPlanes()[0].getBuffer().remaining());
            /*
            Bitmap dbg = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888);
            for (int y = 0; y < 200; y++) {
                for (int x = 0; x < 200; x++) {
                    dbg.setPixel(x, y, capture.getPlanes()[0].getBuffer().getInt(
                            (capture.getHeight() - 200 + y) * capture.getPlanes()[0].getRowStride() + (capture.getWidth() - 200 + x) * 4));
                }
            }
            draw.drawBitmap(dbg, 100, 300, null);
            */
            for (int y = 0; y < tileCountH; y++) {
                for (int x = 0; x < tileCountW; x++) {
                    grid[y][x] = PlayTiles.getTile(capture, draw, (int)(playLeft + x * tileBoxW), (int)(playTop + y * tileBoxH), (int)tileBoxW, (int)tileBoxH);
                }
            }
        }
    }

}
