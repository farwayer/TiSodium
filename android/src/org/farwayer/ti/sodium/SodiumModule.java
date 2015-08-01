package org.farwayer.ti.sodium;

import android.util.Log;
import com.jackwink.libsodium.RandomBytes;
import com.jackwink.libsodium.jni.Sodium;
import org.appcelerator.kroll.KrollModule;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.titanium.TiApplication;
import ti.modules.titanium.BufferProxy;


@Kroll.module(name = "Sodium", id = "org.farwayer.ti.sodium")
public class SodiumModule extends KrollModule {
    @Kroll.constant public static final int NONCE_SIZE = Sodium.CRYPTO_SECRETBOX_NONCEBYTES;
    private static final String TAG = "Sodium";

    private static boolean initialized = false;


    @Kroll.onAppCreate
    public static void onAppCreate(TiApplication app) {}

    @Kroll.method
    public BufferProxy newKey() {
        if (!isInitialized()) return null;

        byte[] key = new byte[Sodium.CRYPTO_SECRETBOX_KEYBYTES];
        RandomBytes.fillBuffer(key);

        return new BufferProxy(key);
    }

    static boolean isInitialized() {
        if (initialized) return true;

        if (Sodium.sodium_init() == -1) {
            logError("Sodium library init error");
            return false;
        }

        initialized = true;
        return true;
    }

    static void logError(String error) {
        Log.e(SodiumModule.TAG, error);
    }
}
