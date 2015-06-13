package ymbab.bot;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.Image;
import android.util.Log;

public class PlayTiles {
    public static final String LOG = "#ymbab-bot";

    public static final boolean debug = false;

    public static class TileRegion {
        Image image;
        Canvas draw;
        int x = 0, y = 0, w = 0, h = 0;
        int debug = 0;
    }

    public static class RGB {
        short r = 0, g = 0, b = 0;
        void set(int c) {
            r = (short)((c >> 0) & 0xff);
            g = (short)((c >> 8) & 0xff);
            b = (short)((c >> 16) & 0xff);
            //b = (short)((c >> 24) & 0x7f);
        }
    }

    private static Paint debugColor = new Paint();
    private static Paint debugBlack = new Paint();
    private static Paint debugText = new Paint();
    private static Paint debugTextSmall = new Paint();
    static {
        debugText.setColor(Color.YELLOW);
        debugText.setTextSize(20);
        debugText.setTextAlign(Paint.Align.CENTER);
        debugTextSmall.setColor(Color.YELLOW);
        debugTextSmall.setTextSize(13);
        debugTextSmall.setTextAlign(Paint.Align.CENTER);
        debugBlack.setColor(Color.BLACK);
    }

    public static abstract class Tile {
        RGB r = new RGB();
        abstract int match(TileRegion t);
        void get(TileRegion t, float x, float y) {
            r.set(t.image.getPlanes()[0].getBuffer().getInt((t.y + (int)(t.h * y)) * t.image.getPlanes()[0].getRowStride() + (int)((t.x + t.w * x) * PlayField.aspectRatioCorrectionX) * 4));
            if (debug && t.debug > 0) {
                t.debug --;
                debugColor.setColor(Color.rgb(r.r, r.g, r.b));
                t.draw.drawCircle(t.x + t.w * x, t.y + t.h * y, 4, debugBlack);
                t.draw.drawCircle(t.x + t.w * x, t.y + t.h * y, 3, debugColor);
                t.draw.drawText(r.r + "," + r.g + "," + r.b, t.x + t.w / 2, t.y + t.h * (8 - t.debug) / 4 * (t.y > PlayField.H / 2 ? -1 : 1), debugTextSmall);
            }
        }
        void getAbs(TileRegion t, int x, int y) {
            r.set(t.image.getPlanes()[0].getBuffer().getInt(y * t.image.getPlanes()[0].getRowStride() + x * 4));
        }
    }

    private static final Tile[] tileMatcher = new Tile[] {
            new Staff(), new Sword(), new Key(), new Shield(), new Crate(), new Power(), new Thought()
    };

    private static TileRegion region = new TileRegion();

    public static int getTile(Image image, Canvas draw, int x, int y, int w, int h) {
        region.image = image;
        region.draw = draw;
        region.x = x;
        region.y = y;
        region.w = w;
        region.h = h;
        int ret = 0;
        for (Tile t : tileMatcher) {
            if (debug) {
                region.debug = 0;
            }
            ret = t.match(region);
            if (ret != 0)
                break;
        }
        if (debug) {
            draw.drawText(String.valueOf(ret), x + w / 2, y + h / 4, debugText);
        }
        return ret;
    }

    static class DebugTile extends Tile {
        int match(TileRegion t) {
            // Debug, will be removed later
            /*
            if (t.x - PlayField.playLeft < t.w / 2 && t.y - PlayField.playTop < t.h / 2) {
                for (int y = 0; y < t.h; y++) {
                    for (int x = 0; x < t.w; x++) {
                        getAbs(t, x, y);
                        debugColor.setARGB(255, r.r, r.g, r.b);
                        t.draw.drawPoint(x + t.w, y + t.h * 4 / 3, debugColor);
                    }
                }
            }
            if (t.x - PlayField.playLeft < t.w / 2 && t.y - PlayField.playTop < t.h / 2) {
                for (int y = 0; y < t.h; y++) {
                    for (int x = 0; x < t.w; x++) {
                        getAbs(t, t.image.getWidth() - t.w + x, t.image.getHeight() - t.h + y);
                        debugColor.setARGB(255, r.r, r.g, r.b);
                        t.draw.drawPoint(x + t.w * 3, y + t.h * 4 / 3, debugColor);
                    }
                }
            }
            */
            /*
            if (t.x - PlayField.playLeft < t.w / 2 && t.y - PlayField.playTop < t.h / 2) {
                t.debug = 1;
                for (int y = 0; y < t.h; y++) {
                    for (int x = 0; x < t.w; x++) {
                        get(t, (float) x / t.w, (float)y / t.h);
                        debugColor.setARGB(255, r.r, r.g, r.b);
                        t.draw.drawPoint(x + t.w, y + t.h * 4 / 3, debugColor);
                    }
                }
            }
            */
            /*
            if (t.x - PlayField.playLeft + 1 >= t.w * (PlayField.tileCountW - 1) && t.y - PlayField.playTop + 1 >= t.h * (PlayField.tileCountH - 1) ) {
                t.debug = 1;
                for (int y = 0; y < t.h; y++) {
                    for (int x = 0; x < t.w; x++) {
                        get(t, (float) x / t.w, (float)y / t.h);
                        debugColor.setARGB(255, r.r, r.g, r.b);
                        t.draw.drawPoint(x + t.w * 3, y + t.h * 4 / 3, debugColor);
                    }
                }
            }
            */

            return 0;
        }
    }

