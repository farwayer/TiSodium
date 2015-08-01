package org.farwayer.ti.sodium;

import com.jackwink.libsodium.CryptoSecretBox;
import com.jackwink.libsodium.RandomBytes;
import com.jackwink.libsodium.jni.Sodium;
import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollFunction;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.titanium.TiC;
import ti.modules.titanium.BufferProxy;

import java.util.HashMap;


@Kroll.proxy(creatableInModule = SodiumModule.class)
public class CipherProxy extends KrollProxy {
    private static final String PROPERTY_KEY = "key";
    private static final String PROPERTY_NONCE = "nonce";
    private static final String PROPERTY_DATA = "data";
    private static final String PROPERTY_CIPHERTEXT = "ciphertext";

    private byte[] key;


    @Override
    public void handleCreationDict(KrollDict args) {
        key = getBuffer(args, PROPERTY_KEY, Sodium.CRYPTO_SECRETBOX_KEYBYTES);
        args.remove(PROPERTY_KEY);
        super.handleCreationDict(args);
    }

    @Kroll.method
    public void encrypt(final KrollDict args) {
        if (!SodiumModule.isInitialized()) return;

        final byte data[] = getBuffer(args, PROPERTY_DATA, -1);
        if (data == null) return;

        final KrollFunction onSuccess = getArgument(args, TiC.PROPERTY_SUCCESS, KrollFunction.class);
        if (onSuccess == null) return;

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] nonce = new byte[Sodium.CRYPTO_SECRETBOX_NONCEBYTES];
                RandomBytes.fillBuffer(nonce);

                byte[] ciphertext = CryptoSecretBox.create_easy(data, nonce, key);
                if (ciphertext == null) {
                    KrollFunction onError = getArgument(args, TiC.EVENT_PROPERTY_ERROR, KrollFunction.class);
                    if (onError != null) {
                        onError.call(getKrollObject(), (HashMap) null);
                    }
                    return;
                }

                HashMap<String, Object> map = new HashMap<String, Object>();
                map.put(PROPERTY_CIPHERTEXT, new BufferProxy(ciphertext));
                map.put(PROPERTY_NONCE, new BufferProxy(nonce));
                onSuccess.call(getKrollObject(), map);
            }
        });
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }

    @Kroll.method
    public void decrypt(final KrollDict args) {
        if (!SodiumModule.isInitialized()) return;

        final byte ciphertext[] = getBuffer(args, PROPERTY_CIPHERTEXT, -1);
        if (ciphertext == null) return;

        final byte[] nonce = getBuffer(args, PROPERTY_NONCE, Sodium.CRYPTO_SECRETBOX_NONCEBYTES);
        if (nonce == null) return;

        final KrollFunction onSuccess = getArgument(args, TiC.PROPERTY_SUCCESS, KrollFunction.class);
        if (onSuccess == null) return;

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                byte data[] = CryptoSecretBox.open_easy(ciphertext, nonce, key);
                if (data == null) {
                    KrollFunction onError = getArgument(args, TiC.EVENT_PROPERTY_ERROR, KrollFunction.class);
                    if (onError != null) {
                        onError.call(getKrollObject(), (HashMap) null);
                    }
                    return;
                }

                HashMap<String, Object> map = new HashMap<String, Object>();
                map.put(PROPERTY_DATA, new BufferProxy(data));
                onSuccess.call(getKrollObject(), map);
            }
        });
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }

    private <T> T getArgument(KrollDict args, String argument, Class<T> clazz) {
        Object value = args.get(argument);
        if (clazz.isInstance(value)) return clazz.cast(value);

        String error = String.format("'%s' must be instance of %s", argument, clazz);
        SodiumModule.logError(error);
        return null;
    }

    private byte[] getBuffer(KrollDict args, String argument, int length) {
        BufferProxy buffer = getArgument(args, argument, BufferProxy.class);
        if (buffer == null) return null;

        if (length != -1 && buffer.getLength() != length) {
            String error = String.format("Length of '%s' must be %d", argument, length);
            SodiumModule.logError(error);
            return null;
        }

        return buffer.getBuffer();
    }
}
