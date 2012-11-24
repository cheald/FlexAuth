package com.chrisheald.flexauth;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

public class Token {
	private static final int RSA_KEY = 257;
	private static final double DIGITS = 8.0;
//	private static String ENROLL_HOST = "m.%s.mobileservice.blizzard.com";
//	private static String ENROLL_URI = "/enrollment/enroll.htm";
//	private static String SYNC_URI = "/enrollment/time.htm";
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
		if(!TokenUtils.isValidRegion(r)) r = "US";
		this.region = r;
	}
	
	public String getPassword() throws NoSuchAlgorithmException, InvalidKeyException {
		long time = (System.currentTimeMillis() + timeOffset) / 30000L;
		byte[] src = {0,0,0,0,0,0,0,0};
		byteBuffer.clear();		
		System.arraycopy(byteBuffer.putInt((int)time).array(), 0, src, 4, 4);
		
		if(secret.length() != 40) throw new InvalidKeyException();
		
		byte[] key = hexStringToByteArray(secret.toLowerCase());
		
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
		return String.format("%08d", code);
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
			url = new URL(TokenUtils.getSyncUri("US"));
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
		
		URL url = new URL(TokenUtils.getEnrollUri(region));
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
			if(TokenUtils.isValidSerial(serial)) {
				break;
			}
		}
		if(!TokenUtils.isValidSerial(serial)) {
			serial = "";
			secret = "";
			throw new InvalidSerialException("Could not generate a valid serial.");
		}			
	}
	
    private static char ConvertRestoreCodeByteToChar(byte b)
    {
        int index = b & 0x1f;
        if (index < 0 || index >= 32)
        	return ' ';
        else if (index <= 9)
            return (char)(index + 48);
        else
        {
            index = (index + 65) - 10;
            if (index >= 73)
                index++;
            if (index >= 76)
                index++;
            if (index >= 79)
                index++;
            if (index >= 83)
                index++;
            return (char)index;
        }
    }
	private static byte ConvertRestoreCodeCharToByte(char c)
	{
		if (c >= '0' && c <= '9')
			return (byte)(c - '0');
		else if (c >= 'A' && c <= 'Z')
		{
			byte index = (byte)(c + 10 - 65);
			if (c >= 'I')
				index--;
			if (c >= 'L')
				index--;
			if (c >= 'O')
				index--;
			if (c >= 'S')
				index--;
			return index;
		}
		else
			return -1;
	}
	public String getRestoreCode() throws NoSuchAlgorithmException, UnsupportedEncodingException
	{
        // get byte array of serial
        byte[] data = new byte[34];
        byte[] serialdata = serial.toUpperCase().replace("-", "").getBytes("US-ASCII");
        byte[] secretdata = hexStringToByteArray(secret.toLowerCase());;
        System.arraycopy(serialdata, 0, data, 0, 14);
        System.arraycopy(secretdata, 0, data, 14, 20);

		// create digest of combined data
		MessageDigest md = MessageDigest.getInstance("SHA-1");
		md.update(data);
		byte[] digestdata = md.digest();

        // take last 10 chars of hash and convert each byte to our encoded string that doesn't use I,L,O,S
        StringBuilder code = new StringBuilder();
        int startpos = digestdata.length - 10;
        for (int i = 0; i < 10; i++)
        {
                code.append(ConvertRestoreCodeByteToChar(digestdata[startpos + i]));
        }

        return code.toString();
	}

	public class RestoreException extends Exception
	{
		public RestoreException(String a)
		{
			super(a);
		}
	}
	public void Restore(String serial, String restoreCode) throws NoSuchAlgorithmException, IOException, InvalidSerialException, RestoreException, InvalidKeyException, NoSuchPaddingException, NoSuchProviderException, InvalidKeySpecException, IllegalBlockSizeException, BadPaddingException
	{
		// get the serial data
        byte[] serialdata = serial.toUpperCase().replace("-", "").getBytes("US-ASCII");
        if(serialdata.length!=14)throw new RestoreException("Wrong Serial Format");

		byte[] bytes = serialdata;
		byte[] challege = new byte[32];
		boolean flag = false;
		
		region = serial.substring(0, 2);
		
		URL url = new URL(TokenUtils.getRestoreUri(region));
		HttpURLConnection conn = null;
		for(int k=0; k<3 && !flag; k++) {
			try{
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
					throw new RestoreException("Restore R1 Response code:" + responseCode);
				} else {
					InputStream is = conn.getInputStream();
					int len = Integer.parseInt(conn.getHeaderField("Content-length"));
					if(len != 32)throw new RestoreException("Restore R1 RespLen");
					is.read(challege);
					flag = true;
				}
			} finally {
				if(conn != null) conn.disconnect();
			}
		}
		if(!flag)
		{
			throw new RestoreException("Unable to get R1 key");
		}

		byte[] restoredata = new byte[10];
		String restoreCode1 = restoreCode.toUpperCase();
		for(int i = 0; i < 10; i++)
		{
			restoredata[i] = ConvertRestoreCodeCharToByte(restoreCode1.charAt(i));
		}
		
		byte[] secret1 = new byte[46];
		System.arraycopy(serialdata, 0, secret1, 0, 14);
		System.arraycopy(challege, 0, secret1, 14, 32);
		
		SecretKeySpec signingKey = new SecretKeySpec(restoredata, HMAC_SHA1_ALGORITHM);
		Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
		mac.init(signingKey);
		mac.update(secret1);
		byte[] rawhmac = mac.doFinal();

		byte[] rand = new byte[20];
		SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
		random.nextBytes(rand);
		secret1 = new byte[40];
		System.arraycopy(rawhmac, 0, secret1, 0, 20);
		System.arraycopy(rand, 0, secret1, 20, 20);

		Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		KeyFactory keyFactory = KeyFactory.getInstance("RSA", "BC");
		Key pubkey_rsa = keyFactory.generatePublic(new RSAPublicKeySpec(pubKey, BigInteger.valueOf(RSA_KEY)));
		cipher.init(Cipher.ENCRYPT_MODE, pubkey_rsa);
		byte[] rsaFinal = cipher.doFinal(secret1);
		
		bytes = new byte[142];
		System.arraycopy(serialdata, 0, bytes, 0, 14);
		System.arraycopy(rsaFinal, 0, bytes, 14, 128);

		url = new URL(TokenUtils.getRestoreValidateUri(region));
		conn = null;
		flag = false;
		for(int k=0; k<3 && !flag; k++) {
			try{
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
					throw new RestoreException("Restore R2 Response code:" + responseCode);
				} else {
					InputStream is = conn.getInputStream();
					int len = Integer.parseInt(conn.getHeaderField("Content-length"));
					if(len != 20)throw new RestoreException("Restore R2 RespLen");
					byte[] secretBytes = new byte[20];
					is.read(secretBytes);
					flag = true;
					for(int i = 0; i < 20; i++)
					{
						secretBytes[i] = (byte) (secretBytes[i] ^ rand[i]);  
					}
					this.secret = getHex(secretBytes);
					this.serial = serial; 
				}
			} finally {
				if(conn != null) conn.disconnect();
			}
		}
	}
}	