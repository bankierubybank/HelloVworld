/* **********************************************************
 * Copyright 2012-2014, 2016, 2017 VMware, Inc.  All rights reserved.
 * **********************************************************/
package com.vmware.sso.client.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

/**
 * Use this utility class to load the private key and corresponding certificate
 * chain from either a java keystore or individual files.
 * <p>
 * <b>Note: </b>This utility class is simply provided here for convenience sake.
 * Users are free to use any other mechanism of loading the private key and
 * certificate in java and use it.
 * </p>
 *
 **/
public class SecurityUtil {

	private PrivateKey privateKey;
	private X509Certificate userCert;

	/**
	 * Loads the keys from the keystore.
	 * <p>
	 * Users can generate their own pair of private key and certificate using the
	 * keytool utility shipped with the jdk. Sample usage of the keytool to
	 * generate a pair would be as follows:
	 *
	 * <pre>
	 * <b>
	 *  &gt; keytool -genkey -keyalg RSA -alias sample -keystore sampleKeystore.jks -storepass sample
	 *  What is your first and last name?
	 *    [Unknown]:  *.vmware.com
	 *  What is the name of your organizational unit?
	 *    [Unknown]:  Ecosystem Engineering
	 *  What is the name of your organization?
	 *    [Unknown]:  VMware, Inc.
	 *  What is the name of your City or Locality?
	 *    [Unknown]:  Palo Alto
	 *  What is the name of your State or Province?
	 *    [Unknown]:  California
	 *  What is the two-letter country code for this unit?
	 *    [Unknown]:  US
	 *  Is CN=*.vmware.com, OU=Ecosystem Engineering, O="VMware, Inc.", L=Palo Alto, ST=
	 *  California, C=US correct?
	 *    [no]:  yes
	 *
	 *  Enter key password for &lt;sample&gt;
	 *          (RETURN if same as keystore password):
	 * </b>
	 * </pre>
	 *
	 * <p>
	 *
	 * @param keyStorePath
	 *            path to the keystore
	 * @param keyStorePassword
	 *            keystore password
	 * @param userAlias
	 *            alias that was used at the time of key generation
	 * @return
	 */
	public static SecurityUtil loadFromKeystore(String keyStorePath,
			String keyStorePassword, String userAlias) {
		try {
			return new SecurityUtil().loadKeystore(keyStorePath,
					keyStorePassword, userAlias);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(
					Constants.EXCEPTION_READING_LOADING_THE_USER_CERTIFICATES_FROM_KEYSTORE,
					e);
		}
	}

        /**
         * Load the pre-generated private keys, and the certificate from individual files.
         *
         * @return the pre-generated key/cert pair
         */
        public static SecurityUtil loadFromDefaultFiles() {

                // A pre-generated self-signed certificate and private key pair
                // to be used in the sample by default.
                // This is to be used for ONLY development purpose.
                String absPath = new File("").getAbsolutePath();
                String privateKeyFileName = absPath.concat("/cert/sdk.key");
                String x509CertFileName = absPath.concat("/cert/sdk.crt");
                return loadFromFiles(privateKeyFileName, x509CertFileName);
        }

	/**
	 * Load the private keys, and the certificate from individual files. This
	 * method comes handy when trying to work as a solution user for e.g.
	 * vCenter server. The open source "openssl" tool can be leveraged for
	 * converting your private key into the PKCS8 format by using the following
	 * command:
	 *
	 * <pre>
	 * <b>
	 * openssl pkcs8 -topk8 -inform PEM -outform DER -in private_key_file -nocrypt &gt; pkcs8_key
	 * </b>
	 * </pre>
	 *
	 * @param privateKeyFileName
	 *            Path to the file storing the private key in PKCS8 format ONLY
	 * @param x509CertFileName
	 *            Path to the file storing the certificate in X509 format ONLY
	 * @return
	 */
	public static SecurityUtil loadFromFiles(String privateKeyFileName,
			String x509CertFileName) {
		try {
			return new SecurityUtil().loadPrivateKey(privateKeyFileName)
					.loadX509Cert(x509CertFileName);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(
					Constants.EXCEPTION_LOADING_THE_PRIVATE_KEY_CERTIFICATES_FROM_FILES,
					e);
		}
	}

	/**
	 *
	 * keytool -genkey -keyalg RSA -alias [userAlias] -keystore [keyStorePath]
	 * -storepass [keyStorePassword]
	 *
	 * @param keyStorePath
	 * @param keyStorePassword
	 * @param userAlias
	 * @return
	 * @throws NoSuchAlgorithmException
	 * @throws UnrecoverableEntryException
	 * @throws KeyStoreException
	 * @throws CertificateException
	 * @throws IOException
	 */
	private SecurityUtil loadKeystore(String keyStorePath,
			String keyStorePassword, String userAlias)
			throws NoSuchAlgorithmException, UnrecoverableEntryException,
			KeyStoreException, CertificateException, IOException {
		File file = new File(keyStorePath);
		if (file.isFile() == false) {
			System.out.println("Keystore file not found");
		}
		System.out.println("Loading KeyStore " + file + "...");
		InputStream in = new FileInputStream(file);
		KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
		char[] passphrase = keyStorePassword.toCharArray();
		ks.load(in, passphrase);
		in.close();
		if (ks.isKeyEntry(userAlias)) {
			// get the private key
			KeyStore.PrivateKeyEntry pkEntry = (KeyStore.PrivateKeyEntry) ks
					.getEntry(userAlias, new KeyStore.PasswordProtection(
							passphrase));
			privateKey = pkEntry.getPrivateKey();
			if (pkEntry.getCertificate() instanceof X509Certificate) {
				userCert = (X509Certificate) pkEntry.getCertificate();
			}
		}
		return this;
	}

	private SecurityUtil loadPrivateKey(String privateKeyFileName)
			throws IOException, NoSuchAlgorithmException,
			InvalidKeySpecException {
		// Load the private key (in PKCS#8 DER encoding).
		File keyFile = new File(privateKeyFileName);
		byte[] encodedKey = new byte[(int) keyFile.length()];
		FileInputStream keyInputStream = new FileInputStream(keyFile);
		keyInputStream.read(encodedKey);
		keyInputStream.close();
		KeyFactory rSAKeyFactory = KeyFactory.getInstance("RSA");
		privateKey = rSAKeyFactory.generatePrivate(new PKCS8EncodedKeySpec(
				encodedKey));
		return this;
	}

	private SecurityUtil loadX509Cert(String x509CertFileName)
			throws IOException, CertificateException {
		System.out.println("Loading X509 Certificate from " + x509CertFileName
				+ "...");
		FileInputStream fis = new FileInputStream(x509CertFileName);
		BufferedInputStream bis = new BufferedInputStream(fis);

		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		while (bis.available() > 0) {
			userCert = (X509Certificate) cf.generateCertificate(bis);
		}
		return this;
	}

	/**
	 * private constructor
	 */
	private SecurityUtil() {
	}

	public PrivateKey getPrivateKey() {
		return privateKey;
	}

	public X509Certificate getUserCert() {
		return userCert;
	}

}
