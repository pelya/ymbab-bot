package ymbab.bot;

import android.content.Context;
import android.util.Log;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class Cfg {

    public static final String LOG = "Pixel Filter";

    public static boolean Initialized = false;

    public static int Pattern = 3;
    public static int ShiftTimeoutIdx = 4;
    public static boolean UseLightSensor = false;
    public static float LightSensorValue = 1000.0f;
    public static boolean FirstStart = true;

    public static String SettingsFileName = "settings.cfg";

    public static void Init(Context ctx) {
        if (Initialized) {
            return;
        }
        try {
            ObjectInputStream in = new ObjectInputStream(ctx.openFileInput(SettingsFileName));
            Pattern = in.readInt();
            ShiftTimeoutIdx = in.readInt();
            UseLightSensor = in.readBoolean();
            LightSensorValue = in.readFloat();
            if (Pattern < 0 || Pattern >= Grids.Patterns.length) {
                Pattern = 3;
            }
            if (ShiftTimeoutIdx < 0 || ShiftTimeoutIdx > Grids.ShiftTimeouts.length) {
                ShiftTimeoutIdx = 4;
            }
            Log.d(LOG, "cfg: pattern " + Pattern + " ShiftTimeoutIdx " + ShiftTimeoutIdx);
            for (int i = Grids.PatternIdCustom; i < Grids.Patterns.length; i++) {
                in.readFully(Grids.Patterns[i]);
            }
            in.close();
            FirstStart = false;
        } catch (Exception e) {
            Log.d(LOG, "Cannot load config file: " + e);
        }
        Initialized = true;
    }

    public static void Save(Context ctx) {
        if (!Initialized) {
            return;
        }
        try {
            ObjectOutputStream out = new ObjectOutputStream(ctx.openFileOutput(SettingsFileName, Context.MODE_PRIVATE));
            out.writeInt(Pattern);
            out.writeInt(ShiftTimeoutIdx);
            out.writeBoolean(UseLightSensor);
            out.writeFloat(LightSensorValue);
            //Log.d(LOG, "cfg: writing pattern " + Pattern + " ShiftTimeoutIdx " + ShiftTimeoutIdx);
            for (int i = Grids.PatternIdCustom; i < Grids.Patterns.length; i++) {
                out.write(Grids.Patterns[i]);
            }
            out.close();
        } catch (Exception e) {
            Log.e(LOG, "Cannot save config file: " + e);
        }
    }
}
