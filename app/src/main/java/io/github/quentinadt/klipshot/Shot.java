package io.github.quentinadt.klipshot;

import android.net.Uri;

/** Une capture d'ecran reperee dans MediaStore. */
final class Shot {
    final long id;
    final Uri uri;
    final String name;
    final String mime;
    /** Secondes depuis l'epoch, tel que stocke par MediaStore. */
    final long dateAdded;
    final long size;

    Shot(long id, Uri uri, String name, String mime, long dateAdded, long size) {
        this.id = id;
        this.uri = uri;
        this.name = name;
        this.mime = (mime == null || mime.isEmpty()) ? "image/png" : mime;
        this.dateAdded = dateAdded;
        this.size = size;
    }

    long ageSeconds() {
        return System.currentTimeMillis() / 1000L - dateAdded;
    }
}
