package io.github.quentinadt.klipshot;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;

/** Interroge MediaStore pour reperer la derniere capture d'ecran. */
final class ScreenshotFinder {

    private static final String[] PROJECTION = {
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.SIZE,
    };

    /**
     * Un fichier compte comme capture d'ecran s'il est range dans un dossier "Screenshots"
     * (Pictures/Screenshots sur Pixel, DCIM/Screenshots ailleurs) ou s'il en porte le nom.
     * LIKE est insensible a la casse pour l'ASCII en SQLite.
     */
    private static final String IS_SCREENSHOT =
            "(" + MediaStore.Images.Media.RELATIVE_PATH + " LIKE '%screenshots%'"
                    + " OR " + MediaStore.Images.Media.DISPLAY_NAME + " LIKE 'screenshot%')";

    /** Ecarte les fichiers encore en cours d'ecriture. */
    private static final String IS_COMPLETE =
            MediaStore.Images.Media.IS_PENDING + " = 0 AND " + MediaStore.Images.Media.SIZE + " > 0";

    private ScreenshotFinder() {}

    private static Uri collection() {
        return MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
    }

    /** La capture la plus recente, ou null. Sert au bouton "Tester". */
    static Shot latest(Context ctx) {
        return query(ctx, IS_SCREENSHOT + " AND " + IS_COMPLETE);
    }

    /** La capture la plus recente dont l'identifiant depasse {@code afterId}, ou null. */
    static Shot newerThan(Context ctx, long afterId) {
        if (afterId <= 0) return latest(ctx);
        return query(ctx, IS_SCREENSHOT + " AND " + IS_COMPLETE
                + " AND " + MediaStore.Images.Media._ID + " > " + afterId);
    }

    /** Identifiant de la capture la plus recente, ou -1. Sert de point de depart au service. */
    static long latestId(Context ctx) {
        Shot s = latest(ctx);
        return s == null ? -1L : s.id;
    }

    private static Shot query(Context ctx, String selection) {
        Bundle args = new Bundle();
        args.putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection);
        args.putString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER,
                MediaStore.Images.Media._ID + " DESC");
        args.putString(ContentResolver.QUERY_ARG_SQL_LIMIT, "1");

        try (Cursor c = ctx.getContentResolver().query(collection(), PROJECTION, args, null)) {
            if (c == null || !c.moveToFirst()) return null;
            long id = c.getLong(c.getColumnIndexOrThrow(MediaStore.Images.Media._ID));
            return new Shot(
                    id,
                    ContentUris.withAppendedId(collection(), id),
                    c.getString(c.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)),
                    c.getString(c.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)),
                    c.getLong(c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)),
                    c.getLong(c.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)));
        } catch (SecurityException | IllegalArgumentException e) {
            return null;
        }
    }
}
