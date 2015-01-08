package com.hxsmart.imatetest;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebSettings.ZoomDensity;
import android.webkit.WebView;
import android.webkit.WebSettings.LayoutAlgorithm;
import android.widget.Toast;

public class HelpActivity extends Activity 
{
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.help);
		
		Toast toast = Toast.makeText(HelpActivity.this, "���ڼ����ļ�......", 10000);
		toast.show();
		
		WebView helpview = (WebView) findViewById(R.id.helpview);
		WebSettings settings = helpview.getSettings();
		helpview.setHorizontalScrollBarEnabled(false);//������ˮƽ����ʾ
		helpview.setVerticalScrollBarEnabled(true); //��������ֱ��ʾ 
		


		
//		settings.setLayoutAlgorithm(LayoutAlgorithm.SINGLE_COLUMN);//ͼƬ�Զ���Ӧ���� settings.setUseWideViewPort(true); ��ͻ
		settings.setLoadWithOverviewMode(true);//ͼƬ�Զ���Ӧ
		settings.setSupportZoom(true); 
		settings.setBuiltInZoomControls(true);
		settings.setUseWideViewPort(true); // ֧��˫�� �Ŵ���С
		
		helpview.loadUrl("file:///android_asset/help/help01.jpg");
	}
	@Override
	protected void onStart() {
        super.onStart();
        ActionBar actionBar = this.getActionBar();
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setTitle("       iMate����ָ��");
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		
		return super.onCreateOptionsMenu(menu);
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if(id == android.R.id.home)
		{
			Intent intent = new Intent();
		    intent.setClass(HelpActivity.this, MainActivity.class);
		    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		    startActivity(intent);
		    overridePendingTransition(R.anim.in_from_left,  R.anim.out_to_right);
		}
		return super.onOptionsItemSelected(item);
	}
}