    static class Staff extends Tile {
        int match(TileRegion t) {

            //if (t.x - PlayField.playLeft < t.w * 4 && t.y - PlayField.playTop < t.h / 2) t.debug = 4;
            //if (t.x - PlayField.playLeft + 1 >= t.w * (PlayField.tileCountW - 3) && t.y - PlayField.playTop + 1 >= t.h * (PlayField.tileCountH - 1) ) t.debug = 4;

            get(t, 0.20f, 0.20f);
            if (r.r < 110 || r.g > 10 || r.b > 10)
                return 0;
            get(t, 0.80f, 0.80f);
            if (r.r < 110 || r.g > 10 || r.b > 10)
                return 0;
            get(t, 0.45f, 0.20f);
            if (r.r < 110 || r.g > 10 || r.b > 10)
                return 0;
            get(t, 0.55f, 0.80f);
            if (r.r < 110 || r.g > 10 || r.b > 10)
                return 0;
            // Center point is too thin to capture precisely
            //get(t, 0.50f, 0.50f);
            //if (r.r < 100 || r.r > 150 || r.g > 90  || r.b > 50)
            //    return 0;
            return 22;
        }
    }

    static class Sword extends Tile {
        int match(TileRegion t) {

            //if (t.x - PlayField.playLeft < t.w * 4 && t.y - PlayField.playTop < t.h / 2) t.debug = 3;
            //if (t.x - PlayField.playLeft + 1 >= t.w * (PlayField.tileCountW - 3) && t.y - PlayField.playTop + 1 >= t.h * (PlayField.tileCountH - 1) ) t.debug = 3;

            get(t, 0.20f, 0.20f);
            if (r.r > 40 || r.g > 80 || r.b < 150)
                return 0;
            get(t, 0.80f, 0.80f);
            if (r.r > 40 || r.g > 80 || r.b < 150)
                return 0;
            get(t, 0.70f, 0.33f);
            if (r.r < 170 || r.g < 170  || r.b < 190)
                return 0;
            return 21;
        }
    }

    static class Key extends Tile {
        int match(TileRegion t) {

            //if (t.x - PlayField.playLeft < t.w * 4 && t.y - PlayField.playTop < t.h / 2) t.debug = 3;
            //if (t.x - PlayField.playLeft + 1 >= t.w * (PlayField.tileCountW - 3) && t.y - PlayField.playTop + 1 >= t.h * (PlayField.tileCountH - 1) ) t.debug = 3;

            get(t, 0.20f, 0.80f);
            if (r.r > 40 || r.g < 100 || r.g > 150 || r.b > 10)
                return 0;
            get(t, 0.80f, 0.20f);
            if (r.r > 40 || r.g < 100 || r.g > 150 || r.b > 10)
                return 0;
            get(t, 0.75f, 0.88f);
            if (r.r < 230 || r.g < 180 || r.b > 90)
                return 0;
            return 20;
        }
    }

    static class Shield extends Tile {
        int match(TileRegion t) {

            //if (t.x - PlayField.playLeft < t.w * 4 && t.y - PlayField.playTop < t.h / 2) t.debug = 3;
            //if (t.x - PlayField.playLeft + 1 >= t.w * (PlayField.tileCountW - 3) && t.y - PlayField.playTop + 1 >= t.h * (PlayField.tileCountH - 1) ) t.debug = 3;

            get(t, 0.15f, 0.85f);
            if (r.r > 10 || r.g > 110 || r.b > 100)
                return 0;
            get(t, 0.85f, 0.85f);
            if (r.r > 10 || r.g > 110 || r.b > 100)
                return 0;
            get(t, 0.48f, 0.31f);
            if (r.r < 160 || r.g < 140 || r.b < 160)
                return 0;
            return 13;
        }
    }

    static class Crate extends Tile {
        int match(TileRegion t) {

            //if (t.x - PlayField.playLeft < t.w * 4 && t.y - PlayField.playTop < t.h / 2) t.debug = 5;
            //if (t.x - PlayField.playLeft + 1 >= t.w * (PlayField.tileCountW - 3) && t.y - PlayField.playTop + 1 >= t.h * (PlayField.tileCountH - 1) ) t.debug = 5;

            get(t, 0.30f, 0.35f);
            if (r.r > 140 || r.r < 100 || r.g > 100 || r.b > 50)
                return 0;
            get(t, 0.30f, 0.70f);
            if (r.r > 140 || r.r < 100 || r.g > 100 || r.b > 50)
                return 0;
            get(t, 0.70f, 0.35f);
            if (r.r > 140 || r.r < 100 || r.g > 100 || r.b > 50)
                return 0;
            get(t, 0.70f, 0.70f);
            if (r.r > 140 || r.r < 100 || r.g > 100 || r.b > 50)
                return 0;
            get(t, 0.50f, 0.50f);
            if (r.r > 140 || r.r < 100 || r.g > 100 || r.b > 50)
                return 0;
            return 7;
        }
    }

    static class Power extends Tile {
        int match(TileRegion t) {
            get(t, 0.15f, 0.15f);
            if (r.r > 90 || r.g > 60 || r.b > 100)
                return 0;
            get(t, 0.85f, 0.85f);
            if (r.r > 90 || r.g > 60 || r.b > 100)
                return 0;
            get(t, 0.65f, 0.35f);
            if (r.r > 90 || r.g > 60 || r.b > 100)
                return 0;
            get(t, 0.65f, 0.65f);
            if (r.r < 210 || r.g < 130 || r.b < 100)
                return 0;
            return 8;
        }
    }

    static class Thought extends Tile {
        int match(TileRegion t) {
            get(t, 0.15f, 0.85f);
            if (r.r > 70 || r.g > 80 || r.b > 90)
                return 0;
            get(t, 0.85f, 0.15f);
            if (r.r > 70 || r.g > 80 || r.b > 90)
                return 0;
            get(t, 0.50f, 0.50f);
            if (r.r < 200 || r.g < 80 || r.b < 80)
                return 0;
            return 9;
        }
    }
}
