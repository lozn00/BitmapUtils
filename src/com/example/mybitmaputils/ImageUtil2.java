package com.example.mybitmaputils;
/**
 * 从内存中读入的太少了
 */
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
import java.lang.ref.SoftReference;
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

//SoftReference
public class ImageUtil2 {
	private static final String TAG = "ImageUtil";
	//
	public static final String FILE_PATH = Environment.getExternalStorageDirectory() + "/IMAGE_CACHE";
	// public Map<String, byte[]> mMapBytes = new HashMap<String, byte[]>();
	public Map<String, SoftReference<byte[]>> mMapBytes = new HashMap<String, SoftReference<byte[]>>();
	private Context context;
	private boolean mEnableFileCache=true;
	private boolean mEnableMemoryCache=true;

	public void setEnableFileCache(boolean value) {
		this.mEnableFileCache = value;
	}

	public void setEnableMemoryCache(boolean value) {
		this.mEnableMemoryCache = value;
	}

	public ImageUtil2(Context context) {
		this.context = context;
		File file = new File(FILE_PATH);
		if (!file.exists()) {
			file.mkdirs();
		}
	}

	public byte[] getBytesByUrl(String url) {
		byte[] data = null;
		try {
			if (mEnableMemoryCache) {
				data=readImageFormMemory(url);
				if (data != null) {
					Log.i(TAG, "从内存中加载");
					return data;
				}
			}

			// String formatUrl = ;// 由于本地文件特殊所以需要格式化一下
			if (mEnableFileCache) {
				data = readImageFormFile(getFormatFile(url));
				if (data != null) {
					
					Log.i(TAG, "从文件中加载" + mMapBytes.size());
					return data;
				}
			}

			Log.i(TAG, "从网络中加载");
			data = readImageFormNet(url);
			if (data == null) {
				mOnFailCallBackListener.onFailCallBack(new Throwable("加载数据失败"));
			}
			// mMapBytes.put(url, data);
			mMapBytes.put(url, new SoftReference<byte[]>(data));
			Log.i(TAG, "数据大小" + mMapBytes.size());
			saveImageToFile(getFormatFile(url), data);

		} catch (IOException e) {
			mOnFailCallBackListener.onFailCallBack(e);
		}
		return data;
	}

	private void saveImageToFile(String formatFile, byte[] bytes) throws IOException {
		FileOutputStream fileOutputStream = new FileOutputStream(formatFile);
		fileOutputStream.write(bytes, 0, bytes.length);
		closeStream(fileOutputStream);
	}

	Handler handler = new Handler();

	public void display(final String url, final ImageView iv) {

		new Thread(new MyTaskInBackground(url, iv)).start();
	}

	// 线程的方式取请求图片
	class MyTaskInBackground implements Runnable {
		private String url;
		private ImageView iv;

		public MyTaskInBackground(String url, ImageView iv) {
			this.url = url;
			this.iv = iv;
		}

		@Override
		public void run() {
			byte[] bytes = getBytesByUrl(url);
			// Bitmap bitmap = getBitmapByBytes(bytes);
			// ;
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
		/**
		 * 1）inBitmap如果设置，当加载内容时该方法将尝试重用这个位图；2）inDensity使用像素密度来表示位图；3）
		 * inDither如果存在抖动，解码器将尝试解码图像抖动；4）inPurgeable如果设置为true，则由此产生的位图将分配其像素，
		 * 以便系统需要回收内存时可以将它们清除
		 * ；5）inInputShareable与inPurgeable一起使用，如果inPurgeable为false那该设置将被忽略
		 * ，如果为true
		 * ，那么它可以决定位图是否能够共享一个指向数据源的引用，或者是进行一份拷贝；6）inJustDecodeBounds如果设置
		 * ，那返回的位图将为空，但会保存数据源图像的宽度和高度；7）inMutable如果设置，解码方法将始终返回一个可变的位图；8）
		 * inPreferQualityOverSpeed如果设置为true
		 * ，解码器将尝试重建图像以获得更高质量的解码，甚至牺牲解码速度；9）inPreferredConfig
		 * 如果为非空，解码器将尝试解码成这个内部配置；10）inSampleSize
		 * 如果设置的值大于1，解码器将等比缩放图像以节约内存；11）inScaled如果设置
		 * ，当inDensity和inTargetDensity不为0
		 * ，加载时该位图将被缩放，以匹配inTargetDensity，而不是依靠图形系统缩放每次将它绘 // 可以进一步压缩，但效果不好 //
		 * 2^8888 #FFFFFFFF 2^4444 : #FFFF // opts.inPreferredConfig
		 * =Config.ARGB_4444;
		 */
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
		opts.inPurgeable = true;
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
		// return mMapBytes.get(url);
		SoftReference<byte[]> reference = mMapBytes.get(url);
		return reference != null ? reference.get() : null;
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
		return (FILE_PATH + File.separator + getMd5(url));
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
