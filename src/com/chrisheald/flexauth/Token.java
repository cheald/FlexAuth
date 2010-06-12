package com.chrisheald.flexauth;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class Token {
	private static final int RSA_KEY = 257;
	private static final double DIGITS = 8.0;
	private static String ENROLL_HOST = "m.%s.mobileservice.blizzard.com";
	private static String ENROLL_URI = "/enrollment/enroll.htm";
	private static String SYNC_URI = "/enrollment/time.htm";
	private static BigInteger pubKey = new BigInteger("104890018807986556874007710914205443157030159668034197186125678960287470894290830530618284943118405110896322835449099433232093151168250152146023319326491587651685252774820340995950744075665455681760652136576493028733914892166700899109836291180881063097461175643998356321993663868233366705340758102567742483097");
	
	public String secret;
	public String serial;
	public String name;
	private String region = "US";
	public int _id = -1;
	public static long timeOffset = 0;	
	private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";
	private ByteBuffer byteBuffer = ByteBuffer.allocate(4);

	Token() {
	}
	
	Token(String name, String secret, String serial) {
		this.name = name;
		this.secret = secret;
		this.serial = serial;
	}
	
	public void setRegion(String r) {
		r = r.toUpperCase();
		if(r.compareTo("US") != 0 && r.compareTo("EU") != 0) r = "US";
		this.region = r;
	}
	
	public String getPassword() throws NoSuchAlgorithmException, InvalidKeyException {
		long time = (System.currentTimeMillis() + timeOffset) / 30000L;
		byte[] src = new byte[8];
		src[0] = 0x0;
		src[1] = 0x0;
		src[2] = 0x0;
		src[3] = 0x0;
		byteBuffer.clear();		
		System.arraycopy(byteBuffer.putInt((int)time).array(), 0, src, 4, 4);
		
		byte[] key = hexStringToByteArray(secret);
		
		SecretKeySpec signingKey = new SecretKeySpec(key, HMAC_SHA1_ALGORITHM);
		Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
		mac.init(signingKey);
		byte[] rawhmac = mac.doFinal(src);
		byte[] authCode = new byte[4];
		System.arraycopy(rawhmac, rawhmac[19] & 0x0F, authCode, 0, 4);
		byteBuffer.clear();
		byteBuffer.put(authCode);
		byteBuffer.rewind();
		int code = byteBuffer.getInt() & 0x7FFFFFFF;
		double modulo = Math.pow(10d, DIGITS);
		code = (int)((double)code % modulo);
		return Integer.toString(code);
	}
	
	public byte[] hexStringToByteArray(String s) {
	    int len = s.length();
	    byte[] data = new byte[len / 2];
	    for (int i = 0; i < len; i += 2) {
	        data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
	                             + Character.digit(s.charAt(i+1), 16));
	    }
	    return data;
	}
		
	static final String HEXES = "0123456789abcdef";
	public String getHex(byte[] raw) {
		if (raw == null) {
			return null;
		}
		final StringBuilder hex = new StringBuilder(2 * raw.length);
		for (final byte b : raw) {
			hex.append(HEXES.charAt((b & 0xF0) >> 4)).append(HEXES.charAt((b & 0x0F)));
		}
		return hex.toString();
	}
	
	public static long fetchTimeOffset() throws IOException {
		long systemTime = System.currentTimeMillis();
		URL url;
		try {
			url = new URL(String.format("http://%s%s", String.format(ENROLL_HOST, "US"), SYNC_URI));
		} catch (MalformedURLException e) {
			return 0L;			
		}
		HttpURLConnection conn = null;
		try {
			conn = (HttpURLConnection)url.openConnection();
			conn.setAllowUserInteraction(false);
			conn.setDoInput(true);
			conn.setDoOutput(true);
			conn.setUseCaches(false);
			conn.setRequestMethod("GET");
			
			int responseCode = conn.getResponseCode();
			if(responseCode != 200) {
				// throw new Exception("Response code:" + responseCode);
			} else {
				InputStream is = conn.getInputStream();
				int len = Integer.parseInt(conn.getHeaderField("Content-length"));
				byte[] buf = new byte[len];
				is.read(buf);
				BigInteger serverTime = new BigInteger(buf);
				timeOffset = (systemTime - serverTime.longValue());
				return timeOffset;
			}
		} finally {
			if(conn != null) conn.disconnect();
		}
		return 0;
	}
	
	public void generate() throws IOException, NoSuchAlgorithmException, InvalidSerialException {
		SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
		byte otp[] = new byte[37];
		byte[] msg = new byte[56];
		msg[0] = (byte)1;
		random.nextBytes(otp);
		System.arraycopy(otp, 0, msg, 1, otp.length);
		System.arraycopy(region.getBytes(), 0, msg, 38, 2);
		System.arraycopy("Motorola RAZR v3".getBytes(), 0, msg, 40, 16);
		
		BigInteger n = new BigInteger(msg);
		n = n.modPow(BigInteger.valueOf(RSA_KEY), pubKey);
		
		byte[] bytes = n.toByteArray();
		
		URL url = new URL(String.format("http://%s%s", String.format(ENROLL_HOST, region.toLowerCase()), ENROLL_URI));
		HttpURLConnection conn = null;
		for(int k=0; k<3; k++) {
			try {
				conn = (HttpURLConnection)url.openConnection();
				conn.setAllowUserInteraction(false);
				conn.setDoInput(true);
				conn.setDoOutput(true);
				conn.setUseCaches(false);
				conn.setRequestMethod("POST");
				conn.setRequestProperty("Content-length", Integer.toString(bytes.length));
				conn.setRequestProperty("Content-type", "application/octet-stream");
				OutputStream os = conn.getOutputStream();
				os.write(bytes);
				os.flush();
				
				int responseCode = conn.getResponseCode();
				if(responseCode != 200) {
					// throw new Exception("Response code:" + responseCode);
				} else {
					InputStream is = conn.getInputStream();
					int len = Integer.parseInt(conn.getHeaderField("Content-length"));
					byte[] buf = new byte[len];
					is.read(buf);
					for(int i=0; i<37; i++) buf[i+8] ^= otp[i];
					
					byte[] serialBytes = new byte[17];
					System.arraycopy(buf, 28, serialBytes, 0, 17);
					serial = new String(serialBytes);
					
					byte[] secretBytes = new byte[20];
					System.arraycopy(buf, 8, secretBytes, 0, 20);						
					secret = getHex(secretBytes);
				}
			} finally {
				if(conn != null) conn.disconnect();
			}
			if(serial.startsWith("US-") || serial.startsWith("EU-")) {
				break;
			}
		}
		if(!(serial.startsWith("US-") || serial.startsWith("EU-"))) {
			serial = "";
			secret = "";
			throw new InvalidSerialException("Could not generate a valid serial.");
		}			
	}
}