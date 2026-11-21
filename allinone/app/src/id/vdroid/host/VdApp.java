package id.vdroid.host;

import android.app.Application;

public class VdApp extends Application {

    public static boolean hookInstalled;

    @Override
    public void onCreate() {
        super.onCreate();
        hookInstalled = InstrumentationHook.install(this);
    }
}
