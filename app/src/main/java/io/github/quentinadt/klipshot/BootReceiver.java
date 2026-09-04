package io.github.quentinadt.klipshot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Relance la surveillance apres un redemarrage ou une mise a jour de l'application.
 *
 * <p>BOOT_COMPLETED fait partie des rares cas ou une application en arriere-plan a le droit de
 * demarrer un service de premier plan, et le type {@code specialUse} est autorise a ce moment-la.
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Prefs.isEnabled(context)) return;
        try {
            WatcherService.start(context);
        } catch (Exception e) {
            Log.w(WatcherService.TAG, "redemarrage impossible : " + e);
        }
    }
}
