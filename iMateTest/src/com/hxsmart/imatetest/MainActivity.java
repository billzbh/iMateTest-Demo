package com.hxsmart.imatetest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.ExpandableListActivity;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebSettings.LayoutAlgorithm;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;

//导入外包
import com.hxsmart.imateinterface.*;
import com.hxsmart.imateinterface.fingerprint.Fingerprint;
import com.hxsmart.imateinterface.memorycardapi.MemoryCard;
import com.hxsmart.imateinterface.mifcard.MifCard;
import com.hxsmart.imateinterface.pbochighsdk.*;
import com.hxsmart.imateinterface.pbocissuecard.PbocIssueCard;
import com.hxsmart.imateinterface.pinpad.*;
import com.hxsmart.imateinterface.printer.DoPrint;
import com.hxsmart.imateinterface.extension.*;
import com.hxsmart.imateinterface.fingerprint.FingerprintZhongZhengForUSB;

//import com.hxsmart.imateinterface.jsbpbocsdk.JsbPbocApi;
import com.ivsign.android.IDCReader.IDCReaderSDK;


public class MainActivity extends ExpandableListActivity {
	
	private LogViewAppendHandler logViewAppendHandler;
	private ImageChangeHandler imageChangeHandler;
	private static boolean threadStarted=false;
	private static boolean isWorking=false;
	public static BluetoothThread bluetoothThread;
	private Pinpad pinpad;
	public MifCard mifCard;
	public DoPrint doPrint;
	Fingerprint fingerprint;
	public MemoryCard memoryCard;

	//语音操作对象  
    private HxRecorder hxRecorder = null;

	private EditText logview;
	
	final static String wltlibDirectory="/data/data/com.hxsmart.imatetest/wltlib";
	private ImageView image;
	private WebView webView;
	private LayoutInflater inflater;
	private View layout;

	public void button00()
	{
		if (isWorking)
			return;
		logview.setText("");
		
		if (bluetoothThread.deviceIsConnecting()) {
			logview.append("蓝牙已经连接成功\n");
		}
		else {
			logview.append("iMate蓝牙未连接\n");
		}
	}
	public void button01()
	{
		if (isWorking)
			return;
		logview.setText("");
		
		logview.setMinHeight(400);
		logview.setMinWidth(2000);
		
		if (bluetoothThread.deviceIsConnecting()) {
			logview.append("iMate固件版本：" + bluetoothThread.deviceVersion() + "\n");
			String batteryLeve = bluetoothThread.getBatteryLeve();
			if (batteryLeve != null)
				logview.append("iMate电池电量：%" + bluetoothThread.getBatteryLeve()+ "\n");
			String SerialNo = bluetoothThread.deviceSerialNumber();
			if(SerialNo!=null)
				logview.append("终端序列号：" + SerialNo + "\n");
			else
				logview.append("此设备不支持查看终端序列号的功能 ！");
		}
		else {
			logview.append("iMate蓝牙未连接\n");
		}	
	}

