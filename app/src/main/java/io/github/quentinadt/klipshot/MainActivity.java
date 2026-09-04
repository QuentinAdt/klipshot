package io.github.quentinadt.klipshot;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Insets;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

/** Ecran de reglages : autorisations, options, verification. */
public class MainActivity extends Activity {

    private static final int REQ_MEDIA = 1;
    private static final int REQ_NOTIF = 2;

    private Switch swEnabled;
    private Switch swToast;
    private Switch swVibrate;
    private TextView tvStatus;
    private TextView tvPermMedia;
    private TextView tvPermNotif;
    private TextView tvPermBattery;
    private TextView tvClip;
    private TextView tvLog;
    private ImageView ivPreview;
    private Button btnPermMedia;
    private Button btnPermNotif;
    private Button btnPermBattery;
    private RadioGroup rgMode;

    /** Vrai pendant {@link #refresh()} : evite que la mise a jour de l'affichage
     *  ne declenche les ecouteurs comme s'il s'agissait d'un geste. */
    private boolean syncing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        applyInsets();

        swEnabled = findViewById(R.id.sw_enabled);
        swToast = findViewById(R.id.sw_toast);
        swVibrate = findViewById(R.id.sw_vibrate);
        tvStatus = findViewById(R.id.tv_status);
        tvPermMedia = findViewById(R.id.tv_perm_media);
        tvPermNotif = findViewById(R.id.tv_perm_notif);
        tvPermBattery = findViewById(R.id.tv_perm_battery);
        tvClip = findViewById(R.id.tv_clip);
        tvLog = findViewById(R.id.tv_log);
        ivPreview = findViewById(R.id.iv_preview);
        btnPermMedia = findViewById(R.id.btn_perm_media);
        btnPermNotif = findViewById(R.id.btn_perm_notif);
        btnPermBattery = findViewById(R.id.btn_perm_battery);
        rgMode = findViewById(R.id.rg_mode);

        swEnabled.setOnCheckedChangeListener((v, checked) -> {
            if (syncing) return;
            if (checked) enable();
            else disable();
        });

        swToast.setOnCheckedChangeListener((v, checked) -> {
            if (!syncing) Prefs.setToastEnabled(this, checked);
        });
        swVibrate.setOnCheckedChangeListener((v, checked) -> {
            if (!syncing) Prefs.setVibrateEnabled(this, checked);
        });

        rgMode.setOnCheckedChangeListener((group, id) -> {
            if (!syncing) {
                Prefs.setMode(this, id == R.id.rb_local
                        ? Prefs.MODE_LOCAL_COPY : Prefs.MODE_MEDIASTORE);
            }
        });

