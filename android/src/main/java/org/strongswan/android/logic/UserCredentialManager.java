package org.strongswan.android.logic;	

import android.util.Log;	
import java.io.InputStream;	
import java.io.IOException;	
import java.security.cert.Certificate;	
import java.security.cert.CertificateException;	
import java.security.KeyStore;	
import java.security.KeyStoreException;	
import java.security.NoSuchAlgorithmException;	
import java.security.PrivateKey;	
import java.security.UnrecoverableKeyException;	
import java.util.ArrayList;	
import java.util.Arrays;	
import java.util.concurrent.locks.ReentrantReadWriteLock;	

public class UserCredentialManager {	

	private static final String TAG = UserCredentialManager.class.getSimpleName();	
	private static final String PKCS12 = "PKCS12";	
	private final ReentrantReadWriteLock mLock = new ReentrantReadWriteLock();	
	private KeyStore store;	

	private UserCredentialManager() {	
		try {	
			store = KeyStore.getInstance(PKCS12);	
			store.load(null, null);	
		} catch(Exception ex) {	
			Log.e(TAG, "Unable to load keystore.", ex);	
		}	
	}	

	private static class Singleton {	
		public static final UserCredentialManager mInstance = new UserCredentialManager();	
	}	

	public static UserCredentialManager getInstance() {	
		return Singleton.mInstance;	
	}	

	public Certificate[] getUserCertificateChain(String alias) throws KeyStoreException {	
		ArrayList<Certificate> certs = new ArrayList<Certificate>();	
		try {	
			this.mLock.readLock().lock();	
			Certificate[] chain = store.getCertificateChain(alias);	
			if(chain != null)	
				certs.addAll(Arrays.asList(store.getCertificateChain(alias)));	
		} finally {	
			this.mLock.readLock().unlock();	
		}	
		return certs.toArray(new Certificate[]{});	
	}	

	public PrivateKey getUserKey(String alias, char[] password) throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException {	
		PrivateKey key;	
		try {	
			this.mLock.readLock().lock();	
			key = (PrivateKey)store.getKey(alias, password);	
		} finally {	
			this.mLock.readLock().unlock();	
		}	
		return key;	
	}	

	public void storeCredentials(byte[] base64Bytes, char[] password) throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {	
		byte[] decoded = android.util.Base64.decode(base64Bytes, 0);	
		InputStream bis = new java.io.ByteArrayInputStream(decoded);	
		try {	
			this.mLock.writeLock().lock();	
			store.load(bis, password);	
		} finally {	
			this.mLock.writeLock().unlock();	
			bis.close();	
		}	
	}	
} 