	public void button02()
	{
		if (isWorking) {
			logview.setText("");
			bluetoothThread.cancel();
			return;
		}
		if (!bluetoothThread.deviceIsConnecting()) {
			logview.append("iMate蓝牙未连接\n");
			return;
		}
		
		logview.setMinHeight(400);
		logview.setMinWidth(2000);
		logview.setText("请放置好二代身份证...\n");
		
		webView.loadUrl("file:///android_asset/gif/id_play.gif");
		webView.setVisibility(View.VISIBLE);
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				isWorking = true;
				int retCode;
				IdInformationData idInformationDataData = new IdInformationData();
				String message;
				retCode = bluetoothThread.readIdInformation(idInformationDataData, 20);
				
				writeViewFromThread(webView);//通过线程设置图片消失不见
				writeViewFromThread(logview);//把logview的大小设置回来
				
				switch (retCode) {
				case 0:
					message = "读二代证成功:\n\n" + "姓名:" + idInformationDataData.getNameString() + "\n"
										+ "性别:" + idInformationDataData.getSexString() + "\n"
										+ "民族:" + idInformationDataData.getNationString() + "\n"
										+ "出生:" + idInformationDataData.getBirthdayYearString() + "年" + idInformationDataData.getBirthdayMonthString() + "月" + idInformationDataData.getBirthdayDayString() + "日" + "\n"
										+ "住址:" + idInformationDataData.getAddressString() + "\n"
										+ "身份号码:" + idInformationDataData.getIdNumberString() + "\n"
										+ "签发机关:" + idInformationDataData.getIssuerString() + "\n"
										+ "有效期限:" + idInformationDataData.getValidDateString();						
					break;
				case 1:
					message = "通讯超时";						
					break;
				case 9:
					message = "蓝牙设备未连接";					
					break;
				default:
					message = idInformationDataData.getErrorString();												
					break;
				}
				writeLogFromThread(message);
				
				
				if (retCode == 0) {						
					retCode = IDCReaderSDK.decodingPictureData(wltlibDirectory, idInformationDataData.getPictureData());
					switch (retCode) {
					case 0:
						message = "照片解码成功";
						Bitmap bm = BitmapFactory.decodeFile(wltlibDirectory+File.separator+"zp.bmp");
						writeViewFromThread(bm);
						break;							
					case 1:
						message = "照片解码初始化失败，需要检查传入的wltlibDirectory以及base.dat文件";
						break;
					case 2:
						message = "授权文件license.lic错误";
						break;
					case 3:
						message = "照片解码失败，其它错误";
						break;
					}
					writeLogFromThread(message);					
				}						
				
				isWorking = false;
			}
		}).start();
	}
	public void button03()
	{
		if (isWorking) {
			logview.setText(" ");
			bluetoothThread.cancel();
			return;
		}
		if (!bluetoothThread.deviceIsConnecting()) {
			logview.append("iMate蓝牙未连接\n");
			return;
		}
		
		logview.setMinHeight(400);
		logview.setMinWidth(2000);
		logview.setText("请刷磁条卡...\n");	
		
		webView.loadUrl("file:///android_asset/gif/mag_play.gif");
		webView.setVisibility(View.VISIBLE);
					
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				isWorking = true;
				int retCode;
				MagCardData cardData = new MagCardData();
				String message;
				retCode = bluetoothThread.swipeCard(cardData, 20);
				
				writeViewFromThread(webView);//通过线程设置图片消失不见
				writeViewFromThread(logview);
				
				switch (retCode) {
				case 0:
					message = "刷卡成功:\n\n" + "卡号:" + cardData.getCardNoString() + "\n"
							+ "二磁道数据:" + cardData.getTrack2String() + "\n"
							+ "三磁道数据:" + cardData.getTrack3String();			
					break;
				case 1:
					message = "通讯超时";						
					break;
				case 9:
					message = "蓝牙设备未连接";						
					break;
				default:
					message = cardData.getErrorString();												
					break;
				}	
				writeLogFromThread(message);
				isWorking = false;
			}
		}).start();
	}
	public void button04()
	{
		if (isWorking) {
			logview.setText("");
			bluetoothThread.cancel();
			return;
		}
		if (!bluetoothThread.deviceIsConnecting()) {
			logview.append("iMate蓝牙未连接\n");
			return;
		}
		
		logview.setMinHeight(400);
		logview.setMinWidth(2000);
		logview.setText("检测SAM卡-1...\n");
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				isWorking = true;
				ApduExchangeData apduData = new ApduExchangeData();
				apduData.reset(); 
				apduData.setSlot(ApduExchangeData.SAM1_SLOT); //设置卡座号，第一SAM卡座 
				
				int retCode = bluetoothThread.pbocReset(apduData, 0);
				
				String message;
				switch (retCode) {
				case 0:
					message = "SAM1卡复位成功";			
					break;
				case 1:
					message = "通讯超时";						
					break;
				case 9:
					message = "蓝牙设备未连接";						
					break;
				default:
					message = apduData.getErrorString() + apduData.getSlot() + "," + ApduExchangeData.SAM1_SLOT;	
					message += "\n\n请检查是否放入PSAM卡";
					break;
				}
				//apduData.getResetDataBytes();
				writeLogFromThread(message);
				if (retCode != 0) {
					isWorking = false;
					return;
				}
				
				//apdu获取随机数
				apduData.reset(); 
				apduData.setSlot(ApduExchangeData.SAM1_SLOT); //设置卡座号，第一SAM卡座  
				apduData.setCardType(ApduExchangeData.NORMAL_CARD_TYPE); //设置卡类型
				
				byte[] apduIn = new byte[5];
				apduIn[0] = 0x00;
				apduIn[1] = (byte)0x84;
				apduIn[2] = 0x00;
				apduIn[3] = 0x00;
				apduIn[4] = 0x04;
				
				apduData.setAuduInBytes(apduIn);
				apduData.setInLength(5);
				retCode = bluetoothThread.pbocApdu(apduData);
				if (retCode == 0) {
					apduData.getAuduOutBytes();
				}
				switch (retCode) {
				case 0:
					byte[] status = apduData.getStatus();
					byte[] outBytes = apduData.getAuduOutBytes();
					message = "SAM卡取随机数成功\n状态位：";
					for (int m=0; m<status.length; m++) {
						message += Integer.toHexString((status[m]&0x000000ff)|0xffffff00).substring(6);
					}
					if (outBytes != null) {			
						message += "\n随机数：";
						for (int m=0; m<outBytes.length; m++) {
							message += Integer.toHexString((outBytes[m]&0x000000ff)|0xffffff00).substring(6);
						}
					}
					break;
				case 1:
					message = "通讯超时";						
					break;
				case 9:
					message = "蓝牙设备未连接";						
					break;
				default:
					//message = apduData.getErrorString();
					message = "其它错误";
					break;
				}
				writeLogFromThread(message);
				
				isWorking = false;
			}
		}).start();
	}
	

	
	public void button05()
	{
		logview.setText("打印测试...\n");
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				writeLogFromThread(""+doPrint.bluePrint("测试打印机\n======================================\n商家:华信智能有限公司\n产品: iMate金融伴侣\n价格:￥5000.00\n" +
						"======================================\n客户签名:\n"));
					
			}
		}).start();
	}
	
	public void button06()
	{
		logview.setText("重新打开连接bluePrinter\n");
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				writeLogFromThread("结果："+doPrint.reOpenPrinter());
			}
		}).start();
	}
	
	public void button07(){
		
		if (isWorking) {
			logview.setText(" ");
			bluetoothThread.cancel();
			return;
		}
		if (!bluetoothThread.deviceIsConnecting()) {
			logview.append("iMate蓝牙未连接\n");
			return;
		}
		
		logview.setText("IC，磁条等待事件\n");
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				isWorking = true;
				byte[] ret = new byte[256];
				try
				{
					ret=bluetoothThread.waitEvent(0x01|0x02, 20);
				}
				catch(Exception e)
				{
					isWorking = false;
					//通讯超时或者蓝牙未连接
					Log.e("zbh",""+e.getMessage());
					writeLogFromThread(e.getMessage());
				}
				
				writeLogFromThread("检测到事件:"+ret[0]);
				//判断事件
				if(ret[0]==0x01)
				{
					Log.e("zbh", "检测到刷卡事件：");
					String retString = "";
					for (int m=1; m<ret.length; m++) {
						retString += ret[m]-48;
					}
					//取卡号
					writeLogFromThread("卡号："+retString.substring(0,16));
				}
				else if(ret[0]==0x02)
				{
					Log.e("zbh", "检测到IC卡事件：");
					IcCardData icCardData = new IcCardData();
					String message;
					int retCode = bluetoothThread.readIcCard(icCardData, 10);
					switch (retCode) {
					case 0:
						message = "卡号:" + icCardData.getCardNoString() + "\n";					
						break;
					case 1:
						message = "通讯超时";						
						break;
					case 9:
						message = "蓝牙设备未连接";						
						break;
					default:
						message = icCardData.getErrorString();												
						break;
					}
					writeLogFromThread(message);
					
				}
				else
				{
					//射频事件
				}
				isWorking = false;
			}
		}).start();
	}
	
	public void button08()
	{
		logview.setText("扫条码测试");
		HxBarcode hxBarcode = new HxBarcode();
		hxBarcode.scan(MainActivity.this, 501);
	}
	
	public void button09()
	{
		logview.setText("拍照测试");
		HxCamera camera = new HxCamera();
		camera.takePhoto(MainActivity.this, 100, HxCamera.FILE_URI, 900 , 450 , 601);
	}
	
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		System.out.printf("%s,%s\n",requestCode,resultCode);
		switch(requestCode)
		{
		case 501:
			if(data!=null)
			{
				String scanResult = data.getStringExtra("data");
				logview.append("\n扫描结果：\n"+scanResult);
			}
			break;
		case 601:
			String scanResult = data.getStringExtra("data");
			logview.append("\n照片URI：\n"+scanResult);
			break;
		}
	}
	
	
	public void button10()
	{
		if (isWorking) {
			logview.setText("");
			pinpad.cancel();
			return;
		}
		if (!bluetoothThread.deviceIsConnecting()) {
			logview.append("iMate蓝牙未连接\n");
			return;
		}
		
		
		logview.setText("iMate指纹模块测试(JSABC)\n");

		new Thread(new Runnable() {
			@Override
			public void run() {
				isWorking = true;

				// 设置指纹模块型号
				fingerprint.fingerprintSetModel(Fingerprint.FINGERPRINT_MODEL_JSABC);
				
				// 打开指纹模块电源
				try {
					fingerprint.powerOn();
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());
					isWorking = false;
					return;
				}
				
				writeLogFromThread("打开指纹模块电源成功");

				writeLogFromThread("检查版本号...");
				String versionString;
				try {
					versionString = fingerprint.getVersion();
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());
					isWorking = false;
					return;
				}
				writeLogFromThread("指纹模块版本:" + versionString);
				
				writeLogFromThread("获取指纹特征值...请按手指");
				String fingerprintFeatureString;
				try {
					fingerprintFeatureString = fingerprint.takeFingerprintFeature();
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());
					isWorking = false;
					return;
				}
				writeLogFromThread("指纹特征值:" + fingerprintFeatureString);

				try {
					fingerprint.powerOff();
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());
					isWorking = false;
					return;
				}
				writeLogFromThread("指纹模块下电成功");
				isWorking = false;
			}
		}).start();
	}
	public void button11()
	{
		if (isWorking) {
			logview.setText("");
			pinpad.cancel();
			return;
		}
		if (!bluetoothThread.deviceIsConnecting()) {
			logview.append("iMate蓝牙未连接\n");
			return;
		}
		
		
		logview.setText("iMate指纹模块测试(Shengteng)\n");

		new Thread(new Runnable() {
			@Override
			public void run() {
				isWorking = true;	
				// 设置指纹模块型号
				fingerprint.fingerprintSetModel(Fingerprint.FINGERPRINT_MODEL_SHENGTENG);
				
				// 打开指纹模块电源
				try {
					fingerprint.powerOn();
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());
					isWorking = false;
					return;
				}
				
				writeLogFromThread("打开指纹模块电源成功");

				writeLogFromThread("检查版本号...");
				String versionString;
				try {
					versionString = fingerprint.getVersion();
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());
					isWorking = false;
					return;
				}
				writeLogFromThread("指纹模块版本:" + versionString);
				
				writeLogFromThread("获取指纹特征值...请按手指");
				String fingerprintFeatureString;
				try {
					fingerprintFeatureString = fingerprint.takeFingerprintFeature();
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());
					isWorking = false;
					return;
				}
				writeLogFromThread("指纹特征值:" + fingerprintFeatureString);

				try {
					fingerprint.powerOff();
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());
					isWorking = false;
					return;
				}
				writeLogFromThread("指纹模块下电成功");
				isWorking = false;
			}
		}).start();
	}
	
	public void button12()
	{
		if (isWorking) {
			logview.setText("");
			pinpad.cancel();
			return;
		}
		if (!bluetoothThread.deviceIsConnecting()) {
			logview.append("iMate蓝牙未连接\n");
			return;
		}
		
		
		logview.setText("iMate指纹模块测试(中正)\n");

		new Thread(new Runnable() {
			@Override
			public void run() {
				isWorking = true;
				// 设置指纹模块型号
				fingerprint.fingerprintSetModel(Fingerprint.FINGERPRINT_MODEL_ZHONGZHENG);
				
				// 打开指纹模块电源
				try {
					fingerprint.powerOn();
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());
					isWorking = false;
					return;
				}
				
				writeLogFromThread("打开指纹模块电源成功");

				writeLogFromThread("获取模块描述符...");
				String versionString;
				try {
					versionString = fingerprint.getVersion();
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());
					isWorking = false;
					return;
				}
				writeLogFromThread("指纹模块描述符:" + versionString);
				
				writeLogFromThread("获取指纹特征值...请按手指");
				String fingerprintFeatureString;
				try {
					fingerprintFeatureString = fingerprint.takeFingerprintFeature();
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());
					isWorking = false;
					return;
				}
				writeLogFromThread("指纹特征值:" + fingerprintFeatureString);

				try {
					fingerprint.powerOff();
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());
					isWorking = false;
					return;
				}
				writeLogFromThread("指纹模块下电成功");
				isWorking = false;
			}
		}).start();
	}
	
	
	//中正指纹仪登记指纹生成模板
	public void button13()
	{
		if (isWorking) {
			logview.setText("");
			pinpad.cancel();
			return;
		}
		if (!bluetoothThread.deviceIsConnecting()) {
			logview.append("iMate蓝牙未连接\n");
			return;
		}
		
		
		logview.setText("iMate指纹模块测试(中正)\n");

		new Thread(new Runnable() {
			@Override
			public void run() {
				isWorking = true;
				// 设置指纹模块型号
				fingerprint.fingerprintSetModel(Fingerprint.FINGERPRINT_MODEL_ZHONGZHENG);
				
				// 打开指纹模块电源
				try {
					fingerprint.powerOn();
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());
					isWorking = false;
					return;
				}
				
				writeLogFromThread("打开指纹模块电源成功");
				writeLogFromThread("登记3次指纹生成指纹模板...请按3次手指");
				String fingerprintFeatureString;
				try {
					fingerprintFeatureString = fingerprint.GenerateFingerTemplate();
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());
					isWorking = false;
					return;
				}
				writeLogFromThread("指纹模板数据:" + fingerprintFeatureString);

				try {
					fingerprint.powerOff();
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());
					isWorking = false;
					return;
				}
				writeLogFromThread("指纹模块下电成功");
				isWorking = false;
			}
		}).start();
	}

	
	public void button20()
	{
		if (isWorking) {
			logview.setText("");
			pinpad.cancel();
			return;
		}
		if (!bluetoothThread.deviceIsConnecting()) {
			logview.append("iMate蓝牙未连接\n");
			return;
		}
		
		logview.setText("凯明杨密码键盘测试\n");
		pinpad.setPinpadModel(Pinpad.KMY_MODEL);
		new Thread(new Runnable() {
			@Override
			public void run() {
				isWorking = true;
				
				// 打开密码键盘电源
				try {
					pinpad.powerOn();
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());
					isWorking = false;
					return;
				}
				writeLogFromThread("打开密码键盘电源成功");
				
				// 等待1秒，密码键盘完成上电后再继续操作
				try {
	                Thread.sleep(1000);
	            }
	            catch (InterruptedException e) {
	            }

				writeLogFromThread("\n密码键盘复位自检...");
				try {
					pinpad.reset(false);
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());					
					isWorking = false;
					return;
				}
				writeLogFromThread("密码键盘复位自检成功");
				
				writeLogFromThread("\n读取序列号...");
				byte[] retbytes;
				try {
					retbytes = pinpad.getSerialNo();
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());
					isWorking = false;
					return;
				}
				writeLogFromThread("Pinpad序列号：" + bytesToHexString(retbytes, 0, retbytes.length));
				
				writeLogFromThread("\n下载主密钥...");
				byte[] masterKey = {0x00, 0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,0x0a,0x0b,0x0c,0x0d,0x0e,0x0f};
				try {
					pinpad.downloadMasterKey(true, 15, masterKey, masterKey.length);
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());
					isWorking = false;
					return;
				}
				writeLogFromThread("下载主密钥成功");
				
				writeLogFromThread("\n下载工作密钥...");
				byte[] workingKey = {0x12, 0x34,0x56,0x78, (byte)0x90, (byte)0xab,(byte)0xcd,(byte)0xef,0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08};
				try {
					pinpad.downloadWorkingKey(true, 15, 1,  workingKey, workingKey.length);
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());
					isWorking = false;
					return;
				}
				writeLogFromThread("下载工作密钥成功");
				
				writeLogFromThread("\n输入PinBlock...");
				byte[] pinblock;
				try {
					pinblock = pinpad.inputPinblock(true, false, 15, 1, "1234567890123", 6, 20);
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());
					isWorking = false;
					return;
				}
				writeLogFromThread(bytesToHexString(pinblock, 0, pinblock.length) + "\n输入PinBlock成功");
				
				try {
					pinpad.powerOff();
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());
					isWorking = false;
					return;
				}
				writeLogFromThread("\nPinpad下电成功");
				isWorking = false;
			}
		}).start();
	}
	

	public void button21()
	{
		if (isWorking) {
			logview.setText("");
			pinpad.cancel();
			return;
		}
		if (!bluetoothThread.deviceIsConnecting()) {
			logview.append("iMate蓝牙未连接\n");
			return;
		}
		
		logview.setText("信雅达密码键盘测试\n");
		pinpad.setPinpadModel(Pinpad.XYD_MODEL);
		new Thread(new Runnable() {
			@Override
			public void run() {
				isWorking = true;
				
				// 打开密码键盘电源
				try {
					pinpad.powerOn();
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());
					isWorking = false;
					return;
				}
				writeLogFromThread("打开密码键盘电源成功");
				
				// 等待2秒，密码键盘完成上电后再继续操作
				try {
	                Thread.sleep(2000);
	            }
	            catch (InterruptedException e) {
	            }

				writeLogFromThread("\n密码键盘复位自检...");
				try {
					pinpad.reset(false);
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());
					isWorking = false;
					return;
				}
				writeLogFromThread("密码键盘复位自检成功");
				
				writeLogFromThread("\n下载主密钥...");
				byte[] masterKey = {0x00, 0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,0x0a,0x0b,0x0c,0x0d,0x0e,0x0f};
				try {
					pinpad.downloadMasterKey(true, 1, masterKey, masterKey.length);
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());
					isWorking = false;
					return;
				}
				writeLogFromThread("下载主密钥成功");
				
				writeLogFromThread("\n下载工作密钥...");
				byte[] workingKey = {0x12, 0x34,0x56,0x78, (byte)0x90, (byte)0xab,(byte)0xcd,(byte)0xef,0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08};
				try {
					pinpad.downloadWorkingKey(true, 1, 1,  workingKey, workingKey.length);
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());
					isWorking = false;
					return;
				}
				writeLogFromThread("下载工作密钥成功");
				
				writeLogFromThread("\n输入PinBlock...");
				byte[] pinblock;
				try {
					pinblock = pinpad.inputPinblock(true, true, 1, 1, "1234567890123", 6, 20);
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());
					isWorking = false;
					return;
				}
				writeLogFromThread(bytesToHexString(pinblock, 0, pinblock.length) + "\n输入PinBlock成功");
				
				try {
					pinpad.powerOff();
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());
					isWorking = false;
					return;
				}
				writeLogFromThread("\nPinpad下电成功");
				isWorking = false;
			}
		}).start();
	}
	
	public void button22()
	{
		if (isWorking) {
			logview.setText("");
			pinpad.cancel();
			return;
		}
		if (!bluetoothThread.deviceIsConnecting()) {
			logview.append("iMate蓝牙未连接\n");
			return;
		}
		
		
		logview.setText("iMate指纹模块测试(USB 中正)\n");

		new Thread(new Runnable() {
			@Override
			public void run() {
				isWorking = true;
				// 设置指纹模块型号
				FingerprintZhongZhengForUSB usbFinger = new FingerprintZhongZhengForUSB();
				
				// 打开指纹模块电源
				try {
					usbFinger.powerOn();
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());
					isWorking = false;
					return;
				}
				
				writeLogFromThread("打开指纹模块电源成功");

				writeLogFromThread("获取模块描述符...");
				String versionString;
				try {
					versionString = usbFinger.getVersion();
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());
					isWorking = false;
					return;
				}
				writeLogFromThread("指纹模块描述符:" + versionString);
				
				writeLogFromThread("获取指纹特征值...请按手指");
				String fingerprintFeatureString;
				try {
					fingerprintFeatureString = usbFinger.takeFingerprintFeature();
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());
					isWorking = false;
					return;
				}
				writeLogFromThread("指纹特征值:" + fingerprintFeatureString);

				try {
					usbFinger.powerOff();
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());
					isWorking = false;
					return;
				}
				writeLogFromThread("指纹模块下电成功");
				isWorking = false;
			}
		}).start();
	}
	

	
	public void button60(){		
		String FileName = Environment.getExternalStorageDirectory().getAbsolutePath();  
        FileName += "/audiorecordtest"; 
        
        try {
        	hxRecorder.startRecorder(FileName);  
        } catch (Exception e) {  
            logview.setText(e.getMessage());
            //logview.setText("启动后台录音失败\n");
            return;
        }  
        logview.setText("启动后台录音成功\n");
	}
	
	public void button61()
	{
		if (hxRecorder == null) {
	        logview.setText("对象未创建\n");
	        return;
		}
		hxRecorder.stopRecorder();        
        logview.setText("关闭后台录音成功\n");
	}
	
	public void button62()
	{	
		String FileName = Environment.getExternalStorageDirectory().getAbsolutePath();  
        FileName += "/audiorecordtest"; 
        
        try {
        	hxRecorder.startPlay(FileName);  
        } catch (Exception e) {  
            logview.setText(e.getMessage());
            return;
        }  
        logview.setText("启动播放录音成功\n");
	}
	
	public void button63()
	{
		if (hxRecorder == null) {
	        logview.setText("对象未创建\n");
	        return;
		}
		hxRecorder.stopPlay();        
        logview.setText("关闭播放录音成功\n"); 
	}
		