        btnPermMedia.setOnClickListener(v -> {
            if (hasMedia()) openAppSettings();
            else requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES}, REQ_MEDIA);
        });
        btnPermNotif.setOnClickListener(v -> {
            if (hasNotifications()) openAppSettings();
            else requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIF);
        });
        btnPermBattery.setOnClickListener(v -> requestBatteryExemption());

        findViewById(R.id.btn_test).setOnClickListener(v -> testCopy());
        findViewById(R.id.btn_check).setOnClickListener(v -> readClipboard());
    }

    /** Depuis Android 15 la fenetre est plein ecran : on decale le contenu sous les barres. */
    private void applyInsets() {
        View content = findViewById(android.R.id.content);
        content.setOnApplyWindowInsetsListener((v, insets) -> {
            Insets bars = insets.getInsets(WindowInsets.Type.systemBars()
                    | WindowInsets.Type.displayCutout());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        syncing = true;
        boolean media = hasMedia();
        boolean enabled = Prefs.isEnabled(this);

        swEnabled.setChecked(enabled);
        swToast.setChecked(Prefs.toastEnabled(this));
        swVibrate.setChecked(Prefs.vibrateEnabled(this));
        ((RadioButton) findViewById(Prefs.mode(this) == Prefs.MODE_LOCAL_COPY
                ? R.id.rb_local : R.id.rb_mediastore)).setChecked(true);

        if (enabled && WatcherService.running) tvStatus.setText(R.string.status_on);
        else if (enabled && !media) tvStatus.setText(R.string.status_missing_perm);
        else tvStatus.setText(R.string.status_off);

        // L'etat reel de l'acces aux images se lit mieux dans le resultat d'une requete que dans
        // la permission elle-meme : Android 14 peut n'accorder qu'un acces partiel.
        Shot latest = ScreenshotFinder.latest(this);
        if (media) {
            tvPermMedia.setText(latest == null
                    ? getString(R.string.perm_media_none)
                    : getString(R.string.perm_media_ok, latest.name));
            btnPermMedia.setText(R.string.action_settings);
        } else if (latest != null) {
            tvPermMedia.setText(R.string.perm_media_partial);
            btnPermMedia.setText(R.string.action_settings);
        } else {
            tvPermMedia.setText(R.string.perm_media_missing);
            btnPermMedia.setText(R.string.action_grant);
        }

        boolean notif = hasNotifications();
        tvPermNotif.setText(notif ? R.string.perm_notif_ok : R.string.perm_notif_missing);
        btnPermNotif.setText(notif ? R.string.action_settings : R.string.action_grant);

        boolean battery = isBatteryExempt();
        tvPermBattery.setText(battery ? R.string.perm_battery_ok : R.string.perm_battery_missing);
        // Une fois l'exemption accordee, le bouton n'a plus d'objet.
        btnPermBattery.setVisibility(battery ? View.GONE : View.VISIBLE);

        String log = Prefs.lastLog(this);
        tvLog.setText(log.isEmpty() ? getString(R.string.log_empty) : log);
        syncing = false;
    }

    private void enable() {
        if (!hasMedia()) {
            swEnabled.setChecked(false);
            requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES}, REQ_MEDIA);
            return;
        }
        Prefs.setEnabled(this, true);
        WatcherService.start(this);
        swEnabled.postDelayed(this::refresh, 400);
    }

    private void disable() {
        Prefs.setEnabled(this, false);
        WatcherService.stop(this);
        swEnabled.postDelayed(this::refresh, 400);
    }

    private void testCopy() {
        new Thread(() -> {
            Shot shot = ScreenshotFinder.latest(this);
            if (shot == null) {
                runOnUiThread(() -> tvClip.setText(R.string.no_screenshot));
                return;
            }
            ClipboardWriter.Result result = ClipboardWriter.copy(this, shot, Prefs.mode(this));
            runOnUiThread(() -> {
                tvClip.setText((result.ok ? "OK  " : "ECHEC  ") + shot.name + "\n" + result.detail);
                Toast.makeText(this,
                        result.ok ? R.string.toast_copied : R.string.toast_failed,
                        Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    /**
     * Lire le presse-papier n'est possible qu'ici, au premier plan : c'est la preuve que le
     * service a bien depose l'image.
     */
    private void readClipboard() {
        ClipboardManager cm = getSystemService(ClipboardManager.class);
        ClipData clip = cm == null ? null : cm.getPrimaryClip();
        ivPreview.setVisibility(View.GONE);

        if (clip == null || clip.getItemCount() == 0) {
            tvClip.setText(R.string.clip_empty);
            return;
        }
        ClipDescription desc = clip.getDescription();
        String mime = desc != null && desc.getMimeTypeCount() > 0 ? desc.getMimeType(0) : "?";
        Uri uri = clip.getItemAt(0).getUri();
        tvClip.setText(getString(R.string.clip_content, mime,
                uri != null ? uri.toString() : String.valueOf(clip.getItemAt(0).getText())));

        if (uri == null || !mime.startsWith("image/")) return;
        try {
            ivPreview.setImageURI(uri);
            ivPreview.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            tvClip.append("\n" + getString(R.string.clip_unreadable));
        }
    }

    private boolean hasMedia() {
        return checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES)
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasNotifications() {
        return checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean isBatteryExempt() {
        PowerManager pm = getSystemService(PowerManager.class);
        return pm != null && pm.isIgnoringBatteryOptimizations(getPackageName());
    }

    /**
     * Ouvre la liste systeme des optimisations de batterie. On evite volontairement
     * ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, dont la permission associee est refusee
     * par Google Play en dehors d'une liste restreinte de cas.
     */
    private void requestBatteryExemption() {
        try {
            startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
        } catch (ActivityNotFoundException e) {
            openAppSettings();
        }
    }

    private void openAppSettings() {
        startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", getPackageName(), null)));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean granted = grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED;

        if (requestCode == REQ_MEDIA) {
            if (granted) {
                enable();
            } else if (!shouldShowRequestPermissionRationale(
                    Manifest.permission.READ_MEDIA_IMAGES)) {
                // Refus definitif ou acces partiel : seuls les parametres peuvent debloquer.
                Toast.makeText(this, R.string.perm_media_partial, Toast.LENGTH_LONG).show();
                openAppSettings();
            }
        }
        refresh();
    }
}
