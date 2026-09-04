package io.github.quentinadt.klipshot;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.database.ContentObserver;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Service de premier plan qui surveille MediaStore et copie chaque nouvelle capture d'ecran
 * dans le presse-papier. Le geste de capture reste celui du systeme.
 */
public class WatcherService extends Service {

    static final String TAG = "Klipshot";
    static final String ACTION_STOP = "io.github.quentinadt.klipshot.STOP";

    private static final String CHANNEL_ID = "watcher";
    private static final int NOTIF_ID = 1;

    /** MediaStore emet plusieurs notifications pour une meme capture : on les regroupe. */
    private static final long DEBOUNCE_MS = 250L;
    /** Deuxieme passage, au cas ou la finalisation du fichier n'emette pas d'evenement. */
    private static final long RETRY_MS = 1500L;
    /** Au-dela, il s'agit d'une ancienne image reindexee, pas d'une capture qui vient d'etre prise. */
    private static final long MAX_AGE_SECONDS = 120L;

    /** Lu par l'ecran de reglages pour afficher l'etat reel du service. */
    static volatile boolean running = false;

    private HandlerThread thread;
    private Handler bg;
    private Handler ui;
    private ContentObserver observer;
    private long lastSeenId = -1L;
    private String lastCopied = null;

    private final Runnable scan = this::scanNow;
    private final Runnable retry = this::scanNow;

    @Override
    public void onCreate() {
        super.onCreate();
        ui = new Handler(Looper.getMainLooper());
        thread = new HandlerThread("klipshot-watch");
        thread.start();
        bg = new Handler(thread.getLooper());
        createChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            Prefs.setEnabled(this, false);
            stopSelf();
            return START_NOT_STICKY;
        }

        startForeground(NOTIF_ID, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);

        if (observer == null) {
            observer = new ContentObserver(bg) {
                @Override
                public void onChange(boolean selfChange, Uri uri) {
                    bg.removeCallbacks(scan);
                    bg.removeCallbacks(retry);
                    bg.postDelayed(scan, DEBOUNCE_MS);
                    bg.postDelayed(retry, RETRY_MS);
                }
            };
            // On part de la capture la plus recente pour ne pas recopier une ancienne au demarrage.
            bg.post(() -> lastSeenId = ScreenshotFinder.latestId(this));
            getContentResolver().registerContentObserver(
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL),
                    /* notifyForDescendants= */ true,
                    observer);
            running = true;
            Log.i(TAG, "surveillance demarree");
        }
        return START_STICKY;
    }

    private void scanNow() {
        Shot shot = ScreenshotFinder.newerThan(this, lastSeenId);
        if (shot == null) return;

        lastSeenId = shot.id;
        if (shot.ageSeconds() > MAX_AGE_SECONDS) {
            Log.i(TAG, "ignoree, trop ancienne : " + shot.name);
            return;
        }

        ClipboardWriter.Result result = ClipboardWriter.copy(this, shot, Prefs.mode(this));
        String stamp = new SimpleDateFormat("HH:mm:ss", Locale.FRANCE).format(new Date());
        Prefs.setLastLog(this, stamp + (result.ok ? "  copiee : " : "  echec : ")
                + shot.name + "\n" + result.detail);
        Log.i(TAG, (result.ok ? "copiee " : "echec ") + shot.name + " " + result.detail);

        if (result.ok) {
            lastCopied = shot.name;
            updateNotification();
            if (Prefs.vibrateEnabled(this)) vibrate();
        }
        if (Prefs.toastEnabled(this)) {
            String message = result.ok
                    ? getString(R.string.toast_copied)
                    : getString(R.string.toast_failed);
            ui.post(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
        }
    }

    private void vibrate() {
        VibratorManager vm = getSystemService(VibratorManager.class);
        if (vm == null) return;
        Vibrator v = vm.getDefaultVibrator();
        if (v != null && v.hasVibrator()) {
            v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK));
        }
    }

    private void createChannel() {
        // IMPORTANCE_MIN : pas d'icone dans la barre d'etat, pas de son, juste une ligne
        // discrete dans le volet de notifications.
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, getString(R.string.channel_name), NotificationManager.IMPORTANCE_MIN);
        channel.setDescription(getString(R.string.channel_desc));
        channel.setShowBadge(false);
        getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }

    private Notification buildNotification() {
        PendingIntent open = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        PendingIntent stop = PendingIntent.getService(this, 1,
                new Intent(this, WatcherService.class).setAction(ACTION_STOP),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        String text = lastCopied == null
                ? getString(R.string.notif_idle)
                : getString(R.string.notif_last, lastCopied);

        return new Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notif)
                .setContentTitle(getString(R.string.notif_title))
                .setContentText(text)
                .setContentIntent(open)
                .addAction(new Notification.Action.Builder(
                        Icon.createWithResource(this, R.drawable.ic_notif),
                        getString(R.string.notif_stop), stop).build())
                .setOngoing(true)
                .setShowWhen(false)
                .build();
    }

    private void updateNotification() {
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) nm.notify(NOTIF_ID, buildNotification());
    }

    @Override
    public void onDestroy() {
        running = false;
        if (observer != null) {
            getContentResolver().unregisterContentObserver(observer);
            observer = null;
        }
        if (bg != null) {
            bg.removeCallbacks(scan);
            bg.removeCallbacks(retry);
        }
        if (thread != null) thread.quitSafely();
        Log.i(TAG, "surveillance arretee");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /** Demarre le service ; l'appelant doit etre au premier plan sur Android 12 et plus. */
    static void start(Context ctx) {
        ctx.startForegroundService(new Intent(ctx, WatcherService.class));
    }

    static void stop(Context ctx) {
        ctx.stopService(new Intent(ctx, WatcherService.class));
    }
}
