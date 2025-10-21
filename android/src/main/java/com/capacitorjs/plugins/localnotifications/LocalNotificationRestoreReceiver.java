package com.capacitorjs.plugins.localnotifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.UserManager;
import com.getcapacitor.CapConfig;
import java.util.List;

public class LocalNotificationRestoreReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        UserManager um = context.getSystemService(UserManager.class);
        if (um == null || !um.isUserUnlocked()) return;

        NotificationStorage storage = new NotificationStorage(context);
        List<LocalNotification> notifications = storage.getSavedNotifications();

        CapConfig config = CapConfig.loadDefault(context);
        LocalNotificationManager localNotificationManager = new LocalNotificationManager(storage, null, context, config);

        // Re-schedule all notifications. The scheduling logic is responsible for
        // calculating the next trigger time correctly. No more hacks.
        localNotificationManager.schedule(null, notifications);
    }
}
