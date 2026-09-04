package io.github.quentinadt.klipshot;

import android.content.Context;
import android.content.SharedPreferences;

/** Reglages persistants de l'application. */
final class Prefs {

    /** L'URI MediaStore de la capture est place tel quel dans le presse-papier. */
    static final int MODE_MEDIASTORE = 0;
    /** La capture est recopiee dans le cache de l'app et exposee via ShotProvider. */
    static final int MODE_LOCAL_COPY = 1;

    private static final String FILE = "snapclip";
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_MODE = "mode";
    private static final String KEY_TOAST = "toast";
    private static final String KEY_VIBRATE = "vibrate";
    private static final String KEY_LAST_LOG = "last_log";

    private Prefs() {}

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    static boolean isEnabled(Context c) {
        return sp(c).getBoolean(KEY_ENABLED, false);
    }

    static void setEnabled(Context c, boolean v) {
        sp(c).edit().putBoolean(KEY_ENABLED, v).apply();
    }

    static int mode(Context c) {
        return sp(c).getInt(KEY_MODE, MODE_MEDIASTORE);
    }

    static void setMode(Context c, int v) {
        sp(c).edit().putInt(KEY_MODE, v).apply();
    }

    static boolean toastEnabled(Context c) {
        return sp(c).getBoolean(KEY_TOAST, true);
    }

    static void setToastEnabled(Context c, boolean v) {
        sp(c).edit().putBoolean(KEY_TOAST, v).apply();
    }

    static boolean vibrateEnabled(Context c) {
        return sp(c).getBoolean(KEY_VIBRATE, true);
    }

    static void setVibrateEnabled(Context c, boolean v) {
        sp(c).edit().putBoolean(KEY_VIBRATE, v).apply();
    }

    static String lastLog(Context c) {
        return sp(c).getString(KEY_LAST_LOG, "");
    }

    static void setLastLog(Context c, String v) {
        sp(c).edit().putString(KEY_LAST_LOG, v).apply();
    }
}
