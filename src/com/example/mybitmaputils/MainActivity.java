package com.example.mybitmaputils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;

import com.example.mybitmaputils.ImageUtil.OnFailCallBackListener;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;

public class MainActivity extends Activity {

	protected static final String TAG = "MainActivity";
	private ListView listView;
	private List<String> list;
	private ImageUtil imageUtil;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		list=new ArrayList<String>();
		String HOST="http://192.168.1.2/zhbj/photos/images/test/";
		for (int i = 1; i <=40; i++) {
			list.add(HOST+i+".jpg");
		}
		imageUtil = new ImageUtil(this);
		imageUtil.setEnableFileCache(false);
		imageUtil.setOnFailCallBackListener(new OnFailCallBackListener() {
			@Override
			public void onFailCallBack(Throwable throwable) {
				Log.i(TAG, "加载失败"+throwable.toString());
			}
		});
		listView = (ListView) findViewById(R.id.listview);
		listView.setAdapter(new MyListAdapter());
		
	}
	class MyListAdapter extends BaseAdapter{
		class MyHolder{
			View convertView;
			ImageView iv;
			public MyHolder(View convertView) {
				this.convertView=convertView;
				iv= (ImageView) this.convertView.findViewById(R.id.iv);
				this.convertView.setTag(this);
			}
		}
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if(convertView==null)
			{
				convertView=View.inflate(parent.getContext(), R.layout.items, null);
				new MyHolder(convertView);
			}
			MyHolder myHolder= (MyHolder) convertView.getTag();
			imageUtil.display(list.get(position), myHolder.iv);
			return convertView;
		}
		@Override
		public int getCount() {
			
			return list.size();
		}

		@Override
		public Object getItem(int position) {
			
			return null;
		}

		@Override
		public long getItemId(int position) {
			
			return 0;
		}
		
	}
}