//	public void button30()
//	{
//		if (isWorking) {
//			logview.setText("");
//			pinpad.cancel();
//			return;
//		}
//		if (!bluetoothThread.deviceIsConnecting()) {
//			logview.setText("蓝牙未连接");
//			return;
//		}
//		logview.setText("");
//		logview.append("Pboc核心测试...\n");
//		logview.setMinHeight(400);
//		logview.setMinWidth(2000);
//		logview.setText("请插入Pboc IC卡...\n");
//		
//		webView.loadUrl("file:///android_asset/gif/ic_play.gif");
//		webView.setVisibility(View.VISIBLE);
//
//
//		new Thread(new Runnable() {
//			@Override
//			public void run() {
//				isWorking = true;
//				PbocApiDemo pbocApiDemo = new PbocApiDemo();
//				pbocApiDemo.setLogHandler(logViewAppendHandler);
//				
//				writeLogFromThread("---- 终端初始化 ----");
//				try {
//					pbocApiDemo.doPbocDemoInit();
//				} catch (Exception e) {
//					writeLogFromThread(e.getMessage());
//					isWorking = false;
//					return;
//				}
//				writeLogFromThread("---- 开始交易 ----");
//				try {				
//					pbocApiDemo.doPbocDemoTrans_1();
//					writeLogFromThread("");
//					pbocApiDemo.getTransData();
//					writeLogFromThread("");
//					pbocApiDemo.doPbocDemoTrans_2();
//				} catch (Exception e) {
//					writeLogFromThread(e.getMessage());
//				}
//
//				writeLogFromThread("Pboc核心测试完成");
//				isWorking = false;
//			}
//		}).start();
//	}
	
	public void button30()
	{
		if (isWorking) {
			logview.setText("");
			bluetoothThread.cancel();
			return;
		}
		if (!bluetoothThread.deviceIsConnecting()) {
			logview.append("iMate蓝牙未连接\n");
			return;
		}
		
		logview.setMinHeight(400);
		logview.setMinWidth(2000);
		logview.setText("请插入 POBC IC 卡...\n");
		
		webView.loadUrl("file:///android_asset/gif/ic_play.gif");
		webView.setVisibility(View.VISIBLE);
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				isWorking = true;
				int retCode;
				IcCardData icCardData = new IcCardData();
				String message;
				retCode = bluetoothThread.readIcCard(icCardData, 20);
				writeViewFromThread(webView);//通过线程设置图片消失不见
				writeViewFromThread(logview);
				
				switch (retCode) {
				case 0:
					message = "读IC卡成功:\n\n" + "卡号:\t\t\t\t\t" + icCardData.getCardNoString() + "\n"
							+ "序列号:\t\t\t\t" + icCardData.getPanSequenceNoString() + "\n"
							+ "持卡人姓名:\t\t\t" + icCardData.getHolderNameString() + "\n"
							+ "持卡人证件号码:\t" + icCardData.getHolderIdString() + "\n"
							+ "有效期:\t\t\t\t" + icCardData.getExpireDateString() + "\n" 
							+"二磁道等效数据:\t" + icCardData.getTrack2String();						
					break;
				case 1:
					message = "通讯超时";						
					break;
				case 9:
					message = "蓝牙设备未连接";						
					break;
				default:
					message = icCardData.getErrorString();												
					break;
				}
				writeLogFromThread(message);
				isWorking = false;
			}
		}).start();
	}
	
	public void button31()
	{
		if (isWorking) {
			logview.setText("");
			pinpad.cancel();
			return;
		}
		if (!bluetoothThread.deviceIsConnecting()) {
			logview.append("iMate蓝牙未连接\n");
			return;
		}
		
		logview.append("Pboc交易接口测试...\n");
		logview.setMinHeight(400);
		logview.setMinWidth(2000);
		logview.append("请插入Pboc IC卡...\n");	
		webView.loadUrl("file:///android_asset/gif/ic_play.gif");
		webView.setVisibility(View.VISIBLE);
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				isWorking = true;
				
				PbocCardData pbocCardData = new PbocCardData();
				PbocHighApi pbocHighApi = new PbocHighApi();
				
				ApduExchangeData apduData = new ApduExchangeData();
				apduData.reset();    
				bluetoothThread.pbocReset(apduData, 20);
				writeViewFromThread(webView);//通过线程设置图片消失不见
				writeViewFromThread(logview);//更新textview大小。
								
				writeLogFromThread("正在测试Pboc交易接口...");
				int ret = pbocHighApi.iHxPbocHighInitCore("123456789000001", "12345601", "泰然工贸园212栋401", 156, 156);
				if (0 == ret)
				{	
					writeLogFromThread("交易初始化...");
					String szDateTime = new String("20140611100000");
					ret = pbocHighApi.iHxPbocHighInitTrans(szDateTime, 1, 0x31, 0, pbocCardData);
					if (ret == 0) 
					{
						writeLogFromThread("开始交易\n打印信息如下：");
						writeLogFromThread("Field55: "+pbocCardData.field55 +"\nPan: " + pbocCardData.pan + "\nPanSeqNo: " + pbocCardData.panSeqNo
							            + "\nTrack2: " + pbocCardData.track2 + "\nExtInfo: " + pbocCardData.extInfo);
					
						// Field55上送后台，后台返回的数据存在szIssuerData中, 输出的outField55再上送后台
						// int iRet = pbocHighApi.iHxPbocHighDoTrans(szIssuerData, outField55, outLength);
					
						writeLogFromThread("Pboc交易测试成功");
					}
					else
					{	
						writeLogFromThread("Pboc交易测试失败,返回值ret:"+ ret);
					}
					writeLogFromThread("获取TAG值");
					String valueOf9F77 = pbocHighApi.szHxPbocHighGetTagValue("9F77");
					writeLogFromThread("TAG-9F77 = [" + valueOf9F77 + "]");
					String valueOf5F34 = pbocHighApi.szHxPbocHighGetTagValue("5F34");
					writeLogFromThread("TAG-5F34 = [" + valueOf5F34 + "]");
					String valueOf57 = pbocHighApi.szHxPbocHighGetTagValue("57");
					writeLogFromThread("TAG-57 = [" + valueOf57 + "]");
					String valueOf5A = pbocHighApi.szHxPbocHighGetTagValue("5A");
					writeLogFromThread("TAG-5A = [" + valueOf5A + "]");	
				}
				else
				{
					writeLogFromThread("Pboc核心初始化失败,返回值ret:"+ ret);
				}
				isWorking = false;
			}
		}).start();
	}
	
	public void button32()
	{
		if (isWorking) {
			logview.setText("");
			pinpad.cancel();
			return;
		}
		if (!bluetoothThread.deviceIsConnecting()) {
			logview.append("iMate蓝牙未连接\n");
			return;
		}
		
		logview.append("PBOC卡创建应用演示...\n\n");
		logview.setMinHeight(400);
		logview.setMinWidth(2000);
		logview.append("请插入一张空白 PBOC 测试卡...\n");
		
		webView.loadUrl("file:///android_asset/gif/ic_play.gif");
		webView.setVisibility(View.VISIBLE);
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				isWorking = true;
				
				int timeout = 20;
				PbocIssueCard pbocIssue = new PbocIssueCard();
				int flag=pbocIssue.pbocIssueInit();
				if (flag != 0 ) {
					writeLogFromThread(String.format("Issue Card初始化失败:%d"));
					isWorking = false;
					return;
				}
				
				ApduExchangeData apduData = new ApduExchangeData();
				apduData.reset();    
				bluetoothThread.pbocReset(apduData, 20);
				writeViewFromThread(webView);//通过线程设置图片消失不见
				writeViewFromThread(logview);//更新textview大小。
				
				pbocIssue.setStatusHandler(logViewAppendHandler);
				
				pbocIssue.setPanString("8888800000123456");
				pbocIssue.setExpireDateString("050128");
				pbocIssue.setHolderNameString("Zhangsan");
				pbocIssue.setHolderIdType(0);
				pbocIssue.setHolderIdString("33010119800201202X");
				pbocIssue.setDefaultPinString("000000");
				pbocIssue.setPanSerialNo(1);
				pbocIssue.setAidString("A000000333010101");
				pbocIssue.setLabelString("debit");
				pbocIssue.setCaIndex(79);
				pbocIssue.setIcRsaKeyLen(128);
				pbocIssue.setIcRsaE(3);
				pbocIssue.setCountryCode(156);
				pbocIssue.setCurrencyCode(156);
				String retString = "创建PBOC应用成功";
				try {
					pbocIssue.pbocIssueCard(timeout);
				}catch (Exception e) {
					retString = "创建PBOC应用失败:" + e.getMessage();
				}
				
				writeLogFromThread(retString);
				isWorking = false;
			}
		}).start();
	}
	
	public void button33()
	{
		if (isWorking) {
			logview.setText("");
			pinpad.cancel();
			return;
		}
		if (!bluetoothThread.deviceIsConnecting()) {
			logview.append("iMate蓝牙未连接\n");
			return;
		}
		
		logview.append("读取Pboc卡信息（扩展）...\n");
		logview.setMinHeight(400);
		logview.setMinWidth(2000);
		logview.append("请插入 PBOC IC卡...\n");	
		webView.loadUrl("file:///android_asset/gif/ic_play.gif");
		webView.setVisibility(View.VISIBLE);
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				isWorking = true;
				
				PbocHighApi pbocHighApi = new PbocHighApi();
				
				ApduExchangeData apduData = new ApduExchangeData();
				apduData.reset();    
				bluetoothThread.pbocReset(apduData, 20);
				writeViewFromThread(webView);//通过线程设置图片消失不见
				writeViewFromThread(logview);//更新textview大小。
				
				byte[] outData = new byte[1024];
				writeLogFromThread("正在读取IC卡...\n");
				int ret = pbocHighApi.iHxPbocHighInitCore("123456789000001", "12345601", "泰然工贸园212栋401", 156, 156);
				if (0 == ret)
				{
					ret = pbocHighApi.iHxPbocHighReadInfoEx(outData, 0);

					if (ret == 0) 
					{
						String str = new String(outData);
						writeLogFromThread("\nPboc 扩展信息：\n");
						writeLogFromThread(str);
						
						writeLogFromThread("\n读取完毕");
					}
					else
					{	
						writeLogFromThread("\n读取IC卡失败");
					}
				}
				isWorking = false;
			}
		}).start();
	}
	
	
	public void button40()
	{
		if (isWorking) {
			logview.setText("");
			bluetoothThread.cancel();
			return;
		}
		if (!bluetoothThread.deviceIsConnecting()) {
			logview.append("iMate蓝牙未连接\n");
			return;
		}
		
		logview.setText("Mifware one卡测试...\n");

		new Thread(new Runnable() {
			@Override
			public void run() {
				isWorking = true;
				byte[] uid;
					
				writeLogFromThread("\n请放置M1卡...");
				
				//1、等待卡片
				try {
					uid = mifCard.waitCard(20);
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());
					isWorking = false;
					return;
				}
				String retString = "寻卡成功， UID：";
				for (int m=0; m<uid.length; m++) {
					retString += Integer.toHexString((uid[m]&0x000000ff)|0xffffff00).substring(6);
				}
				writeLogFromThread(retString);
				
				//2、认证扇区						
				byte[] key = {(byte)0xff,(byte)0xff,(byte)0xff,(byte)0xff,(byte)0xff,(byte)0xff};
				try {
					mifCard.mifareAuth(MifCard.Mifare_KeyA, 1, key);
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());
					isWorking = false;
					return;
				}
				
				writeLogFromThread("扇区1认证成功");
				
				//3、读1扇区0块
				byte[] block;
				try {
					block = mifCard.mifareRead(1*4+0);
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());
					isWorking = false;
					return;
				}
				retString = "1扇区0块读卡成功， Data：";
				for (int m=0; m<block.length; m++) {
					retString += Integer.toHexString((block[m]&0x000000ff)|0xffffff00).substring(6);
				}
				writeLogFromThread(retString);
				
				//4、写1扇区0块
				byte[] block2 = {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16};
				try {
					mifCard.mifareWrite(1*4, block2);
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());							
					isWorking = false;
					return;
				}
				retString = "1扇区0块写卡成功， Data：";
				for (int m=0; m<block2.length; m++) {
					retString += Integer.toHexString((block2[m]&0x000000ff)|0xffffff00).substring(6);
				}
				writeLogFromThread(retString);
				
				//5、读1扇区0块（对比）
				try {
					block = mifCard.mifareRead(1*4);
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());							
					isWorking = false;
					return;
				}
				retString = "1扇区0块读卡成功， Data：";
				for (int m=0; m<block.length; m++) {
					retString += Integer.toHexString((block[m]&0x000000ff)|0xffffff00).substring(6);
				}
				writeLogFromThread(retString);
				
				//6、初始化钱包（1扇区1块，100.00元）
				byte[] moneyBloack = mifInitMoney(10000);
				try {
					mifCard.mifareWrite(1*4+1, moneyBloack);
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());							
					isWorking = false;
					return;
				}
				retString = "1扇区1块写钱包初始化成功， Data：";
				for (int m=0; m<moneyBloack.length; m++) {
					retString += Integer.toHexString((moneyBloack[m]&0x000000ff)|0xffffff00).substring(6);
				}
				writeLogFromThread(retString);
				
				//7、钱包加值(10.00元）
				try {
					mifCard.mifareInc(1*4+1, 1000);
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());							
					isWorking = false;
					return;
				}
				writeLogFromThread("1扇区1块钱包加值成功");
				
				
				//8、读1扇区1块钱包（验证）
				try {
					block = mifCard.mifareRead(1*4+1);
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());							
					isWorking = false;
					return;
				}
				retString = "1扇区1块读卡成功， Data：";
				for (int m=0; m<block.length; m++) {
					retString += Integer.toHexString((block[m]&0x000000ff)|0xffffff00).substring(6);
				}
				writeLogFromThread(retString);
				
				//9、钱包减值(10.00元）
				try {
					mifCard.mifareDec(1*4+1, 1000);
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());							
					isWorking = false;
					return;
				}
				writeLogFromThread("1扇区1块钱包减值成功");
				
				//10、读1扇区钱包块（验证）
				try {
					block = mifCard.mifareRead(1*4+1);
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());							
					isWorking = false;
					return;
				}
				retString = "1扇区1块读卡成功， Data：";
				for (int m=0; m<block.length; m++) {
					retString += Integer.toHexString((block[m]&0x000000ff)|0xffffff00).substring(6);
				}
				writeLogFromThread(retString);
				
				//块拷贝（1块拷贝到2块）
				try {
					mifCard.mifareCopy(1*4+1, 1*4+2);
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());							
					isWorking = false;
					return;
				}
				writeLogFromThread("1扇区1块拷贝到1扇区2块成功");
				
				//读1扇区2块
				try {
					block = mifCard.mifareRead(1*4+2);
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());							
					isWorking = false;
					return;
				}
				retString = "1扇区2块读卡成功， Data：";
				for (int m=0; m<block.length; m++) {
					retString += Integer.toHexString((block[m]&0x000000ff)|0xffffff00).substring(6);
				}
				writeLogFromThread(retString);
										
				//恢复成全0状态						
				byte[] block3 = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
				try {
					mifCard.mifareWrite(1*4, block3);
					mifCard.mifareWrite(1*4+1, block3);
					mifCard.mifareWrite(1*4+2, block3);
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());							
					isWorking = false;
					return;
				}
				writeLogFromThread("扇区1数据复原成功");						
				writeLogFromThread("\n请移除卡片...");
				
				//移除卡片
				Boolean removal;
				try {
					removal = mifCard.waitRemoval(10);
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());							
					isWorking = false;
					return;
				}
				if (removal)
					writeLogFromThread("卡片移除成功");
				else 
					writeLogFromThread("卡片还未移除");
									
				isWorking = false;
				
				return;
			}
		}).start();
	}
	public void button41()
	{
		if (isWorking) {
			logview.setText("");
			bluetoothThread.cancel();
			return;
		}
		if (!bluetoothThread.deviceIsConnecting()) {
			logview.append("iMate蓝牙未连接\n");
			return;
		}
		
		logview.setText("Pboc读卡信息(射频)...\n");
		logview.append("请放置PBOC射频卡...\n");

		new Thread(new Runnable() {
			@Override
			public void run() {
				isWorking = true;					
				
				String cardInfoString;
				try {
					cardInfoString = mifCard.readPbocCard(20);
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());
					isWorking = false;
					return;
				}
				writeLogFromThread("PBOC卡信息:\n" + cardInfoString);									
				isWorking = false;
				
				return;
			}
		}).start();
	}
	public void button42()
	{
		if (isWorking) {
			logview.setText("");
			pinpad.cancel();
			return;
		}
		if (!bluetoothThread.deviceIsConnecting()) {
			logview.append("iMate蓝牙未连接\n");
			return;
		}
		
		logview.setText("Pboc交易接口测试(射频卡）...\n");
		logview.append("请放置PBOC射频卡...\n");	
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				isWorking = true;
				
				PbocCardData pbocCardData = new PbocCardData();
				PbocHighApi pbocHighApi = new PbocHighApi();
				
				ApduExchangeData apduData = new ApduExchangeData();
				apduData.reset();
				apduData.setSlot(ApduExchangeData.MIF_SLOT); 
				bluetoothThread.pbocReset(apduData, 20);
				writeViewFromThread(webView);//通过线程设置图片消失不见
				writeViewFromThread(logview);//更新textview大小。
				
				writeLogFromThread("正在测试Pboc交易接口...");
				
				pbocHighApi.vHxPbocHighSetCardReaderType(1);//设置射频卡读卡器
				int ret = pbocHighApi.iHxPbocHighInitCore("123456789000001", "12345601", "泰然工贸园212栋401", 156, 156);
				if (0 == ret)
				{	
					writeLogFromThread("交易初始化...");
					String szDateTime = new String("20140611100000");
					ret = pbocHighApi.iHxPbocHighInitTrans(szDateTime, 1, 0x00, 0, pbocCardData);
					if (ret == 0) 
					{
						writeLogFromThread("开始交易\n打印信息如下：");
						writeLogFromThread("Field55: "+pbocCardData.field55 +"\nPan: " + pbocCardData.pan + "\nPanSeqNo: " + pbocCardData.panSeqNo
							            + "\nTrack2: " + pbocCardData.track2 + "\nExtInfo: " + pbocCardData.extInfo);
					
						// Field55上送后台，后台返回的数据存在szIssuerData中, 输出的outField55再上送后台
						// int iRet = pbocHighApi.iHxPbocHighDoTrans(szIssuerData, outField55, outLength);
					
						writeLogFromThread("Pboc交易测试成功");
					}
					else
					{	
						writeLogFromThread("Pboc交易测试失败,返回值ret:"+ ret);
					}
					writeLogFromThread("获取TAG值");
					String valueOf9F77 = pbocHighApi.szHxPbocHighGetTagValue("9F77");
					writeLogFromThread("TAG-9F77 = [" + valueOf9F77 + "]");
					String valueOf5F34 = pbocHighApi.szHxPbocHighGetTagValue("5F34");
					writeLogFromThread("TAG-5F34 = [" + valueOf5F34 + "]");
					String valueOf57 = pbocHighApi.szHxPbocHighGetTagValue("57");
					writeLogFromThread("TAG-57 = [" + valueOf57 + "]");
					String valueOf5A = pbocHighApi.szHxPbocHighGetTagValue("5A");
					writeLogFromThread("TAG-5A = [" + valueOf5A + "]");	
				}
				else
				{
					writeLogFromThread("Pboc核心初始化失败,返回值ret:"+ ret);
				}
				isWorking = false;
			}
		}).start();
	}
	
	private int waitForInsertCard(int timeout) {
	    long timeMillis = System.currentTimeMillis() + timeout * 1000L; //整体20秒等待时间
	    while (System.currentTimeMillis() < timeMillis) {
	    	if (memoryCard.TestCard() != 0)
	    		return 1;
	    }
		return 0;
	}
	
	public void button50()
	{
		if (isWorking) {
			logview.setText("");
			bluetoothThread.cancel();
			return;
		}
		if (!bluetoothThread.deviceIsConnecting()) {
			logview.append("iMate蓝牙未连接\n");
			return;
		}
		
		if (memoryCard == null) {
			memoryCard = new MemoryCard();
		}
		
		logview.setText("测试逻辑加密卡类型接口测试...\n");
		logview.append("请插逻辑加密卡...\n\n");

		new Thread(new Runnable() {
			@Override
			public void run() {
				isWorking = true;	
				
				if (waitForInsertCard(10) == 0) {
					writeLogFromThread("未插卡");
					isWorking = false;
					return;
				}
				
				int cardType = memoryCard.TestCardType();
				if (cardType == 0) {
					isWorking = false;
					return;
				}
				
				byte[] dataBytes = new byte[10];
			
				System.out.println("cardtype = " + cardType);
				
				switch (cardType) {
				case MemoryCard.SLE4442_TYPE:
					writeLogFromThread("卡类型:SLE4442");
					memoryCard.SLE4442_Read(0, 2, dataBytes);
					break;
				case MemoryCard.AT102_TYPE:
					writeLogFromThread("卡类型:AT88SC102");
					memoryCard.AT102_ReadWords(0, 1, dataBytes);
					break;
				case MemoryCard.AT1604_TYPE:
					writeLogFromThread("卡类型:AT88SC1604");
					memoryCard.AT1604_Read(0, 1, dataBytes);
					break;
				case MemoryCard.AT1608_TYPE:
					writeLogFromThread("卡类型:AT88SC1608");
					memoryCard.AT1608_Read(1, 8, 2, dataBytes);
					break;
				case MemoryCard.SLE4428_TYPE:
					writeLogFromThread("卡类型:SLE4428");
					memoryCard.SLE4428_Read(0, 2, dataBytes);
					break;
				case MemoryCard.AT24Cxx_TYPE:
					writeLogFromThread("卡类型:AT24Cxx");
					memoryCard.AT24Cxx_Read(0, 2, dataBytes);					
					break;
				default:
					writeLogFromThread("无法识别该存储卡类型");
					break;					
				}
				writeLogFromThread("数据：" + bytesToHexString(dataBytes, 0, 2));
				writeLogFromThread("测试完成");
				
				isWorking = false;
				
				return;
			}
		}).start();
	}
	
	public void button51()
	{
		if (isWorking) {
			logview.setText("");
			bluetoothThread.cancel();
			return;
		}
		if (!bluetoothThread.deviceIsConnecting()) {
			logview.append("iMate蓝牙未连接\n");
			return;
		}
		
		if (memoryCard == null) {
			memoryCard = new MemoryCard();
		}
		
		logview.setText("SLE4442接口测试...\n");
		logview.append("请插SLE4442卡...\n");

		new Thread(new Runnable() {
			@Override
			public void run() {
				isWorking = true;	
								
				if (waitForInsertCard(10) == 0) {
					writeLogFromThread("未插卡");
					isWorking = false;
					return;
				}
				
				// ESAM卡产生过程密钥，需要在openCard之前调用，全程有效
				/*
				byte[] random = {1,2,3,4,5,6,7,8};
				memoryCard.GenCommKey(6, random);
				*/
				
				if (memoryCard.SLE4442_OpenAuto() != 0) {
					writeLogFromThread("卡片打开失败");
					isWorking = false;
					return;
				}
				/*
				writeLogFromThread("安全验证密码：43ae5affb653393f(随机数：0102030405060708)");
				byte[] passwordEx = {(byte)0x43, (byte)0xae, (byte)0x5a, (byte)0xff, (byte)0xb6, (byte)0x53, (byte)0x39, (byte)0x3f};
				if (memoryCard.SLE4442_ChkCodeEx(passwordEx) != 0) {
					writeLogFromThread("密码验证失败");
					isWorking = false;
					return;
				}
				*/
				
				writeLogFromThread("验证密码：ffffff");
				byte[] password1 = {(byte)0xff, (byte)0xff, (byte)0xff};
				byte[] password2 = {(byte)0xf0, (byte)0xf0, (byte)0xf0};
				if (memoryCard.SLE4442_ChkCode(password1) != 0) {
					writeLogFromThread("密码验证失败");
					writeLogFromThread("验证密码：f0f0f0");
					if (memoryCard.SLE4442_ChkCode(password2) != 0) {
						writeLogFromThread("密码验证失败");
						isWorking = false;
						return;
					}
					return;
				}
				
				writeLogFromThread("读卡数据：0~255");
				byte[] dataBytes = new byte[256];
				memoryCard.SLE4442_Read(0, 256, dataBytes);
				writeLogFromThread(bytesToHexString(dataBytes, 0, 256));
				
				writeLogFromThread("写卡数据, 从 100 开始, 100个字节");
				byte[] writeData = genRandom(100);				
				
				memoryCard.SLE4442_Write(100, 100, writeData);
				
				memoryCard.SLE4442_Read(0, 256, dataBytes);
				if (checkData(dataBytes, 100, writeData, 0, 100))
					writeLogFromThread("数据验证成功");
				else {
					writeLogFromThread("数据验证失败");
					isWorking = false;
					return;
				}				
				writeLogFromThread("数据：" + bytesToHexString(dataBytes, 0, 256));
				
				writeLogFromThread("测试完成");
				
				isWorking = false;
				
				return;
			}
		}).start();
	}

	public void button52()
	{
		if (isWorking) {
			logview.setText("");
			bluetoothThread.cancel();
			return;
		}
		if (!bluetoothThread.deviceIsConnecting()) {
			logview.append("iMate蓝牙未连接\n");
			return;
		}
		
		if (memoryCard == null) {
			memoryCard = new MemoryCard();
		}
		
		logview.setText("SLE4428接口测试...\n");
		logview.append("请插SLE4428卡...\n");

		new Thread(new Runnable() {
			@Override
			public void run() {
				isWorking = true;
				
				
				if (waitForInsertCard(10) == 0) {
					writeLogFromThread("未插卡");
					isWorking = false;
					return;
				}
				
				byte[] random = {1,2,3,4,5,6,7,8};
				memoryCard.GenCommKey(6, random);
				
				if (memoryCard.SLE4428_OpenAuto() != 0) {
					writeLogFromThread("卡片打开失败");
					isWorking = false;
					return;
				}
				
				writeLogFromThread("安全验证密码：43ae5affb653393f(随机数：0102030405060708)");
				byte[] passwordEx = {(byte)0x43, (byte)0xae, (byte)0x5a, (byte)0xff, (byte)0xb6, (byte)0x53, (byte)0x39, (byte)0x3f};
				if (memoryCard.SLE4428_ChkCodeEx(passwordEx) != 0) {
					writeLogFromThread("密码验证失败");
					isWorking = false;
					return;
				}
				
				writeLogFromThread("验证密码：ffff");
				byte[] password1 = {(byte)0xff, (byte)0xff};
				byte[] password2 = {(byte)0xf0, (byte)0xf0};
				if (memoryCard.SLE4428_ChkCode(password1) != 0) {
					writeLogFromThread("密码验证失败");
					writeLogFromThread("验证密码：f0f0");
					if (memoryCard.SLE4428_ChkCode(password2) != 0) {
						writeLogFromThread("密码验证失败");
						isWorking = false;
						return;
					}
				}
				writeLogFromThread("成功");
				
				writeLogFromThread("读卡数据：0~1023");
				byte[] dataBytes = new byte[1024];
				memoryCard.SLE4428_Read(0, 1024, dataBytes);
				writeLogFromThread("数据：" + bytesToHexString(dataBytes, 0, 1024));
				
				writeLogFromThread("写卡数据：100开始, 100个字节");
				byte[] writeData = genRandom(100);
				memoryCard.SLE4428_Write(100, 100, writeData);
				
				memoryCard.SLE4428_Read(0, 256, dataBytes);
				if (checkData(dataBytes, 100, writeData, 0, 100))
					writeLogFromThread("数据验证成功");
				else {
					writeLogFromThread("数据验证失败");
					isWorking = false;
					return;
				}
				writeLogFromThread("数据（0~255）：" + bytesToHexString(dataBytes, 0, 256));

				writeLogFromThread("测试完成");
				
				isWorking = false;
				
				return;
			}
		}).start();
	}
	public void button53()
	{
		if (isWorking) {
			logview.setText("");
			bluetoothThread.cancel();
			return;
		}
		if (!bluetoothThread.deviceIsConnecting()) {
			logview.append("iMate蓝牙未连接\n");
			return;
		}
		
		if (memoryCard == null) {
			memoryCard = new MemoryCard();
		}
		
		logview.setText("AT102接口测试<Level 1>...\n");
		logview.append("请插AT88SC102卡...\n");

		new Thread(new Runnable() {
			@Override
			public void run() {
				isWorking = true;	
				
				if (waitForInsertCard(10) == 0) {
					writeLogFromThread("未插卡");
					isWorking = false;
					return;
				}
				
				if (memoryCard.AT102_OpenAuto() != 0) {
					writeLogFromThread("卡片打开失败");
					isWorking = false;
					return;
				}
				writeLogFromThread("验证密码：f0f0");
				byte[] password1 = {(byte)0xf0, (byte)0xf0};
				byte[] password2 = {(byte)0xff, (byte)0xff};
				if (memoryCard.AT102_ChkCode(password1) != 0) {
					writeLogFromThread("密码验证失败");
					writeLogFromThread("验证密码：ffff");
					if (memoryCard.AT102_ChkCode(password2) != 0) {
						writeLogFromThread("密码验证失败");
						isWorking = false;
						return;
					}
				}
				writeLogFromThread("成功");
			
				byte[] dataBytes = new byte[200];
				writeLogFromThread("读全部数据:");
				memoryCard.AT102_ReadWords(0, 89, dataBytes);
				writeLogFromThread("数据：" + bytesToHexString(dataBytes, 0, 178));
				
				int appNo = 1;
				writeLogFromThread("读应用区" + appNo + ":");
				memoryCard.AT102_ReadAZ(appNo, dataBytes);
				writeLogFromThread("数据：" + bytesToHexString(dataBytes, 0, 64));
				
				writeLogFromThread("擦除应用区" + appNo + ":");
				if (memoryCard.AT102_EraseApp(appNo, 0, null) != 0) {
					writeLogFromThread("失败");
					isWorking = false;
					return;
				}				
				memoryCard.AT102_ReadAZ(appNo, dataBytes);		
				for (int i = 0; i < 64; i++) {
					if (dataBytes[i] != (byte)0xff) {
						writeLogFromThread("应用区擦除失败");
						isWorking = false;
						return;
					}
				}
				writeLogFromThread("应用区擦除成功");
				
				writeLogFromThread("写应用区" + appNo + ":");
				byte[] tmp = genRandom(64);
				tmp[0] = (byte)0xff;
				if (memoryCard.AT102_WriteAZ(appNo, tmp) != 0) {
					writeLogFromThread("失败");
					isWorking = false;
					return;
				}
				memoryCard.AT102_ReadAZ(appNo, dataBytes);	
				writeLogFromThread("数据:" + bytesToHexString(dataBytes, 0, 64));
				if (checkData(dataBytes, 0, tmp, 0, 64))
					writeLogFromThread("数据验证成功");
				else {
					writeLogFromThread("数据验证失败");
					isWorking = false;
					return;					
				}
				
				writeLogFromThread("恢复应用区" + appNo + ":");
				if (memoryCard.AT102_EraseApp(appNo, 0, null) != 0) {
					writeLogFromThread("失败");
					isWorking = false;
					return;
				}
				writeLogFromThread("测试完成");
				
				isWorking = false;
				
				return;
			}
		}).start();
	}
	
	public void button54()
	{
		if (isWorking) {
			logview.setText("");
			bluetoothThread.cancel();
			return;
		}
		if (!bluetoothThread.deviceIsConnecting()) {
			logview.append("iMate蓝牙未连接\n");
			return;
		}
		
		if (memoryCard == null) {
			memoryCard = new MemoryCard();
		}
		
		logview.setText("AT1604接口测试...\n");
		logview.append("请插AT88SC1604卡...\n");

		new Thread(new Runnable() {
			@Override
			public void run() {
				isWorking = true;	
				
				if (waitForInsertCard(10) == 0) {
					writeLogFromThread("未插卡");
					isWorking = false;
					return;
				}
				
				byte[] dataBytes = new byte[256];
				
				if (memoryCard.AT1604_OpenAuto() != 0) {
					writeLogFromThread("卡片打开失败");
					isWorking = false;
					return;
				}
				writeLogFromThread("更新测试区");
				byte[] tmp = genRandom(2);
				if (memoryCard.AT1604_WriteMTZ(tmp) != 0) {
					writeLogFromThread("更新测试区失败");
					isWorking = false;
					return;
				}
				memoryCard.AT1604_ReadMTZ(dataBytes);
				if (checkData(dataBytes, 0, tmp, 0, 2))
					writeLogFromThread("数据验证成功");
				else {
					writeLogFromThread("数据验证失败");
					isWorking = false;
					return;					
				}
				
				writeLogFromThread("验证主密码");
				byte[] password = {(byte)0x23, (byte)0x23};
				if (memoryCard.AT1604_ChkCode(password) != 0) {
					writeLogFromThread("密码验证失败");
					isWorking = false;
					return;
				}
				writeLogFromThread("成功");
				
				writeLogFromThread("验证分区1密码:ffff");
				byte[] password2 = {(byte)0xff, (byte)0xff};
				if (memoryCard.AT1604_ChkAreaCode(1, password2) != 0) {
					writeLogFromThread("分区1密码验证失败");
					isWorking = false;
					return;
				}
				writeLogFromThread("成功");
				
				writeLogFromThread("读分区1数据");
				memoryCard.AT1604_ReadAZ(1, 0, 10, dataBytes);
				writeLogFromThread(bytesToHexString(dataBytes, 0, 128));
			
				writeLogFromThread("测试完成");
				
				isWorking = false;
				
				return;
			}
		}).start();
	}
	
	public void button55()
	{
		if (isWorking) {
			logview.setText("");
			bluetoothThread.cancel();
			return;
		}
		if (!bluetoothThread.deviceIsConnecting()) {
			logview.append("iMate蓝牙未连接\n");
			return;
		}
		
		if (memoryCard == null) {
			memoryCard = new MemoryCard();
		}
		
		logview.setText("AT1608接口测试...\n");
		logview.append("请插AT88SC1608卡...\n");

		new Thread(new Runnable() {
			@Override
			public void run() {
				isWorking = true;	
				
				if (waitForInsertCard(10) == 0) {
					writeLogFromThread("未插卡");
					isWorking = false;
					return;
				}
				
				byte[] random = {1,2,3,4,5,6,7,8};
				memoryCard.GenCommKey(6, random);
				
				byte[] dataBytes = new byte[256];
				
				if (memoryCard.AT1608_OpenAuto(dataBytes) != 0) {
					writeLogFromThread("卡片打开失败");
					isWorking = false;
					return;
				}
				writeLogFromThread("复位数据:" + bytesToHexString(dataBytes, 0, 2));
				
				if (memoryCard.AT1608_ReadFuse(dataBytes) != 0) {
					writeLogFromThread("读熔丝状态失败");
					isWorking = false;
					return;
				}
				int fuseStatus = (int)dataBytes[0];
				writeLogFromThread("熔丝状态:" + fuseStatus);
				
				
				writeLogFromThread("安全验证主密码：46A97001BC54794B");
				byte[] password = {(byte)0x46, (byte)0xA9, (byte)0x70,(byte)0x01, (byte)0xBC, (byte)0x54, (byte)0x79, (byte)0x4B};
				if (memoryCard.AT1608_ChkCodeEx(7, password) != 0) {
					writeLogFromThread("密码验证失败");
					isWorking = false;
					return;
				}
				
				
				writeLogFromThread("验证主密码：343434");
				byte[] password1 = {(byte)0x34, (byte)0x34, (byte)0x34};
				if (memoryCard.AT1608_ChkCode(7, password1) != 0) {
					writeLogFromThread("密码验证失败");
					isWorking = false;
					return;
				}
				writeLogFromThread("成功");
				
				writeLogFromThread("写认证密钥：1313131313131313");
				byte[] key = {(byte)0x13, (byte)0x13, (byte)0x13, (byte)0x13, (byte)0x13, (byte)0x13, (byte)0x13, (byte)0x13};
				if (memoryCard.AT1608_Write(1,0x30,8,key) != 0) {
					writeLogFromThread("写认证密钥失败");
					isWorking = false;
					return;
				}
				writeLogFromThread("成功");
				
				
				byte[] key2 = {(byte)0x62, (byte)0x62, (byte)0xd7, (byte)0x42, (byte)0xf0, (byte)0x67, (byte)0x2d, (byte)0x73, (byte)0x51, (byte)0x39, (byte)0x1e, (byte)0x49, (byte)0x8a, (byte)0x39, (byte)0x6d, (byte)0x0e};
				
				writeLogFromThread("安全认证");
				if (memoryCard.AT1608_AuthEx(key2) != 0) {
					writeLogFromThread("安全认证失败");
					isWorking = false;
					return;
				}
				writeLogFromThread("成功");
				
				
				writeLogFromThread("写制卡商代码");
				byte[] tmp = genRandom(4);
				if (memoryCard.AT1608_Write(1,0x0c,4,tmp) != 0) {
					writeLogFromThread("写制卡商代码失败");
					isWorking = false;
					return;
				}
				if (memoryCard.AT1608_Read(1, 0, 128, dataBytes) != 0) {
					writeLogFromThread("读设置区失败");
					isWorking = false;
					return;
				}
				if (checkData(dataBytes, 12, tmp, 0, 4))
					writeLogFromThread("数据验证成功");
				else {
					writeLogFromThread("数据验证失败");
					isWorking = false;
					return;					
				}
				
				writeLogFromThread("设置区：" + bytesToHexString(dataBytes, 0, 128));
			
				writeLogFromThread("测试完成");
				
				isWorking = false;
				
				return;
			}
		}).start();
	}
	public void button56()
	{
		if (isWorking) {
			logview.setText("");
			bluetoothThread.cancel();
			return;
		}
		if (!bluetoothThread.deviceIsConnecting()) {
			logview.append("iMate蓝牙未连接\n");
			return;
		}
		
		if (memoryCard == null) {
			memoryCard = new MemoryCard();
		}
		
		logview.setText("AT24Cxx接口测试...\n");
		logview.append("请插AT24Cxx卡...\n");

		new Thread(new Runnable() {
			@Override
			public void run() {
				isWorking = true;	
				
				if (waitForInsertCard(10) == 0) {
					writeLogFromThread("未插卡");
					isWorking = false;
					return;
				}
				if (memoryCard.AT24Cxx_OpenAuto() != 0) {
					writeLogFromThread("卡片打开失败");
					isWorking = false;
					return;
				}
						
				writeLogFromThread("读卡数据");
				byte[] dataBytes = new byte[256];
				if (memoryCard.AT24Cxx_Read(0, 256, dataBytes) != 0) {
					writeLogFromThread("读卡数据失败");
					isWorking = false;
					return;
				}
				writeLogFromThread("数据：" + bytesToHexString(dataBytes, 0, 256));
								
				writeLogFromThread("写卡测试");
				byte[] tmp = genRandom(256);
				
				if (memoryCard.AT24Cxx_Write(0, 256, tmp) != 0) {
					writeLogFromThread("写卡数据失败");
					isWorking = false;
					return;
				}
				
				if (memoryCard.AT24Cxx_Read(0, 256, dataBytes) != 0) {
					writeLogFromThread("读卡数据失败");
					isWorking = false;
					return;
				}
				
				if (checkData(dataBytes, 0, tmp, 0, 256))
					writeLogFromThread("数据验证成功");
				else {
					writeLogFromThread("数据验证失败");
					isWorking = false;
					return;					
				}				
				writeLogFromThread("数据：" + bytesToHexString(dataBytes, 0, 256));
				
				writeLogFromThread("恢复数据");
				for (int i = 0; i < 256; i++)
					dataBytes[i] = (byte)0xff;
				
				if (memoryCard.AT24Cxx_Write(0, 256, dataBytes) != 0) {
					writeLogFromThread("恢复数据失败");
					isWorking = false;
					return;
				}
				writeLogFromThread("成功");

				writeLogFromThread("测试完成");
				
				isWorking = false;
				
				return;
			}
		}).start();
	}
	
	public String bytesToHexString(byte[] bytesData, int offset, int length)
	{
		String string = "";
		
		for (int i = 0; i < length; i++)
			string += Integer.toHexString((bytesData[i + offset]&0x000000ff)|0xffffff00).substring(6);
		return string;
	}
	
	public boolean checkData(byte[] data1, int offset1, byte[] data2, int offset2, int length)
	{
		for (int i = 0; i < length; i++)
			if (data1[i+offset1] != data2[i+offset2])
				return false;
		return true;
	}
	
	public byte[] genRandom(int length)
	{
		byte[] random = new byte[length];
		
		for (int i = 0 ;i < length; i++)
			random[i] = (byte)(Math.random()*256);
		return random;
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.e("测试activity生命周期", "oncreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		logViewAppendHandler = new LogViewAppendHandler();
		imageChangeHandler= new ImageChangeHandler();
		doPrint = new DoPrint(this);
		pinpad = new Pinpad();
		mifCard = new MifCard();
		fingerprint = new Fingerprint();
		hxRecorder = new HxRecorder();
		
		//备份 asset文件夹
		CopyAssets("wltlib",wltlibDirectory);
		
	
		//init  bluetooth
    	if (!threadStarted) {
			BluetoothAdapter bluetoothAdapter = BluetoothAdapter
					.getDefaultAdapter();
			bluetoothThread = new BluetoothThread(bluetoothAdapter);
			bluetoothThread.start();
			threadStarted = true;
		}
    	
		//界面元素
		// 1. 创建一级条目
		List<Map<String,String>>groups = new ArrayList<Map<String,String>>();
		// 创建一级条目的名称和数量,在这里写组的标题，对应写buttonXX函数，和响应点击事件
		String groupTitle[] = new String[]{"通用测试","指纹测试","密码键盘测试","PBOC测试","射频卡测试","逻辑加密卡测试","后台录音"};
		
		// 二级条目的名称
		String childTitle[][] = new String[][]
				{
				{"检测蓝牙连接","查看固件版本、电量、终端序列号","读二代证","刷卡","PSAM卡测试","蓝牙打印测试","重新连接蓝牙打印机","等待事件","扫条码","拍照"},
				{"iMate指纹模块测试(JSABC)","iMate指纹模块测试(Shengteng)","iMate指纹模块测试(ZhongZheng)"},
				{"KMY密码键盘测试","XYD密码键盘测试","test usb fingger"},
				{"Pboc读取IC卡信息","Pboc交易定制接口","创建PBOC应用演示"},
				{"Mifware one卡测试","Pboc读取IC卡信息(射频)","Pboc交易定制接口(射频)"},
				{"卡类型检测","SLE4442","SLE4428","AT88SC102","AT88SC1604","AT88SC1608", "AT24Cxx"},
				{"开始后台录音", "停止后台录音", "播放录音", "停止播放录音"},
				};

		//将group title 加入 groups
		for(int i = 0; i < groupTitle.length; i++)
		{
			Map<String,String> group=new HashMap<String,String>();
			group.put("group", groupTitle[i]);
			groups.add(group);
		}	
		//==================================================
		//2. 创建一级条目下的二级条目 (通用测试组)
		//在二级条目下创建多个项目
		List<List<Map<String, String>>> childs = new ArrayList<List<Map<String, String>>>();
		for(int i=0; i < groupTitle.length ; i++)
		{
			List<Map<String,String>> childTitleList=new ArrayList<Map<String,String>>();
			for(int j = 0; j < childTitle[i].length; j++)
			{
				Map<String,String> child=new HashMap<String,String>();
				child.put("child", childTitle[i][j]);
				childTitleList.add(child);
			}
			childs.add(childTitleList);
		}
		
		/**
		 * 第四步：
         * 使用SimpleExpandableListAdapter显示ExpandableListView
         * 参数1.上下文对象Context
         * 参数2.一级条目目录集合
         * 参数3.一级条目对应的布局文件
         * 参数4.fromto，就是map中的key，指定要显示的对象
         * 参数5.与参数4对应，指定要显示在groups中的id
         * 参数6.二级条目目录集合
         * 参数7.二级条目对应的布局文件
         * 参数8.fromto，就是map中的key，指定要显示的对象
         * 参数9.与参数8对应，指定要显示在childs中的id
         */
        SimpleExpandableListAdapter adapter = new SimpleExpandableListAdapter(
                this, groups, R.layout.groups, new String[] { "group" },
                new int[] { R.id.group }, childs, R.layout.childs,
                new String[] { "child" }, new int[] { R.id.child });
        setListAdapter(adapter);
        
        //展开所有组
        int groupCount = adapter.getGroupCount();
        for (int i=0; i<groupCount; i++) 
        {
            getExpandableListView().expandGroup(i);
        }
        //	界面元素 结束
	}
	
	// 从线程写log到logview的方法。
	private void writeLogFromThread(String logString)
	{
		Message message = new Message();
		message.obj = logString;
		logViewAppendHandler.sendMessage(message);
	}
	
	@SuppressLint("HandlerLeak")
	class  LogViewAppendHandler extends Handler {
		@Override
	    public void handleMessage(Message message) {
			super.handleMessage(message);
			logview.append((String)message.obj + "\n");
	    }
	}
	
	
	// 从线程更新图片到imageview的方法。
	private void writeViewFromThread(Object bm)
	{
		Message message = new Message();
	
		message.obj = bm;
		
		imageChangeHandler.sendMessage(message);
	}
	
	@SuppressLint("HandlerLeak")
	class  ImageChangeHandler extends Handler 
	{
		@Override
	    public void handleMessage(Message message) {
			super.handleMessage(message);
			if(message.obj instanceof Bitmap)
			{
				image.setImageBitmap((Bitmap)message.obj);
			}
			if(message.obj instanceof WebView)
			{
				((WebView)message.obj).setVisibility(View.GONE);	
			}
			if(message.obj instanceof TextView)
			{
				((TextView)message.obj).setMinHeight(1000);
			}    
		}
	}
	
	protected void onStart() 
	{
		Log.e("测试activity生命周期", "onstart");
		super.onStart();
		ActionBar actionBar = this.getActionBar();
		actionBar.setDisplayShowHomeEnabled(true);
		actionBar.setHomeButtonEnabled(false);
		actionBar.setTitle(" iMate POC 测试");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		//menu.add(1,2,0,"退出程序");// 组id ，item id ，显示顺序 ，标题	
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if(id == R.id.action_help)
		{
			Intent intent = new Intent();
		    intent.setClass(MainActivity.this, HelpActivity.class);
		    startActivity(intent);
		    overridePendingTransition(R.anim.in_from_right,  R.anim.out_to_left);//切换动画
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		Log.e("测试activity生命周期", "onresume");

		// App进入前台，建立蓝牙连接
		if (bluetoothThread != null)
			bluetoothThread.resumeThread();
		/*
		if (!threadStarted) {
			BluetoothAdapter bluetoothAdapter = BluetoothAdapter
					.getDefaultAdapter();
			bluetoothThread = new BluetoothThread(bluetoothAdapter);
			bluetoothThread.start();
			threadStarted = true;
			System.out.println(bluetoothThread);
		}
		*/
	}

	@Override
	public void onPause() {
		Log.e("测试activity生命周期", "onpause");
		super.onPause();
		// App进入后台，关闭蓝牙连接，释放资源
		if (bluetoothThread != null)
			bluetoothThread.pauseThread();
		/*
		if (threadStarted) {
			bluetoothThread.exitThread();
			bluetoothThread = null;
			threadStarted = false;
		}
		*/
	}
	
	@Override
	public void onDestroy() {
		Log.e("测试activity生命周期", "onDestroy");
		super.onDestroy();
		// App退出后，关闭蓝牙连接，释放资源
		if (bluetoothThread != null)
			bluetoothThread.pauseThread();
		//关闭蓝牙打印机连接
		if(doPrint != null)
			doPrint.stopPrinter();
		/*
		if (threadStarted) {
			bluetoothThread.exitThread();
			bluetoothThread = null;
			threadStarted = false;
		}
		*/
	}
	
	public byte[] mifInitMoney(long balence)
	{

		byte[] sTmp = new byte[16];

		sTmp[0] = (byte)(balence & 0xff);
		sTmp[1] = (byte)((balence>>8) & 0xff);
		sTmp[2] = (byte)((balence>>16) & 0xff);
		sTmp[3] = (byte)((balence>>24) & 0xff);
		sTmp[4] = (byte)~sTmp[0];
		sTmp[5] = (byte)~sTmp[1];
		sTmp[6] = (byte)~sTmp[2];
		sTmp[7] = (byte)~sTmp[3];
		sTmp[8] = sTmp[0];
		sTmp[9] = sTmp[1];
		sTmp[10] = sTmp[2];
		sTmp[11] = sTmp[3];
		    
		sTmp[12] = 0x01;
		sTmp[13] = (byte)0xfe;
		sTmp[14] = 0x01;
		sTmp[15] = (byte)0xfe;
		
		return sTmp;
	}
	
    // assetDir  -----asset目录
    //dir       -----目标目录
	private void CopyAssets(String assetDir, String dir) 
	{
		 String[] files = null;
		 try 
		 {
			 files = this.getResources().getAssets().list(assetDir);
		 } 
		 catch (IOException e1) 
		 {
			 e1.printStackTrace();
		 }
		 
		 File mWorkingPath = new File(dir);
		 // if this directory does not exists, make one.
		  if (!mWorkingPath.exists()) 
		  {
			  	// 创建目录
			  if (!mWorkingPath.mkdirs()) 
		   		{
				  Log.e("--CopyAssets--", "cannot create directory.");
		   		}
		  }
		  
		  
		  for (int i = 0; i < files.length; i++) 
		  {
			  try 
			  {
				  String fileName = files[i];
				  if (!fileName.contains(".")) {
					  if (0 == assetDir.length()) {
						  CopyAssets(fileName, dir + fileName + "/");
					  } 
					  else 
					  {
						  CopyAssets(assetDir + "/" + fileName, dir + fileName+ "/");
					  }
					  continue;
				  }
			  
				  File outFile = new File(mWorkingPath, fileName);
				  if (outFile.exists())
					  outFile.delete();
				  InputStream in = null;
				  
				  if (0 != assetDir.length())
					  in = getAssets().open(assetDir + "/" + fileName);
				  else
					  in = getAssets().open(fileName);
				  OutputStream out = new FileOutputStream(outFile);
			  
				  // Transfer bytes from in to out
				  byte[] buf = new byte[1024];
				  int len;
				  while ((len = in.read(buf)) > 0) 
				  {
					  out.write(buf, 0, len);
				  }
				  
				  in.close();
				  out.close();
			  } 
			  catch (FileNotFoundException e)
			  {
				  e.printStackTrace();
			  } 
			  catch (IOException e)
			  {
				  e.printStackTrace();
			  }
		  }
	}
	
	//取消外设设备的等待时间
	public void Peripheralcancel()
	{
		//取消蓝牙打印等待时间、密码键盘等待时间（并且关闭）、指纹仪的等待时间（并且下电）
		doPrint.cancel();
		pinpad.cancel();
		fingerprint.cancel();
		//取消蓝牙的耗时操作（IC，磁条卡，身份证）
		bluetoothThread.cancel();
	}
	
	/**
     * 当二级条目被点击时响应
     */
    @Override
    public boolean onChildClick(ExpandableListView parent, View v,
            int groupPosition, int childPosition, long id) {
    	
    	//System.out.println("group:"+groupPosition+",child:"+childPosition+",id:"+id);
    	int whichclick = groupPosition*10+childPosition;
    	
    	//弹出信息框,设置对话框的格式

    		inflater = getLayoutInflater();
    		layout = inflater.inflate(R.layout.dialog, (ViewGroup)findViewById(R.id.dialog));
    		
    		logview= (EditText) layout.findViewById(R.id.tvname);
    		
    		logview.setMinHeight(1000);
    		logview.setMinWidth(2000);
    		logview.setTextSize(18.0f);
    		logview.setMovementMethod(ScrollingMovementMethod.getInstance());
    		
    		webView = (WebView) layout.findViewById(R.id.webview);
    		WebSettings settings = webView.getSettings();
    		webView.setHorizontalScrollBarEnabled(false);//滚动条水平不显示
    		webView.setVerticalScrollBarEnabled(false); //滚动条垂直不显示 
    		webView.setVisibility(View.GONE);
    		settings.setLayoutAlgorithm(LayoutAlgorithm.SINGLE_COLUMN);//图片自动适应。
    		
    		image=(ImageView) layout.findViewById(R.id.verify);
    		
    		//读取res文件夹的图片
    		//Bitmap  bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.g1); 
    		//image.setImageBitmap(bitmap);
    			
    		new AlertDialog.Builder(this).setTitle("运行日志").setView(layout)
    		.setNegativeButton("返回测试",new DialogInterface.OnClickListener(){
    		public void onClick(DialogInterface dialog, int id) { 
    			
    			Peripheralcancel();
				dialog.dismiss();     
				} 
    		})
    		.setIcon(android.R.drawable.ic_dialog_info)
    		.setCancelable(false)
    		.show();

    			
    	switch(whichclick)
    	{
    	//组id*10+子id,设计共有40个菜单，空的是备用的。
    	//每个分支调用函数，并把log显示到弹出的页面或者消息框
    	case 0:
			button00();
			break;
    	case 1:
			button01();
			break;
    	case 2:
			button02();
			break;
    	case 3:
			button03();
			break;
    	case 4:
			button04();
			break;	
    	case 5:
			button05();
			break;	
    	case 6:
			button06();
			break;
    	case 7:
			button07();
			break;
    	case 8:
			button08();
			break;
    	case 9:
			button09();
			break;
				
    	case 10:
			button10();
	        break;    	     
    	case 11:
    			button11();
    			break;
    	case 12:
			button12();
			break;
    	case 13:
			button13();
			break;
    			
    	case 20:
			button20();
			break;
    	case 21:
			button21();
			break;
		
    	case 22:
			button22();
			break;
			/*
    	case 23:
			button23();
			break;
    	*/	
    	case 30:
			button30();
			break;
    	case 31:
			button31();
			break;
    	case 32:
			button32();
			break;
    	case 33:
			button33();
			break;
    	
    	case 40:
			button40();
			break;
    	case 41:
			button41();
			break;
    	case 42:
			button42();
			break;
			
    	case 50:
			button50();
			break;
    	case 51:
			button51();
			break;
    	case 52:
			button52();
			break;
    	case 53:
			button53();
			break;
    	case 54:
			button54();
			break;
    	case 55:
			button55();
			break;
    	case 56:
			button56();
			break;
			
    	case 60:
			button60();
			break;
    	case 61:
			button61();
			break;
    	case 62:
			button62();
			break;
    	case 63:
			button63();
			break;
    	}
    	
        return super.onChildClick(parent, v, groupPosition, childPosition, id);
    }

    
}
