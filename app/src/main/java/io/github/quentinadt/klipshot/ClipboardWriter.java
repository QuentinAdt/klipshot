package io.github.quentinadt.klipshot;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.net.Uri;

/**
 * Place une capture dans le presse-papier systeme.
 *
 * <p>Android n'autorise la <em>lecture</em> du presse-papier qu'a l'application au premier plan
 * ou au clavier par defaut, mais l'<em>ecriture</em> reste permise depuis l'arriere-plan : c'est
 * ce qui rend ce service possible sans faire clignoter une activite a chaque capture.
 */
final class ClipboardWriter {

    static final class Result {
        final boolean ok;
        final String detail;

        private Result(boolean ok, String detail) {
            this.ok = ok;
            this.detail = detail;
        }

        static Result ok(String detail) {
            return new Result(true, detail);
        }

        static Result fail(String detail) {
            return new Result(false, detail);
        }
    }

    private ClipboardWriter() {}

    static Result copy(Context ctx, Shot shot, int mode) {
        Uri uri = shot.uri;
        if (mode == Prefs.MODE_LOCAL_COPY) {
            uri = ShotProvider.publish(ctx, shot);
            if (uri == null) return Result.fail("copie locale impossible");
        }

        ClipboardManager cm = ctx.getSystemService(ClipboardManager.class);
        if (cm == null) return Result.fail("presse-papier indisponible");

        ClipDescription description =
                new ClipDescription(shot.name, new String[]{shot.mime});
        ClipData clip = new ClipData(description, new ClipData.Item(uri));

        try {
            cm.setPrimaryClip(clip);
        } catch (Throwable t) {
            return Result.fail(t.getClass().getSimpleName()
                    + (t.getMessage() == null ? "" : " : " + t.getMessage()));
        }
        return Result.ok(uri.toString());
    }
}
