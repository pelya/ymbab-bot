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
            if (sum < tileCountH * tileCountW * 4) {
                showLogo(bmp, draw);
            } else {
                calculateMatches(draw);
            }
        }
    }

    public static int grid[][] = new int[tileCountH][tileCountW];

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

    public static int match[][] = new int[tileCountH][tileCountW];
    public static int matchesDrawn[][] = new int[tileCountH][tileCountW];
    public static final int UP = 0x01000000, DOWN = 0x02000000, LEFT = 0x04000000, RIGHT = 0x08000000;

    private static void calculateMatches(Canvas draw) {

        for (int y = 0; y < tileCountH; y++) {
            for (int x = 0; x < tileCountW; x++) {
                matchesDrawn[y][x] = 0;
            }
        }

        // Shift lines horizontally
        for (int y = 0; y < tileCountH; y++) {
            System.arraycopy(grid[y], 0, match[y], 0, tileCountW);
        }
        for (int y = 0; y < tileCountH; y++) {
            for (int x = 1; x < tileCountW; x++) {
                System.arraycopy(grid[y], 0, match[y], x, tileCountW - x);
                System.arraycopy(grid[y], tileCountW - x, match[y], 0, x);
                // TODO: sort matches, draw only biggest match, but draw arrows for both directions
                showMatchesVertical(draw, match, x, y);
            }
            System.arraycopy(grid[y], 0, match[y], 0, tileCountW);
        }

        // Shift lines vertically
        for (int y = 0; y < tileCountH; y++) {
            System.arraycopy(grid[y], 0, match[y], 0, tileCountW);
        }
        for (int x = 0; x < tileCountW; x++) {
            for (int y = 1; y < tileCountH; y++) {
                for (int c = 0; c < tileCountH - y; c++) {
                    match[y + c][x] = grid[c][x];
                }
                for (int c = 0; c < y; c++) {
                    match[c][x] = grid[tileCountH - y + c][x];
                }
                // TODO: sort matches, draw only biggest match, but draw arrows for both directions
                showMatchesHorizontal(draw, match, x, y);
            }
            for (int y = 0; y < tileCountH; y++) {
                System.arraycopy(grid[y], 0, match[y], 0, tileCountW);
            }
        }
    }

    private static int showMatchesVertical(Canvas draw, int match[][], int matchX, int matchY) {
        int group = 0;
        int count = 0;
        int matchesSum = 0;

        for (int x = 0; x < tileCountW; x++) {
            group = 0;
            count = 0;
            for (int y = 0; y < tileCountH; y++) {
                if (group == match[y][x]) {
                    count++;
                } else {
                    if (count >= tileGroupSize) {
                        matchesSum += count;
                        drawTileArrowsHorizontal(draw, x - matchX, matchY, matchX);
                    }
                    group = match[y][x];
                    count = 1;
                }
            }
            if (count >= tileGroupSize) {
                matchesSum += count;
                drawTileArrowsHorizontal(draw, x - matchX, matchY, matchX);
            }
        }
        // Check for a group on the line we're shifting, snap to border
        group = match[matchY][0];
        count = 1;
        for (int x = 1; x < tileGroupSize; x++) {
            if (group == match[matchY][x]) {
                count++;
            }
        }
        if (count == tileGroupSize && group != match[matchY][tileCountW - 1]) {
            drawTileArrowsHorizontal(draw, tileCountW - matchX, matchY, matchX);
        }

        return matchesSum;
    }

    private static void drawTileArrowsHorizontal(Canvas draw, int x, int y, int arrowCount) {
        if (x < 0) {
            x += tileCountW;
        }
        if (x >= tileCountW) {
            x -= tileCountW;
        }
        boolean arrowsPointLeft = false;
        if (arrowCount > tileCountW / 2) {
            arrowCount = tileCountW - arrowCount;
            arrowsPointLeft = true;
            if ((matchesDrawn[y][x] & LEFT) != 0) {
                return;
            }
            matchesDrawn[y][x] |= LEFT;
        } else {
            if ((matchesDrawn[y][x] & RIGHT) != 0) {
                return;
            }
            matchesDrawn[y][x] |= RIGHT;
        }
        float arrowStep = tileBoxH / (tileCountH * 1.2f);
        float arrowShift = arrowStep * arrowCount / 2 - ((arrowCount % 2 == 1) ? 0 : arrowStep / 2);
        for (int a = 0; a < arrowCount; a++) {
            float drawY = playTop + tileBoxH * (y + 0.5f) - arrowShift + a * arrowStep;
            if (arrowsPointLeft) {
                draw.drawLine(playLeft + tileBoxW * (x + 0.1f), drawY, playLeft + tileBoxW * (x + 0.3f), drawY, yellow);
                draw.drawLine(playLeft + tileBoxW * (x + 0.1f), drawY, playLeft + tileBoxW * (x + 0.2f), drawY - tileBoxH * 0.05f, yellow);
                draw.drawLine(playLeft + tileBoxW * (x + 0.1f), drawY, playLeft + tileBoxW * (x + 0.2f), drawY + tileBoxH * 0.05f, yellow);
            } else {
                draw.drawLine(playLeft + tileBoxW * (x + 0.9f), drawY, playLeft + tileBoxW * (x + 0.7f), drawY, yellow);
                draw.drawLine(playLeft + tileBoxW * (x + 0.9f), drawY, playLeft + tileBoxW * (x + 0.8f), drawY - tileBoxH * 0.05f, yellow);
                draw.drawLine(playLeft + tileBoxW * (x + 0.9f), drawY, playLeft + tileBoxW * (x + 0.8f), drawY + tileBoxH * 0.05f, yellow);
            }
        }
    }

    private static int showMatchesHorizontal(Canvas draw, int match[][], int matchX, int matchY) {
        int group = 0;
        int count = 0;
        int matchesSum = 0;

        for (int y = 0; y < tileCountH; y++) {
            group = 0;
            count = 0;
            for (int x = 0; x < tileCountW; x++) {
                if (group == match[y][x]) {
                    count++;
                } else {
                    if (count >= tileGroupSize) {
                        matchesSum += count;
                        drawTileArrowsVertical(draw, matchX, y - matchY, matchY);
                    }
                    group = match[y][x];
                    count = 1;
                }
            }
            if (count >= tileGroupSize) {
                matchesSum += count;
                drawTileArrowsVertical(draw, matchX, y - matchY, matchY);
            }
        }
        // Check for a group on the line we're shifting, snap to border
        group = match[0][matchX];
        count = 1;
        for (int y = 1; y < tileGroupSize; y++) {
            if (group == match[y][matchX]) {
                count++;
            }
        }
        if (count == tileGroupSize && group != match[tileCountH - 1][matchX]) {
            drawTileArrowsVertical(draw, matchX, tileCountH - matchY, matchY);
        }

        return matchesSum;
    }

    private static void drawTileArrowsVertical(Canvas draw, int x, int y, int arrowCount) {
        if (y < 0) {
            y += tileCountH;
        }
        if (y >= tileCountH) {
            y -= tileCountH;
        }
        boolean arrowsPointUp = false;
        if (arrowCount > tileCountH / 2) {
            arrowCount = tileCountH - arrowCount;
            arrowsPointUp = true;
            if ((matchesDrawn[y][x] & UP) != 0) {
                return;
            }
            matchesDrawn[y][x] |= UP;
        } else {
            if ((matchesDrawn[y][x] & DOWN) != 0) {
                return;
            }
            matchesDrawn[y][x] |= DOWN;
        }
        float arrowStep = tileBoxW / (tileCountW * 1.2f);
        float arrowShift = arrowStep * arrowCount / 2 - ((arrowCount % 2 == 1) ? 0 : arrowStep / 2);
        for (int a = 0; a < arrowCount; a++) {
            float drawX = playLeft + tileBoxW * (x + 0.5f) - arrowShift + a * arrowStep;
            if (arrowsPointUp) {
                draw.drawLine(drawX, playTop + tileBoxH * (y + 0.1f), drawX, playTop + tileBoxH * (y + 0.3f), yellow);
                draw.drawLine(drawX, playTop + tileBoxH * (y + 0.1f), drawX - tileBoxW * 0.05f, playTop + tileBoxH * (y + 0.2f), yellow);
                draw.drawLine(drawX, playTop + tileBoxH * (y + 0.1f), drawX + tileBoxW * 0.05f, playTop + tileBoxH * (y + 0.2f), yellow);
            } else {
                draw.drawLine(drawX, playTop + tileBoxH * (y + 0.9f), drawX, playTop + tileBoxH * (y + 0.7f), yellow);
                draw.drawLine(drawX, playTop + tileBoxH * (y + 0.9f), drawX - tileBoxW * 0.05f, playTop + tileBoxH * (y + 0.8f), yellow);
                draw.drawLine(drawX, playTop + tileBoxH * (y + 0.9f), drawX + tileBoxW * 0.05f, playTop + tileBoxH * (y + 0.8f), yellow);
            }
        }
    }

}
