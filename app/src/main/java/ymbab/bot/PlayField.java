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

    public static final int TIMEOUT = 1000;

    // Borders of the playing field
    public static final float playLeft = 0.020f * W;
    public static final float playRight = 0.025f * W;
    public static final float playTop = 0.237f * H;
    public static final float playBottom = 0.044f * H;

    // Tile count
    public static final int tileCountW = 6;
    public static final int tileCountH = 8;

    // Tile size
    public static final float tileBoxW = (W - playLeft - playRight) / (float)tileCountW;
    public static final float tileBoxH = (H - playTop - playBottom) / (float)tileCountH;

    // Minimum 3 tiles will match
    public static final int tileGroupSize = 3;

    public static int grid[][] = new int[tileCountH][tileCountW];
    public static int match[][] = new int[tileCountH][tileCountW];
    //public static int matches[tileCountH * tileCountW * 2][tileCountH][tileCountW];
    //public static int matchesRank[] = new int[tileCountH * tileCountW * 2];
    //public static int matchesRankSorted[tileCountH * tileCountW * 2];
    public static float aspectRatioCorrectionX = 1.0f;

    public static boolean needCapture = false;


    private static Paint textPaint = new Paint();
    static {
        textPaint.setTextSize(30);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.YELLOW);
    }

    private static int logoTicks = 0;
    private static void showLogo(Bitmap bmp, Canvas draw) {
        needCapture = false;
        logoTicks ++;
        bmp.eraseColor(Color.TRANSPARENT);
        if (logoTicks % 2 == 0) {
            textPaint.setColor(Color.YELLOW);
        } else {
            textPaint.setColor(Color.BLUE);
        }
        draw.drawText("Begin run", W / 2, H / 8, textPaint);
    }

    private static Paint yellow = new Paint();
    static {
        yellow.setColor(Color.YELLOW);
        yellow.setTextSize(30);
        yellow.setTextAlign(Paint.Align.CENTER);
    }
    private  static void showGrid(Bitmap bmp, Canvas draw) {
        for (int x = 0; x <= tileCountW; x++) {
            draw.drawLine(playLeft + x * tileBoxW, playTop, playLeft + x * tileBoxW, H - playBottom, yellow);
        }
        for (int y = 0; y <= tileCountH; y++) {
            draw.drawLine(playLeft, playTop + y * tileBoxH, W - playRight, playTop + y * tileBoxH, yellow);
        }
    }

    private static int playTicks = 0;
    //private static byte[] captureArray = new byte[0];

    public static void updatePlayField(Bitmap bmp, Canvas draw, Image capture) {
        playTicks++;
        if (playTicks % 2 == 0) {
            // Capture current screen state
            bmp.eraseColor(Color.TRANSPARENT);
            needCapture = true;
            //showGrid(bmp, draw);
        } else {
            needCapture = false;
            //bmp.eraseColor(Color.TRANSPARENT);
            //showGrid(bmp, draw);
            if (capture == null) {
                Log.d(LOG, "Capture is null, skipped frame!");
                return;
            }
            int sum = findTiles(draw, capture);
            if (sum < tileCountH * tileCountW * 5) {
                showLogo(bmp, draw);
            } else {
                calculateMatches(draw);
            }
        }
    }

    private static int findTiles(Canvas draw, Image capture) {
        int sum = 0;
        for (int y = 0; y < tileCountH; y++) {
            for (int x = 0; x < tileCountW; x++) {
                grid[y][x] = PlayTiles.getTile(capture, draw, (int)(playLeft + x * tileBoxW), (int)(playTop + y * tileBoxH), (int)tileBoxW, (int)tileBoxH);
                sum += grid[y][x];
            }
        }
        return sum;
    }

    private static void calculateMatches(Canvas draw) {
        for (int y = 0; y < tileCountH; y++) {
            System.arraycopy(grid[y], 0, match[y], 0, tileCountW);
        }
        for (int y = 0; y < tileCountH; y++) {
            for (int x = 1; x < tileCountW; x++) {
                System.arraycopy(grid[y], 0, match[y], x, tileCountW - x);
                System.arraycopy(grid[y], tileCountW - x, match[y], 0, x);
                showMatches(draw, x, y);
            }
        }
    }

    private static void showMatches(Canvas draw, int shiftX, int shiftY) {
        int group = 0;
        int count = 0;
        for (int x = 0; x < tileCountW; x++) {
            group = 0;
            count = 0;
            for (int y = 0; y < tileCountH; y++) {
                if (group == match[y][x]) {
                    count++;
                }
                group = match[y][x];

            }
        }
    }
}
