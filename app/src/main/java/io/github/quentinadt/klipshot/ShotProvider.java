package io.github.quentinadt.klipshot;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Expose une copie locale de la capture, pour le mode {@link Prefs#MODE_LOCAL_COPY}.
 *
 * <p>Ce fallback existe parce que certaines applications refusent de coller un URI MediaStore
 * appartenant a une autre application. Un URI servi par notre propre provider est, lui, toujours
 * accessible : le systeme accorde une permission de lecture temporaire a l'application qui lit le
 * presse-papier.
 */
public class ShotProvider extends ContentProvider {

    static final String AUTHORITY = "io.github.quentinadt.klipshot.shots";
    private static final String DIR = "clip";
    /** Nombre de captures conservees dans le cache. */
    private static final int KEEP = 3;

    /** Recopie la capture dans le cache et renvoie l'URI a poser dans le presse-papier. */
    static Uri publish(Context ctx, Shot shot) {
        File dir = new File(ctx.getCacheDir(), DIR);
        if (!dir.isDirectory() && !dir.mkdirs()) return null;

        String ext = "jpeg".equals(shot.mime) || shot.mime.endsWith("jpeg") ? ".jpg" : ".png";
        File out = new File(dir, "shot_" + shot.id + ext);

        if (!out.isFile() || out.length() != shot.size) {
            try (InputStream in = ctx.getContentResolver().openInputStream(shot.uri);
                 OutputStream os = new FileOutputStream(out)) {
                if (in == null) return null;
                byte[] buf = new byte[64 * 1024];
                int n;
                while ((n = in.read(buf)) > 0) os.write(buf, 0, n);
            } catch (IOException | SecurityException e) {
                out.delete();
                return null;
            }
        }

        prune(dir);
        return new Uri.Builder()
                .scheme("content")
                .authority(AUTHORITY)
                .appendPath(out.getName())
                .build();
    }

    /** Ne conserve que les {@link #KEEP} fichiers les plus recents. */
    private static void prune(File dir) {
        File[] files = dir.listFiles();
        if (files == null || files.length <= KEEP) return;
        Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
        for (int i = KEEP; i < files.length; i++) files[i].delete();
    }

    private File resolve(Uri uri) throws FileNotFoundException {
        String name = uri.getLastPathSegment();
        if (name == null) throw new FileNotFoundException(String.valueOf(uri));
        Context ctx = getContext();
        if (ctx == null) throw new FileNotFoundException(String.valueOf(uri));
        File dir = new File(ctx.getCacheDir(), DIR);
        File f = new File(dir, name);
        try {
            // Empeche un URI du type ../../databases/x de sortir du dossier de cache.
            if (!f.getCanonicalPath().startsWith(dir.getCanonicalPath() + File.separator)) {
                throw new FileNotFoundException(String.valueOf(uri));
            }
        } catch (IOException e) {
            throw new FileNotFoundException(String.valueOf(uri));
        }
        if (!f.isFile()) throw new FileNotFoundException(String.valueOf(uri));
        return f;
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        return ParcelFileDescriptor.open(resolve(uri), ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override
    public String getType(Uri uri) {
        String name = uri.getLastPathSegment();
        return (name != null && name.endsWith(".jpg")) ? "image/jpeg" : "image/png";
    }

    /** Les applications qui collent demandent souvent le nom et la taille du fichier. */
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        File f;
        try {
            f = resolve(uri);
        } catch (FileNotFoundException e) {
            return null;
        }
        String[] cols = projection != null ? projection
                : new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE};
        MatrixCursor cursor = new MatrixCursor(cols, 1);
        Object[] row = new Object[cols.length];
        for (int i = 0; i < cols.length; i++) {
            if (OpenableColumns.DISPLAY_NAME.equals(cols[i])) row[i] = f.getName();
            else if (OpenableColumns.SIZE.equals(cols[i])) row[i] = f.length();
        }
        cursor.addRow(row);
        return cursor;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }
}
