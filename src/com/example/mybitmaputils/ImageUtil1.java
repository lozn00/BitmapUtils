package com.example.mybitmaputils;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.spec.IvParameterSpec;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.os.Environment;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ImageView;

public class ImageUtil1 {
	private static final String TAG = "ImageUtil";
	public static final String FILE_PATH = Environment.getExternalStorageDirectory() + "/IMAGE_CACHE";
	public Map<String, byte[]> mMapBytes = new HashMap<String, byte[]>();
	private Context context;

	public ImageUtil1(Context context) {
		this.context = context;
		File file = new File(FILE_PATH);
		if (!file.exists()) {
			file.mkdirs();
		}
	}

	public byte[] getBytesByUrl(String url) {
		Log.i(TAG, "getBytesByUrl");
		byte[] data = null;
		try {
			data = readImageFormMemory(url);
			if (data != null) {
				Log.i(TAG, "从内存中加载");
				return data;
			}
			// String formatUrl = ;// 由于本地文件特殊所以需要格式化一下
			data = readImageFormFile(getFormatFile(url));
			if (data != null) {
				
				Log.i(TAG, "从文件中加载");
				return data;
			}
			
			Log.i(TAG, "从网络中加载");
			data = readImageFormNet(url);
			if (data == null) {
				mOnFailCallBackListener.onFailCallBack(new Throwable("加载数据失败"));
			}
			mMapBytes.put(url, data);
			Log.i(TAG, "数据大小"+mMapBytes.size());
			saveImageToFile(getFormatFile(url),data);
			
		} catch (IOException e) {
			mOnFailCallBackListener.onFailCallBack(e);
		}
		return data;
	}

	private void saveImageToFile(String formatFile,byte[] bytes) throws IOException {
		FileOutputStream fileOutputStream=new FileOutputStream(formatFile);
		fileOutputStream.write(bytes, 0, bytes.length);
		closeStream(fileOutputStream);
	}

	Handler handler = new Handler();

	public void display(final String url, final ImageView iv) {

		new Thread(new MyTaskInBackground(url,iv)).start();
	}

	// 线程的方式取请求图片
	class MyTaskInBackground implements Runnable {
		private String url;
		private ImageView iv;
		public MyTaskInBackground(String url, ImageView iv) {
			this.url=url;
			this.iv=iv;
		}
		@Override
		public void run() {
			byte[] bytes = getBytesByUrl(url);
//			Bitmap bitmap = getBitmapByBytes(bytes);
//			;
			handler.post(new SetImageViewRunnable(BitmapFactory.decodeByteArray(bytes, 0, bytes.length), iv));

		}
	};

	/**
	 * 这是post礼貌的runnable
	 * 
	 * @author luozheng
	 *
	 */
	class SetImageViewRunnable implements Runnable {
		private ImageView imageView;
		private Bitmap bitmap;

		public SetImageViewRunnable(Bitmap bitmap, ImageView imageView) {
			this.bitmap = bitmap;
			this.imageView = imageView;
		}

		@Override
		public void run() {
			this.imageView.setImageBitmap(bitmap);
		}

	}

	/**
	 * bytes获得bitmap
	 * 
	 * @param bytes
	 * @return
	 */
	protected Bitmap getBitmapByBytes(byte[] data) {
		Log.i(TAG, "getBitmapByBytes");
		Options opts = new Options();
		opts.inJustDecodeBounds = true;// 只是测量高宽
		BitmapFactory.decodeByteArray(data, 0, data.length, opts);
		WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		DisplayMetrics outMetrics = new DisplayMetrics();
		manager.getDefaultDisplay().getMetrics(outMetrics);
		// int screenWidth=outMetrics.widthPixels;
		// int screenHeight=outMetrics.heightPixels;
		int scaleWidth = opts.outWidth / outMetrics.widthPixels;
		int scaleHeight = opts.outHeight / outMetrics.heightPixels;
		int max = Math.max(scaleWidth, scaleHeight);// 谁大取谁
		// opts.inInputShareable
		opts.inSampleSize = max;
		opts.inInputShareable = true;
		opts.inPurgeable=true;
		opts.inJustDecodeBounds = false;
		// 缩放之后然后
		return BitmapFactory.decodeByteArray(data, 0, data.length);
	}


	private byte[] readImageFormFile(String url) throws IOException {
		File file = new File(url);
		if (!file.exists()) {
			return null;
		}
		byte[] results = null;
		BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(file));
		ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
		copyStream(bufferedInputStream, arrayOutputStream);
		results = arrayOutputStream.toByteArray();
		return results;
		// byte[] buffer=new byte[1024*8];
		// int len;
		// while((len=bufferedInputStream.read(buffer))!=-1)
		// {
		//
		// }
	}

	private byte[] readImageFormMemory(String url) {
		return mMapBytes.get(url);
		// return null;
	}

	private byte[] readImageFormNet(String urlStr) throws IOException {
		URL url = new URL(urlStr);
		InputStream inputStream = url.openStream();
		ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
		copyStream(inputStream, arrayOutputStream);
		closeStream(inputStream, arrayOutputStream);
		byte[] byteArray = arrayOutputStream.toByteArray();
		closeStream(inputStream, arrayOutputStream);// 关流
		return byteArray;
	}

	/**
	 * 关闭流错误被捕获 可关闭多个流
	 * 
	 * @param closeable
	 */
	public void closeStream(Closeable... closeable) {
		try {
			for (int i = 0; i < closeable.length; i++) {
				closeable[i].close();
			}
		} catch (IOException e) {

			e.printStackTrace();
		}
	}

	/**
	 * 从输入流复制到输出流
	 * 
	 * @param inputStream
	 * @param outputStream
	 * @throws IOException
	 */
	public void copyStream(InputStream inputStream, OutputStream outputStream) throws IOException {
		byte[] buffer = new byte[1024 * 8];
		int len;
		while ((len = inputStream.read(buffer)) != -1) {
			outputStream.write(buffer, 0, len);
		}
	}

	/**
	 * 根据网络地址获取MD5
	 * 
	 * @param url
	 * @return
	 */
	private String getFormatFile(String url) {
		return (FILE_PATH+File.separator+getMd5(url));
	}

	/**
	 * 获取MD5
	 * 
	 * @param password
	 * @return
	 */
	public static String getMd5(String password) {
		try {
			MessageDigest digest = MessageDigest.getInstance("MD5");
			byte[] result = digest.digest(password.getBytes());
			StringBuffer sb = new StringBuffer();
			for (byte b : result) {
				int number = (int) (b & 0xff);
				String str = Integer.toHexString(number);
				if (str.length() == 1) {
					sb.append("0");
				}
				sb.append(str);
			}
			return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			// can't reach
			return "";
		}
	}

	/**
	 * 设置错误的监听
	 * 
	 * @param onFailCallBackListener
	 */
	public void setOnFailCallBackListener(OnFailCallBackListener onFailCallBackListener) {
		this.mOnFailCallBackListener = onFailCallBackListener;
	}

	public static interface OnFailCallBackListener {
		public void onFailCallBack(Throwable throwable);
	}

	public OnFailCallBackListener mOnFailCallBackListener = new OnFailCallBackListener() {

		@Override
		public void onFailCallBack(Throwable throwable) {
			Log.i(TAG, throwable.toString());
		}
	};
}
