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

//�������
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

	//������������  
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
			logview.append("�����Ѿ����ӳɹ�\n");
		}
		else {
			logview.append("iMate����δ����\n");
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
			logview.append("iMate�̼��汾��" + bluetoothThread.deviceVersion() + "\n");
			String batteryLeve = bluetoothThread.getBatteryLeve();
			if (batteryLeve != null)
				logview.append("iMate��ص�����%" + bluetoothThread.getBatteryLeve()+ "\n");
			String SerialNo = bluetoothThread.deviceSerialNumber();
			if(SerialNo!=null)
				logview.append("�ն����кţ�" + SerialNo + "\n");
			else
				logview.append("���豸��֧�ֲ鿴�ն����кŵĹ��� ��");
		}
		else {
			logview.append("iMate����δ����\n");
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
			logview.append("iMate����δ����\n");
			return;
		}
		
		logview.setMinHeight(400);
		logview.setMinWidth(2000);
		logview.setText("����úö������֤...\n");
		
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
				
				writeViewFromThread(webView);//ͨ���߳�����ͼƬ��ʧ����
				writeViewFromThread(logview);//��logview�Ĵ�С���û���
				
				switch (retCode) {
				case 0:
					message = "������֤�ɹ�:\n\n" + "����:" + idInformationDataData.getNameString() + "\n"
										+ "�Ա�:" + idInformationDataData.getSexString() + "\n"
										+ "����:" + idInformationDataData.getNationString() + "\n"
										+ "����:" + idInformationDataData.getBirthdayYearString() + "��" + idInformationDataData.getBirthdayMonthString() + "��" + idInformationDataData.getBirthdayDayString() + "��" + "\n"
										+ "סַ:" + idInformationDataData.getAddressString() + "\n"
										+ "��ݺ���:" + idInformationDataData.getIdNumberString() + "\n"
										+ "ǩ������:" + idInformationDataData.getIssuerString() + "\n"
										+ "��Ч����:" + idInformationDataData.getValidDateString();						
					break;
				case 1:
					message = "ͨѶ��ʱ";						
					break;
				case 9:
					message = "�����豸δ����";					
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
						message = "��Ƭ����ɹ�";
						Bitmap bm = BitmapFactory.decodeFile(wltlibDirectory+File.separator+"zp.bmp");
						writeViewFromThread(bm);
						break;							
					case 1:
						message = "��Ƭ�����ʼ��ʧ�ܣ���Ҫ��鴫���wltlibDirectory�Լ�base.dat�ļ�";
						break;
					case 2:
						message = "��Ȩ�ļ�license.lic����";
						break;
					case 3:
						message = "��Ƭ����ʧ�ܣ���������";
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
			logview.append("iMate����δ����\n");
			return;
		}
		
		logview.setMinHeight(400);
		logview.setMinWidth(2000);
		logview.setText("��ˢ������...\n");	
		
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
				
				writeViewFromThread(webView);//ͨ���߳�����ͼƬ��ʧ����
				writeViewFromThread(logview);
				
				switch (retCode) {
				case 0:
					message = "ˢ���ɹ�:\n\n" + "����:" + cardData.getCardNoString() + "\n"
							+ "���ŵ�����:" + cardData.getTrack2String() + "\n"
							+ "���ŵ�����:" + cardData.getTrack3String();			
					break;
				case 1:
					message = "ͨѶ��ʱ";						
					break;
				case 9:
					message = "�����豸δ����";						
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
			logview.append("iMate����δ����\n");
			return;
		}
		
		logview.setMinHeight(400);
		logview.setMinWidth(2000);
		logview.setText("���SAM��-1...\n");
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				isWorking = true;
				ApduExchangeData apduData = new ApduExchangeData();
				apduData.reset(); 
				apduData.setSlot(ApduExchangeData.SAM1_SLOT); //���ÿ����ţ���һSAM���� 
				
				int retCode = bluetoothThread.pbocReset(apduData, 0);
				
				String message;
				switch (retCode) {
				case 0:
					message = "SAM1����λ�ɹ�";			
					break;
				case 1:
					message = "ͨѶ��ʱ";						
					break;
				case 9:
					message = "�����豸δ����";						
					break;
				default:
					message = apduData.getErrorString() + apduData.getSlot() + "," + ApduExchangeData.SAM1_SLOT;	
					message += "\n\n�����Ƿ����PSAM��";
					break;
				}
				//apduData.getResetDataBytes();
				writeLogFromThread(message);
				if (retCode != 0) {
					isWorking = false;
					return;
				}
				
				//apdu��ȡ�����
				apduData.reset(); 
				apduData.setSlot(ApduExchangeData.SAM1_SLOT); //���ÿ����ţ���һSAM����  
				apduData.setCardType(ApduExchangeData.NORMAL_CARD_TYPE); //���ÿ�����
				
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
					message = "SAM��ȡ������ɹ�\n״̬λ��";
					for (int m=0; m<status.length; m++) {
						message += Integer.toHexString((status[m]&0x000000ff)|0xffffff00).substring(6);
					}
					if (outBytes != null) {			
						message += "\n�������";
						for (int m=0; m<outBytes.length; m++) {
							message += Integer.toHexString((outBytes[m]&0x000000ff)|0xffffff00).substring(6);
						}
					}
					break;
				case 1:
					message = "ͨѶ��ʱ";						
					break;
				case 9:
					message = "�����豸δ����";						
					break;
				default:
					//message = apduData.getErrorString();
					message = "��������";
					break;
				}
				writeLogFromThread(message);
				
				isWorking = false;
			}
		}).start();
	}
	

	
	public void button05()
	{
		logview.setText("��ӡ����...\n");
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				writeLogFromThread(""+doPrint.bluePrint("���Դ�ӡ��\n======================================\n�̼�:�����������޹�˾\n��Ʒ: iMate���ڰ���\n�۸�:��5000.00\n" +
						"======================================\n�ͻ�ǩ��:\n"));
					
			}
		}).start();
	}
	
	public void button06()
	{
		logview.setText("���´�����bluePrinter\n");
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				writeLogFromThread("�����"+doPrint.reOpenPrinter());
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
			logview.append("iMate����δ����\n");
			return;
		}
		
		logview.setText("IC�������ȴ��¼�\n");
		
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
					//ͨѶ��ʱ��������δ����
					Log.e("zbh",""+e.getMessage());
					writeLogFromThread(e.getMessage());
				}
				
				writeLogFromThread("��⵽�¼�:"+ret[0]);
				//�ж��¼�
				if(ret[0]==0x01)
				{
					Log.e("zbh", "��⵽ˢ���¼���");
					String retString = "";
					for (int m=1; m<ret.length; m++) {
						retString += ret[m]-48;
					}
					//ȡ����
					writeLogFromThread("���ţ�"+retString.substring(0,16));
				}
				else if(ret[0]==0x02)
				{
					Log.e("zbh", "��⵽IC���¼���");
					IcCardData icCardData = new IcCardData();
					String message;
					int retCode = bluetoothThread.readIcCard(icCardData, 10);
					switch (retCode) {
					case 0:
						message = "����:" + icCardData.getCardNoString() + "\n";					
						break;
					case 1:
						message = "ͨѶ��ʱ";						
						break;
					case 9:
						message = "�����豸δ����";						
						break;
					default:
						message = icCardData.getErrorString();												
						break;
					}
					writeLogFromThread(message);
					
				}
				else
				{
					//��Ƶ�¼�
				}
				isWorking = false;
			}
		}).start();
	}
	
	public void button08()
	{
		logview.setText("ɨ�������");
		HxBarcode hxBarcode = new HxBarcode();
		hxBarcode.scan(MainActivity.this, 501);
	}
	
	public void button09()
	{
		logview.setText("���ղ���");
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
				logview.append("\nɨ������\n"+scanResult);
			}
			break;
		case 601:
			String scanResult = data.getStringExtra("data");
			logview.append("\n��ƬURI��\n"+scanResult);
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
			logview.append("iMate����δ����\n");
			return;
		}
		
		
		logview.setText("iMateָ��ģ�����(JSABC)\n");

		new Thread(new Runnable() {
			@Override
			public void run() {
				isWorking = true;

				// ����ָ��ģ���ͺ�
				fingerprint.fingerprintSetModel(Fingerprint.FINGERPRINT_MODEL_JSABC);
				
				// ��ָ��ģ���Դ
				try {
					fingerprint.powerOn();
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());
					isWorking = false;
					return;
				}
				
				writeLogFromThread("��ָ��ģ���Դ�ɹ�");

				writeLogFromThread("���汾��...");
				String versionString;
				try {
					versionString = fingerprint.getVersion();
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());
					isWorking = false;
					return;
				}
				writeLogFromThread("ָ��ģ��汾:" + versionString);
				
				writeLogFromThread("��ȡָ������ֵ...�밴��ָ");
				String fingerprintFeatureString;
				try {
					fingerprintFeatureString = fingerprint.takeFingerprintFeature();
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());
					isWorking = false;
					return;
				}
				writeLogFromThread("ָ������ֵ:" + fingerprintFeatureString);

				try {
					fingerprint.powerOff();
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());
					isWorking = false;
					return;
				}
				writeLogFromThread("ָ��ģ���µ�ɹ�");
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
			logview.append("iMate����δ����\n");
			return;
		}
		
		
		logview.setText("iMateָ��ģ�����(Shengteng)\n");

		new Thread(new Runnable() {
			@Override
			public void run() {
				isWorking = true;	
				// ����ָ��ģ���ͺ�
				fingerprint.fingerprintSetModel(Fingerprint.FINGERPRINT_MODEL_SHENGTENG);
				
				// ��ָ��ģ���Դ
				try {
					fingerprint.powerOn();
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());
					isWorking = false;
					return;
				}
				
				writeLogFromThread("��ָ��ģ���Դ�ɹ�");

				writeLogFromThread("���汾��...");
				String versionString;
				try {
					versionString = fingerprint.getVersion();
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());
					isWorking = false;
					return;
				}
				writeLogFromThread("ָ��ģ��汾:" + versionString);
				
				writeLogFromThread("��ȡָ������ֵ...�밴��ָ");
				String fingerprintFeatureString;
				try {
					fingerprintFeatureString = fingerprint.takeFingerprintFeature();
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());
					isWorking = false;
					return;
				}
				writeLogFromThread("ָ������ֵ:" + fingerprintFeatureString);

				try {
					fingerprint.powerOff();
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());
					isWorking = false;
					return;
				}
				writeLogFromThread("ָ��ģ���µ�ɹ�");
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
			logview.append("iMate����δ����\n");
			return;
		}
		
		
		logview.setText("iMateָ��ģ�����(����)\n");

		new Thread(new Runnable() {
			@Override
			public void run() {
				isWorking = true;
				// ����ָ��ģ���ͺ�
				fingerprint.fingerprintSetModel(Fingerprint.FINGERPRINT_MODEL_ZHONGZHENG);
				
				// ��ָ��ģ���Դ
				try {
					fingerprint.powerOn();
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());
					isWorking = false;
					return;
				}
				
				writeLogFromThread("��ָ��ģ���Դ�ɹ�");

				writeLogFromThread("��ȡģ��������...");
				String versionString;
				try {
					versionString = fingerprint.getVersion();
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());
					isWorking = false;
					return;
				}
				writeLogFromThread("ָ��ģ��������:" + versionString);
				
				writeLogFromThread("��ȡָ������ֵ...�밴��ָ");
				String fingerprintFeatureString;
				try {
					fingerprintFeatureString = fingerprint.takeFingerprintFeature();
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());
					isWorking = false;
					return;
				}
				writeLogFromThread("ָ������ֵ:" + fingerprintFeatureString);

				try {
					fingerprint.powerOff();
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());
					isWorking = false;
					return;
				}
				writeLogFromThread("ָ��ģ���µ�ɹ�");
				isWorking = false;
			}
		}).start();
	}
	
	
	//����ָ���ǵǼ�ָ������ģ��
	public void button13()
	{
		if (isWorking) {
			logview.setText("");
			pinpad.cancel();
			return;
		}
		if (!bluetoothThread.deviceIsConnecting()) {
			logview.append("iMate����δ����\n");
			return;
		}
		
		
		logview.setText("iMateָ��ģ�����(����)\n");

		new Thread(new Runnable() {
			@Override
			public void run() {
				isWorking = true;
				// ����ָ��ģ���ͺ�
				fingerprint.fingerprintSetModel(Fingerprint.FINGERPRINT_MODEL_ZHONGZHENG);
				
				// ��ָ��ģ���Դ
				try {
					fingerprint.powerOn();
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());
					isWorking = false;
					return;
				}
				
				writeLogFromThread("��ָ��ģ���Դ�ɹ�");
				writeLogFromThread("�Ǽ�3��ָ������ָ��ģ��...�밴3����ָ");
				String fingerprintFeatureString;
				try {
					fingerprintFeatureString = fingerprint.GenerateFingerTemplate();
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());
					isWorking = false;
					return;
				}
				writeLogFromThread("ָ��ģ������:" + fingerprintFeatureString);

				try {
					fingerprint.powerOff();
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());
					isWorking = false;
					return;
				}
				writeLogFromThread("ָ��ģ���µ�ɹ�");
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
			logview.append("iMate����δ����\n");
			return;
		}
		
		logview.setText("������������̲���\n");
		pinpad.setPinpadModel(Pinpad.KMY_MODEL);
		new Thread(new Runnable() {
			@Override
			public void run() {
				isWorking = true;
				
				// ��������̵�Դ
				try {
					pinpad.powerOn();
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());
					isWorking = false;
					return;
				}
				writeLogFromThread("��������̵�Դ�ɹ�");
				
				// �ȴ�1�룬�����������ϵ���ټ�������
				try {
	                Thread.sleep(1000);
	            }
	            catch (InterruptedException e) {
	            }

				writeLogFromThread("\n������̸�λ�Լ�...");
				try {
					pinpad.reset(false);
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());					
					isWorking = false;
					return;
				}
				writeLogFromThread("������̸�λ�Լ�ɹ�");
				
				writeLogFromThread("\n��ȡ���к�...");
				byte[] retbytes;
				try {
					retbytes = pinpad.getSerialNo();
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());
					isWorking = false;
					return;
				}
				writeLogFromThread("Pinpad���кţ�" + bytesToHexString(retbytes, 0, retbytes.length));
				
				writeLogFromThread("\n��������Կ...");
				byte[] masterKey = {0x00, 0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,0x0a,0x0b,0x0c,0x0d,0x0e,0x0f};
				try {
					pinpad.downloadMasterKey(true, 15, masterKey, masterKey.length);
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());
					isWorking = false;
					return;
				}
				writeLogFromThread("��������Կ�ɹ�");
				
				writeLogFromThread("\n���ع�����Կ...");
				byte[] workingKey = {0x12, 0x34,0x56,0x78, (byte)0x90, (byte)0xab,(byte)0xcd,(byte)0xef,0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08};
				try {
					pinpad.downloadWorkingKey(true, 15, 1,  workingKey, workingKey.length);
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());
					isWorking = false;
					return;
				}
				writeLogFromThread("���ع�����Կ�ɹ�");
				
				writeLogFromThread("\n����PinBlock...");
				byte[] pinblock;
				try {
					pinblock = pinpad.inputPinblock(true, false, 15, 1, "1234567890123", 6, 20);
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());
					isWorking = false;
					return;
				}
				writeLogFromThread(bytesToHexString(pinblock, 0, pinblock.length) + "\n����PinBlock�ɹ�");
				
				try {
					pinpad.powerOff();
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());
					isWorking = false;
					return;
				}
				writeLogFromThread("\nPinpad�µ�ɹ�");
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
			logview.append("iMate����δ����\n");
			return;
		}
		
		logview.setText("���Ŵ�������̲���\n");
		pinpad.setPinpadModel(Pinpad.XYD_MODEL);
		new Thread(new Runnable() {
			@Override
			public void run() {
				isWorking = true;
				
				// ��������̵�Դ
				try {
					pinpad.powerOn();
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());
					isWorking = false;
					return;
				}
				writeLogFromThread("��������̵�Դ�ɹ�");
				
				// �ȴ�2�룬�����������ϵ���ټ�������
				try {
	                Thread.sleep(2000);
	            }
	            catch (InterruptedException e) {
	            }

				writeLogFromThread("\n������̸�λ�Լ�...");
				try {
					pinpad.reset(false);
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());
					isWorking = false;
					return;
				}
				writeLogFromThread("������̸�λ�Լ�ɹ�");
				
				writeLogFromThread("\n��������Կ...");
				byte[] masterKey = {0x00, 0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,0x0a,0x0b,0x0c,0x0d,0x0e,0x0f};
				try {
					pinpad.downloadMasterKey(true, 1, masterKey, masterKey.length);
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());
					isWorking = false;
					return;
				}
				writeLogFromThread("��������Կ�ɹ�");
				
				writeLogFromThread("\n���ع�����Կ...");
				byte[] workingKey = {0x12, 0x34,0x56,0x78, (byte)0x90, (byte)0xab,(byte)0xcd,(byte)0xef,0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08};
				try {
					pinpad.downloadWorkingKey(true, 1, 1,  workingKey, workingKey.length);
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());
					isWorking = false;
					return;
				}
				writeLogFromThread("���ع�����Կ�ɹ�");
				
				writeLogFromThread("\n����PinBlock...");
				byte[] pinblock;
				try {
					pinblock = pinpad.inputPinblock(true, true, 1, 1, "1234567890123", 6, 20);
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());
					isWorking = false;
					return;
				}
				writeLogFromThread(bytesToHexString(pinblock, 0, pinblock.length) + "\n����PinBlock�ɹ�");
				
				try {
					pinpad.powerOff();
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());
					isWorking = false;
					return;
				}
				writeLogFromThread("\nPinpad�µ�ɹ�");
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
			logview.append("iMate����δ����\n");
			return;
		}
		
		
		logview.setText("iMateָ��ģ�����(USB ����)\n");

		new Thread(new Runnable() {
			@Override
			public void run() {
				isWorking = true;
				// ����ָ��ģ���ͺ�
				FingerprintZhongZhengForUSB usbFinger = new FingerprintZhongZhengForUSB();
				
				// ��ָ��ģ���Դ
				try {
					usbFinger.powerOn();
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());
					isWorking = false;
					return;
				}
				
				writeLogFromThread("��ָ��ģ���Դ�ɹ�");

				writeLogFromThread("��ȡģ��������...");
				String versionString;
				try {
					versionString = usbFinger.getVersion();
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());
					isWorking = false;
					return;
				}
				writeLogFromThread("ָ��ģ��������:" + versionString);
				
				writeLogFromThread("��ȡָ������ֵ...�밴��ָ");
				String fingerprintFeatureString;
				try {
					fingerprintFeatureString = usbFinger.takeFingerprintFeature();
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());
					isWorking = false;
					return;
				}
				writeLogFromThread("ָ������ֵ:" + fingerprintFeatureString);

				try {
					usbFinger.powerOff();
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());
					isWorking = false;
					return;
				}
				writeLogFromThread("ָ��ģ���µ�ɹ�");
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
            //logview.setText("������̨¼��ʧ��\n");
            return;
        }  
        logview.setText("������̨¼���ɹ�\n");
	}
	
	public void button61()
	{
		if (hxRecorder == null) {
	        logview.setText("����δ����\n");
	        return;
		}
		hxRecorder.stopRecorder();        
        logview.setText("�رպ�̨¼���ɹ�\n");
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
        logview.setText("��������¼���ɹ�\n");
	}
	
	public void button63()
	{
		if (hxRecorder == null) {
	        logview.setText("����δ����\n");
	        return;
		}
		hxRecorder.stopPlay();        
        logview.setText("�رղ���¼���ɹ�\n"); 
	}
		
//	public void button30()
//	{
//		if (isWorking) {
//			logview.setText("");
//			pinpad.cancel();
//			return;
//		}
//		if (!bluetoothThread.deviceIsConnecting()) {
//			logview.setText("����δ����");
//			return;
//		}
//		logview.setText("");
//		logview.append("Pboc���Ĳ���...\n");
//		logview.setMinHeight(400);
//		logview.setMinWidth(2000);
//		logview.setText("�����Pboc IC��...\n");
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
//				writeLogFromThread("---- �ն˳�ʼ�� ----");
//				try {
//					pbocApiDemo.doPbocDemoInit();
//				} catch (Exception e) {
//					writeLogFromThread(e.getMessage());
//					isWorking = false;
//					return;
//				}
//				writeLogFromThread("---- ��ʼ���� ----");
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
//				writeLogFromThread("Pboc���Ĳ������");
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
			logview.append("iMate����δ����\n");
			return;
		}
		
		logview.setMinHeight(400);
		logview.setMinWidth(2000);
		logview.setText("����� POBC IC ��...\n");
		
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
				writeViewFromThread(webView);//ͨ���߳�����ͼƬ��ʧ����
				writeViewFromThread(logview);
				
				switch (retCode) {
				case 0:
					message = "��IC���ɹ�:\n\n" + "����:\t\t\t\t\t" + icCardData.getCardNoString() + "\n"
							+ "���к�:\t\t\t\t" + icCardData.getPanSequenceNoString() + "\n"
							+ "�ֿ�������:\t\t\t" + icCardData.getHolderNameString() + "\n"
							+ "�ֿ���֤������:\t" + icCardData.getHolderIdString() + "\n"
							+ "��Ч��:\t\t\t\t" + icCardData.getExpireDateString() + "\n" 
							+"���ŵ���Ч����:\t" + icCardData.getTrack2String();						
					break;
				case 1:
					message = "ͨѶ��ʱ";						
					break;
				case 9:
					message = "�����豸δ����";						
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
			logview.append("iMate����δ����\n");
			return;
		}
		
		logview.append("Pboc���׽ӿڲ���...\n");
		logview.setMinHeight(400);
		logview.setMinWidth(2000);
		logview.append("�����Pboc IC��...\n");	
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
				writeViewFromThread(webView);//ͨ���߳�����ͼƬ��ʧ����
				writeViewFromThread(logview);//����textview��С��
								
				writeLogFromThread("���ڲ���Pboc���׽ӿ�...");
				int ret = pbocHighApi.iHxPbocHighInitCore("123456789000001", "12345601", "̩Ȼ��ó԰212��401", 156, 156);
				if (0 == ret)
				{	
					writeLogFromThread("���׳�ʼ��...");
					String szDateTime = new String("20140611100000");
					ret = pbocHighApi.iHxPbocHighInitTrans(szDateTime, 1, 0x31, 0, pbocCardData);
					if (ret == 0) 
					{
						writeLogFromThread("��ʼ����\n��ӡ��Ϣ���£�");
						writeLogFromThread("Field55: "+pbocCardData.field55 +"\nPan: " + pbocCardData.pan + "\nPanSeqNo: " + pbocCardData.panSeqNo
							            + "\nTrack2: " + pbocCardData.track2 + "\nExtInfo: " + pbocCardData.extInfo);
					
						// Field55���ͺ�̨����̨���ص����ݴ���szIssuerData��, �����outField55�����ͺ�̨
						// int iRet = pbocHighApi.iHxPbocHighDoTrans(szIssuerData, outField55, outLength);
					
						writeLogFromThread("Pboc���ײ��Գɹ�");
					}
					else
					{	
						writeLogFromThread("Pboc���ײ���ʧ��,����ֵret:"+ ret);
					}
					writeLogFromThread("��ȡTAGֵ");
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
					writeLogFromThread("Pboc���ĳ�ʼ��ʧ��,����ֵret:"+ ret);
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
			logview.append("iMate����δ����\n");
			return;
		}
		
		logview.append("PBOC������Ӧ����ʾ...\n\n");
		logview.setMinHeight(400);
		logview.setMinWidth(2000);
		logview.append("�����һ�ſհ� PBOC ���Կ�...\n");
		
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
					writeLogFromThread(String.format("Issue Card��ʼ��ʧ��:%d"));
					isWorking = false;
					return;
				}
				
				ApduExchangeData apduData = new ApduExchangeData();
				apduData.reset();    
				bluetoothThread.pbocReset(apduData, 20);
				writeViewFromThread(webView);//ͨ���߳�����ͼƬ��ʧ����
				writeViewFromThread(logview);//����textview��С��
				
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
				String retString = "����PBOCӦ�óɹ�";
				try {
					pbocIssue.pbocIssueCard(timeout);
				}catch (Exception e) {
					retString = "����PBOCӦ��ʧ��:" + e.getMessage();
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
			logview.append("iMate����δ����\n");
			return;
		}
		
		logview.append("��ȡPboc����Ϣ����չ��...\n");
		logview.setMinHeight(400);
		logview.setMinWidth(2000);
		logview.append("����� PBOC IC��...\n");	
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
				writeViewFromThread(webView);//ͨ���߳�����ͼƬ��ʧ����
				writeViewFromThread(logview);//����textview��С��
				
				byte[] outData = new byte[1024];
				writeLogFromThread("���ڶ�ȡIC��...\n");
				int ret = pbocHighApi.iHxPbocHighInitCore("123456789000001", "12345601", "̩Ȼ��ó԰212��401", 156, 156);
				if (0 == ret)
				{
					ret = pbocHighApi.iHxPbocHighReadInfoEx(outData, 0);

					if (ret == 0) 
					{
						String str = new String(outData);
						writeLogFromThread("\nPboc ��չ��Ϣ��\n");
						writeLogFromThread(str);
						
						writeLogFromThread("\n��ȡ���");
					}
					else
					{	
						writeLogFromThread("\n��ȡIC��ʧ��");
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
			logview.append("iMate����δ����\n");
			return;
		}
		
		logview.setText("Mifware one������...\n");

		new Thread(new Runnable() {
			@Override
			public void run() {
				isWorking = true;
				byte[] uid;
					
				writeLogFromThread("\n�����M1��...");
				
				//1���ȴ���Ƭ
				try {
					uid = mifCard.waitCard(20);
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());
					isWorking = false;
					return;
				}
				String retString = "Ѱ���ɹ��� UID��";
				for (int m=0; m<uid.length; m++) {
					retString += Integer.toHexString((uid[m]&0x000000ff)|0xffffff00).substring(6);
				}
				writeLogFromThread(retString);
				
				//2����֤����						
				byte[] key = {(byte)0xff,(byte)0xff,(byte)0xff,(byte)0xff,(byte)0xff,(byte)0xff};
				try {
					mifCard.mifareAuth(MifCard.Mifare_KeyA, 1, key);
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());
					isWorking = false;
					return;
				}
				
				writeLogFromThread("����1��֤�ɹ�");
				
				//3����1����0��
				byte[] block;
				try {
					block = mifCard.mifareRead(1*4+0);
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());
					isWorking = false;
					return;
				}
				retString = "1����0������ɹ��� Data��";
				for (int m=0; m<block.length; m++) {
					retString += Integer.toHexString((block[m]&0x000000ff)|0xffffff00).substring(6);
				}
				writeLogFromThread(retString);
				
				//4��д1����0��
				byte[] block2 = {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16};
				try {
					mifCard.mifareWrite(1*4, block2);
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());							
					isWorking = false;
					return;
				}
				retString = "1����0��д���ɹ��� Data��";
				for (int m=0; m<block2.length; m++) {
					retString += Integer.toHexString((block2[m]&0x000000ff)|0xffffff00).substring(6);
				}
				writeLogFromThread(retString);
				
				//5����1����0�飨�Աȣ�
				try {
					block = mifCard.mifareRead(1*4);
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());							
					isWorking = false;
					return;
				}
				retString = "1����0������ɹ��� Data��";
				for (int m=0; m<block.length; m++) {
					retString += Integer.toHexString((block[m]&0x000000ff)|0xffffff00).substring(6);
				}
				writeLogFromThread(retString);
				
				//6����ʼ��Ǯ����1����1�飬100.00Ԫ��
				byte[] moneyBloack = mifInitMoney(10000);
				try {
					mifCard.mifareWrite(1*4+1, moneyBloack);
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());							
					isWorking = false;
					return;
				}
				retString = "1����1��дǮ����ʼ���ɹ��� Data��";
				for (int m=0; m<moneyBloack.length; m++) {
					retString += Integer.toHexString((moneyBloack[m]&0x000000ff)|0xffffff00).substring(6);
				}
				writeLogFromThread(retString);
				
				//7��Ǯ����ֵ(10.00Ԫ��
				try {
					mifCard.mifareInc(1*4+1, 1000);
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());							
					isWorking = false;
					return;
				}
				writeLogFromThread("1����1��Ǯ����ֵ�ɹ�");
				
				
				//8����1����1��Ǯ������֤��
				try {
					block = mifCard.mifareRead(1*4+1);
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());							
					isWorking = false;
					return;
				}
				retString = "1����1������ɹ��� Data��";
				for (int m=0; m<block.length; m++) {
					retString += Integer.toHexString((block[m]&0x000000ff)|0xffffff00).substring(6);
				}
				writeLogFromThread(retString);
				
				//9��Ǯ����ֵ(10.00Ԫ��
				try {
					mifCard.mifareDec(1*4+1, 1000);
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());							
					isWorking = false;
					return;
				}
				writeLogFromThread("1����1��Ǯ����ֵ�ɹ�");
				
				//10����1����Ǯ���飨��֤��
				try {
					block = mifCard.mifareRead(1*4+1);
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());							
					isWorking = false;
					return;
				}
				retString = "1����1������ɹ��� Data��";
				for (int m=0; m<block.length; m++) {
					retString += Integer.toHexString((block[m]&0x000000ff)|0xffffff00).substring(6);
				}
				writeLogFromThread(retString);
				
				//�鿽����1�鿽����2�飩
				try {
					mifCard.mifareCopy(1*4+1, 1*4+2);
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());							
					isWorking = false;
					return;
				}
				writeLogFromThread("1����1�鿽����1����2��ɹ�");
				
				//��1����2��
				try {
					block = mifCard.mifareRead(1*4+2);
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());							
					isWorking = false;
					return;
				}
				retString = "1����2������ɹ��� Data��";
				for (int m=0; m<block.length; m++) {
					retString += Integer.toHexString((block[m]&0x000000ff)|0xffffff00).substring(6);
				}
				writeLogFromThread(retString);
										
				//�ָ���ȫ0״̬						
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
				writeLogFromThread("����1���ݸ�ԭ�ɹ�");						
				writeLogFromThread("\n���Ƴ���Ƭ...");
				
				//�Ƴ���Ƭ
				Boolean removal;
				try {
					removal = mifCard.waitRemoval(10);
				}catch (Exception e) {
					writeLogFromThread(e.getMessage());							
					isWorking = false;
					return;
				}
				if (removal)
					writeLogFromThread("��Ƭ�Ƴ��ɹ�");
				else 
					writeLogFromThread("��Ƭ��δ�Ƴ�");
									
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
			logview.append("iMate����δ����\n");
			return;
		}
		
		logview.setText("Pboc������Ϣ(��Ƶ)...\n");
		logview.append("�����PBOC��Ƶ��...\n");

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
				writeLogFromThread("PBOC����Ϣ:\n" + cardInfoString);									
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
			logview.append("iMate����δ����\n");
			return;
		}
		
		logview.setText("Pboc���׽ӿڲ���(��Ƶ����...\n");
		logview.append("�����PBOC��Ƶ��...\n");	
		
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
				writeViewFromThread(webView);//ͨ���߳�����ͼƬ��ʧ����
				writeViewFromThread(logview);//����textview��С��
				
				writeLogFromThread("���ڲ���Pboc���׽ӿ�...");
				
				pbocHighApi.vHxPbocHighSetCardReaderType(1);//������Ƶ��������
				int ret = pbocHighApi.iHxPbocHighInitCore("123456789000001", "12345601", "̩Ȼ��ó԰212��401", 156, 156);
				if (0 == ret)
				{	
					writeLogFromThread("���׳�ʼ��...");
					String szDateTime = new String("20140611100000");
					ret = pbocHighApi.iHxPbocHighInitTrans(szDateTime, 1, 0x00, 0, pbocCardData);
					if (ret == 0) 
					{
						writeLogFromThread("��ʼ����\n��ӡ��Ϣ���£�");
						writeLogFromThread("Field55: "+pbocCardData.field55 +"\nPan: " + pbocCardData.pan + "\nPanSeqNo: " + pbocCardData.panSeqNo
							            + "\nTrack2: " + pbocCardData.track2 + "\nExtInfo: " + pbocCardData.extInfo);
					
						// Field55���ͺ�̨����̨���ص����ݴ���szIssuerData��, �����outField55�����ͺ�̨
						// int iRet = pbocHighApi.iHxPbocHighDoTrans(szIssuerData, outField55, outLength);
					
						writeLogFromThread("Pboc���ײ��Գɹ�");
					}
					else
					{	
						writeLogFromThread("Pboc���ײ���ʧ��,����ֵret:"+ ret);
					}
					writeLogFromThread("��ȡTAGֵ");
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
					writeLogFromThread("Pboc���ĳ�ʼ��ʧ��,����ֵret:"+ ret);
				}
				isWorking = false;
			}
		}).start();
	}
	
	private int waitForInsertCard(int timeout) {
	    long timeMillis = System.currentTimeMillis() + timeout * 1000L; //����20��ȴ�ʱ��
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
			logview.append("iMate����δ����\n");
			return;
		}
		
		if (memoryCard == null) {
			memoryCard = new MemoryCard();
		}
		
		logview.setText("�����߼����ܿ����ͽӿڲ���...\n");
		logview.append("����߼����ܿ�...\n\n");

		new Thread(new Runnable() {
			@Override
			public void run() {
				isWorking = true;	
				
				if (waitForInsertCard(10) == 0) {
					writeLogFromThread("δ�忨");
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
					writeLogFromThread("������:SLE4442");
					memoryCard.SLE4442_Read(0, 2, dataBytes);
					break;
				case MemoryCard.AT102_TYPE:
					writeLogFromThread("������:AT88SC102");
					memoryCard.AT102_ReadWords(0, 1, dataBytes);
					break;
				case MemoryCard.AT1604_TYPE:
					writeLogFromThread("������:AT88SC1604");
					memoryCard.AT1604_Read(0, 1, dataBytes);
					break;
				case MemoryCard.AT1608_TYPE:
					writeLogFromThread("������:AT88SC1608");
					memoryCard.AT1608_Read(1, 8, 2, dataBytes);
					break;
				case MemoryCard.SLE4428_TYPE:
					writeLogFromThread("������:SLE4428");
					memoryCard.SLE4428_Read(0, 2, dataBytes);
					break;
				case MemoryCard.AT24Cxx_TYPE:
					writeLogFromThread("������:AT24Cxx");
					memoryCard.AT24Cxx_Read(0, 2, dataBytes);					
					break;
				default:
					writeLogFromThread("�޷�ʶ��ô洢������");
					break;					
				}
				writeLogFromThread("���ݣ�" + bytesToHexString(dataBytes, 0, 2));
				writeLogFromThread("�������");
				
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
			logview.append("iMate����δ����\n");
			return;
		}
		
		if (memoryCard == null) {
			memoryCard = new MemoryCard();
		}
		
		logview.setText("SLE4442�ӿڲ���...\n");
		logview.append("���SLE4442��...\n");

		new Thread(new Runnable() {
			@Override
			public void run() {
				isWorking = true;	
								
				if (waitForInsertCard(10) == 0) {
					writeLogFromThread("δ�忨");
					isWorking = false;
					return;
				}
				
				// ESAM������������Կ����Ҫ��openCard֮ǰ���ã�ȫ����Ч
				/*
				byte[] random = {1,2,3,4,5,6,7,8};
				memoryCard.GenCommKey(6, random);
				*/
				
				if (memoryCard.SLE4442_OpenAuto() != 0) {
					writeLogFromThread("��Ƭ��ʧ��");
					isWorking = false;
					return;
				}
				/*
				writeLogFromThread("��ȫ��֤���룺43ae5affb653393f(�������0102030405060708)");
				byte[] passwordEx = {(byte)0x43, (byte)0xae, (byte)0x5a, (byte)0xff, (byte)0xb6, (byte)0x53, (byte)0x39, (byte)0x3f};
				if (memoryCard.SLE4442_ChkCodeEx(passwordEx) != 0) {
					writeLogFromThread("������֤ʧ��");
					isWorking = false;
					return;
				}
				*/
				
				writeLogFromThread("��֤���룺ffffff");
				byte[] password1 = {(byte)0xff, (byte)0xff, (byte)0xff};
				byte[] password2 = {(byte)0xf0, (byte)0xf0, (byte)0xf0};
				if (memoryCard.SLE4442_ChkCode(password1) != 0) {
					writeLogFromThread("������֤ʧ��");
					writeLogFromThread("��֤���룺f0f0f0");
					if (memoryCard.SLE4442_ChkCode(password2) != 0) {
						writeLogFromThread("������֤ʧ��");
						isWorking = false;
						return;
					}
					return;
				}
				
				writeLogFromThread("�������ݣ�0~255");
				byte[] dataBytes = new byte[256];
				memoryCard.SLE4442_Read(0, 256, dataBytes);
				writeLogFromThread(bytesToHexString(dataBytes, 0, 256));
				
				writeLogFromThread("д������, �� 100 ��ʼ, 100���ֽ�");
				byte[] writeData = genRandom(100);				
				
				memoryCard.SLE4442_Write(100, 100, writeData);
				
				memoryCard.SLE4442_Read(0, 256, dataBytes);
				if (checkData(dataBytes, 100, writeData, 0, 100))
					writeLogFromThread("������֤�ɹ�");
				else {
					writeLogFromThread("������֤ʧ��");
					isWorking = false;
					return;
				}				
				writeLogFromThread("���ݣ�" + bytesToHexString(dataBytes, 0, 256));
				
				writeLogFromThread("�������");
				
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
			logview.append("iMate����δ����\n");
			return;
		}
		
		if (memoryCard == null) {
			memoryCard = new MemoryCard();
		}
		
		logview.setText("SLE4428�ӿڲ���...\n");
		logview.append("���SLE4428��...\n");

		new Thread(new Runnable() {
			@Override
			public void run() {
				isWorking = true;
				
				
				if (waitForInsertCard(10) == 0) {
					writeLogFromThread("δ�忨");
					isWorking = false;
					return;
				}
				
				byte[] random = {1,2,3,4,5,6,7,8};
				memoryCard.GenCommKey(6, random);
				
				if (memoryCard.SLE4428_OpenAuto() != 0) {
					writeLogFromThread("��Ƭ��ʧ��");
					isWorking = false;
					return;
				}
				
				writeLogFromThread("��ȫ��֤���룺43ae5affb653393f(�������0102030405060708)");
				byte[] passwordEx = {(byte)0x43, (byte)0xae, (byte)0x5a, (byte)0xff, (byte)0xb6, (byte)0x53, (byte)0x39, (byte)0x3f};
				if (memoryCard.SLE4428_ChkCodeEx(passwordEx) != 0) {
					writeLogFromThread("������֤ʧ��");
					isWorking = false;
					return;
				}
				
				writeLogFromThread("��֤���룺ffff");
				byte[] password1 = {(byte)0xff, (byte)0xff};
				byte[] password2 = {(byte)0xf0, (byte)0xf0};
				if (memoryCard.SLE4428_ChkCode(password1) != 0) {
					writeLogFromThread("������֤ʧ��");
					writeLogFromThread("��֤���룺f0f0");
					if (memoryCard.SLE4428_ChkCode(password2) != 0) {
						writeLogFromThread("������֤ʧ��");
						isWorking = false;
						return;
					}
				}
				writeLogFromThread("�ɹ�");
				
				writeLogFromThread("�������ݣ�0~1023");
				byte[] dataBytes = new byte[1024];
				memoryCard.SLE4428_Read(0, 1024, dataBytes);
				writeLogFromThread("���ݣ�" + bytesToHexString(dataBytes, 0, 1024));
				
				writeLogFromThread("д�����ݣ�100��ʼ, 100���ֽ�");
				byte[] writeData = genRandom(100);
				memoryCard.SLE4428_Write(100, 100, writeData);
				
				memoryCard.SLE4428_Read(0, 256, dataBytes);
				if (checkData(dataBytes, 100, writeData, 0, 100))
					writeLogFromThread("������֤�ɹ�");
				else {
					writeLogFromThread("������֤ʧ��");
					isWorking = false;
					return;
				}
				writeLogFromThread("���ݣ�0~255����" + bytesToHexString(dataBytes, 0, 256));

				writeLogFromThread("�������");
				
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
			logview.append("iMate����δ����\n");
			return;
		}
		
		if (memoryCard == null) {
			memoryCard = new MemoryCard();
		}
		
		logview.setText("AT102�ӿڲ���<Level 1>...\n");
		logview.append("���AT88SC102��...\n");

		new Thread(new Runnable() {
			@Override
			public void run() {
				isWorking = true;	
				
				if (waitForInsertCard(10) == 0) {
					writeLogFromThread("δ�忨");
					isWorking = false;
					return;
				}
				
				if (memoryCard.AT102_OpenAuto() != 0) {
					writeLogFromThread("��Ƭ��ʧ��");
					isWorking = false;
					return;
				}
				writeLogFromThread("��֤���룺f0f0");
				byte[] password1 = {(byte)0xf0, (byte)0xf0};
				byte[] password2 = {(byte)0xff, (byte)0xff};
				if (memoryCard.AT102_ChkCode(password1) != 0) {
					writeLogFromThread("������֤ʧ��");
					writeLogFromThread("��֤���룺ffff");
					if (memoryCard.AT102_ChkCode(password2) != 0) {
						writeLogFromThread("������֤ʧ��");
						isWorking = false;
						return;
					}
				}
				writeLogFromThread("�ɹ�");
			
				byte[] dataBytes = new byte[200];
				writeLogFromThread("��ȫ������:");
				memoryCard.AT102_ReadWords(0, 89, dataBytes);
				writeLogFromThread("���ݣ�" + bytesToHexString(dataBytes, 0, 178));
				
				int appNo = 1;
				writeLogFromThread("��Ӧ����" + appNo + ":");
				memoryCard.AT102_ReadAZ(appNo, dataBytes);
				writeLogFromThread("���ݣ�" + bytesToHexString(dataBytes, 0, 64));
				
				writeLogFromThread("����Ӧ����" + appNo + ":");
				if (memoryCard.AT102_EraseApp(appNo, 0, null) != 0) {
					writeLogFromThread("ʧ��");
					isWorking = false;
					return;
				}				
				memoryCard.AT102_ReadAZ(appNo, dataBytes);		
				for (int i = 0; i < 64; i++) {
					if (dataBytes[i] != (byte)0xff) {
						writeLogFromThread("Ӧ��������ʧ��");
						isWorking = false;
						return;
					}
				}
				writeLogFromThread("Ӧ���������ɹ�");
				
				writeLogFromThread("дӦ����" + appNo + ":");
				byte[] tmp = genRandom(64);
				tmp[0] = (byte)0xff;
				if (memoryCard.AT102_WriteAZ(appNo, tmp) != 0) {
					writeLogFromThread("ʧ��");
					isWorking = false;
					return;
				}
				memoryCard.AT102_ReadAZ(appNo, dataBytes);	
				writeLogFromThread("����:" + bytesToHexString(dataBytes, 0, 64));
				if (checkData(dataBytes, 0, tmp, 0, 64))
					writeLogFromThread("������֤�ɹ�");
				else {
					writeLogFromThread("������֤ʧ��");
					isWorking = false;
					return;					
				}
				
				writeLogFromThread("�ָ�Ӧ����" + appNo + ":");
				if (memoryCard.AT102_EraseApp(appNo, 0, null) != 0) {
					writeLogFromThread("ʧ��");
					isWorking = false;
					return;
				}
				writeLogFromThread("�������");
				
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
			logview.append("iMate����δ����\n");
			return;
		}
		
		if (memoryCard == null) {
			memoryCard = new MemoryCard();
		}
		
		logview.setText("AT1604�ӿڲ���...\n");
		logview.append("���AT88SC1604��...\n");

		new Thread(new Runnable() {
			@Override
			public void run() {
				isWorking = true;	
				
				if (waitForInsertCard(10) == 0) {
					writeLogFromThread("δ�忨");
					isWorking = false;
					return;
				}
				
				byte[] dataBytes = new byte[256];
				
				if (memoryCard.AT1604_OpenAuto() != 0) {
					writeLogFromThread("��Ƭ��ʧ��");
					isWorking = false;
					return;
				}
				writeLogFromThread("���²�����");
				byte[] tmp = genRandom(2);
				if (memoryCard.AT1604_WriteMTZ(tmp) != 0) {
					writeLogFromThread("���²�����ʧ��");
					isWorking = false;
					return;
				}
				memoryCard.AT1604_ReadMTZ(dataBytes);
				if (checkData(dataBytes, 0, tmp, 0, 2))
					writeLogFromThread("������֤�ɹ�");
				else {
					writeLogFromThread("������֤ʧ��");
					isWorking = false;
					return;					
				}
				
				writeLogFromThread("��֤������");
				byte[] password = {(byte)0x23, (byte)0x23};
				if (memoryCard.AT1604_ChkCode(password) != 0) {
					writeLogFromThread("������֤ʧ��");
					isWorking = false;
					return;
				}
				writeLogFromThread("�ɹ�");
				
				writeLogFromThread("��֤����1����:ffff");
				byte[] password2 = {(byte)0xff, (byte)0xff};
				if (memoryCard.AT1604_ChkAreaCode(1, password2) != 0) {
					writeLogFromThread("����1������֤ʧ��");
					isWorking = false;
					return;
				}
				writeLogFromThread("�ɹ�");
				
				writeLogFromThread("������1����");
				memoryCard.AT1604_ReadAZ(1, 0, 10, dataBytes);
				writeLogFromThread(bytesToHexString(dataBytes, 0, 128));
			
				writeLogFromThread("�������");
				
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
			logview.append("iMate����δ����\n");
			return;
		}
		
		if (memoryCard == null) {
			memoryCard = new MemoryCard();
		}
		
		logview.setText("AT1608�ӿڲ���...\n");
		logview.append("���AT88SC1608��...\n");

		new Thread(new Runnable() {
			@Override
			public void run() {
				isWorking = true;	
				
				if (waitForInsertCard(10) == 0) {
					writeLogFromThread("δ�忨");
					isWorking = false;
					return;
				}
				
				byte[] random = {1,2,3,4,5,6,7,8};
				memoryCard.GenCommKey(6, random);
				
				byte[] dataBytes = new byte[256];
				
				if (memoryCard.AT1608_OpenAuto(dataBytes) != 0) {
					writeLogFromThread("��Ƭ��ʧ��");
					isWorking = false;
					return;
				}
				writeLogFromThread("��λ����:" + bytesToHexString(dataBytes, 0, 2));
				
				if (memoryCard.AT1608_ReadFuse(dataBytes) != 0) {
					writeLogFromThread("����˿״̬ʧ��");
					isWorking = false;
					return;
				}
				int fuseStatus = (int)dataBytes[0];
				writeLogFromThread("��˿״̬:" + fuseStatus);
				
				
				writeLogFromThread("��ȫ��֤�����룺46A97001BC54794B");
				byte[] password = {(byte)0x46, (byte)0xA9, (byte)0x70,(byte)0x01, (byte)0xBC, (byte)0x54, (byte)0x79, (byte)0x4B};
				if (memoryCard.AT1608_ChkCodeEx(7, password) != 0) {
					writeLogFromThread("������֤ʧ��");
					isWorking = false;
					return;
				}
				
				
				writeLogFromThread("��֤�����룺343434");
				byte[] password1 = {(byte)0x34, (byte)0x34, (byte)0x34};
				if (memoryCard.AT1608_ChkCode(7, password1) != 0) {
					writeLogFromThread("������֤ʧ��");
					isWorking = false;
					return;
				}
				writeLogFromThread("�ɹ�");
				
				writeLogFromThread("д��֤��Կ��1313131313131313");
				byte[] key = {(byte)0x13, (byte)0x13, (byte)0x13, (byte)0x13, (byte)0x13, (byte)0x13, (byte)0x13, (byte)0x13};
				if (memoryCard.AT1608_Write(1,0x30,8,key) != 0) {
					writeLogFromThread("д��֤��Կʧ��");
					isWorking = false;
					return;
				}
				writeLogFromThread("�ɹ�");
				
				
				byte[] key2 = {(byte)0x62, (byte)0x62, (byte)0xd7, (byte)0x42, (byte)0xf0, (byte)0x67, (byte)0x2d, (byte)0x73, (byte)0x51, (byte)0x39, (byte)0x1e, (byte)0x49, (byte)0x8a, (byte)0x39, (byte)0x6d, (byte)0x0e};
				
				writeLogFromThread("��ȫ��֤");
				if (memoryCard.AT1608_AuthEx(key2) != 0) {
					writeLogFromThread("��ȫ��֤ʧ��");
					isWorking = false;
					return;
				}
				writeLogFromThread("�ɹ�");
				
				
				writeLogFromThread("д�ƿ��̴���");
				byte[] tmp = genRandom(4);
				if (memoryCard.AT1608_Write(1,0x0c,4,tmp) != 0) {
					writeLogFromThread("д�ƿ��̴���ʧ��");
					isWorking = false;
					return;
				}
				if (memoryCard.AT1608_Read(1, 0, 128, dataBytes) != 0) {
					writeLogFromThread("��������ʧ��");
					isWorking = false;
					return;
				}
				if (checkData(dataBytes, 12, tmp, 0, 4))
					writeLogFromThread("������֤�ɹ�");
				else {
					writeLogFromThread("������֤ʧ��");
					isWorking = false;
					return;					
				}
				
				writeLogFromThread("��������" + bytesToHexString(dataBytes, 0, 128));
			
				writeLogFromThread("�������");
				
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
			logview.append("iMate����δ����\n");
			return;
		}
		
		if (memoryCard == null) {
			memoryCard = new MemoryCard();
		}
		
		logview.setText("AT24Cxx�ӿڲ���...\n");
		logview.append("���AT24Cxx��...\n");

		new Thread(new Runnable() {
			@Override
			public void run() {
				isWorking = true;	
				
				if (waitForInsertCard(10) == 0) {
					writeLogFromThread("δ�忨");
					isWorking = false;
					return;
				}
				if (memoryCard.AT24Cxx_OpenAuto() != 0) {
					writeLogFromThread("��Ƭ��ʧ��");
					isWorking = false;
					return;
				}
						
				writeLogFromThread("��������");
				byte[] dataBytes = new byte[256];
				if (memoryCard.AT24Cxx_Read(0, 256, dataBytes) != 0) {
					writeLogFromThread("��������ʧ��");
					isWorking = false;
					return;
				}
				writeLogFromThread("���ݣ�" + bytesToHexString(dataBytes, 0, 256));
								
				writeLogFromThread("д������");
				byte[] tmp = genRandom(256);
				
				if (memoryCard.AT24Cxx_Write(0, 256, tmp) != 0) {
					writeLogFromThread("д������ʧ��");
					isWorking = false;
					return;
				}
				
				if (memoryCard.AT24Cxx_Read(0, 256, dataBytes) != 0) {
					writeLogFromThread("��������ʧ��");
					isWorking = false;
					return;
				}
				
				if (checkData(dataBytes, 0, tmp, 0, 256))
					writeLogFromThread("������֤�ɹ�");
				else {
					writeLogFromThread("������֤ʧ��");
					isWorking = false;
					return;					
				}				
				writeLogFromThread("���ݣ�" + bytesToHexString(dataBytes, 0, 256));
				
				writeLogFromThread("�ָ�����");
				for (int i = 0; i < 256; i++)
					dataBytes[i] = (byte)0xff;
				
				if (memoryCard.AT24Cxx_Write(0, 256, dataBytes) != 0) {
					writeLogFromThread("�ָ�����ʧ��");
					isWorking = false;
					return;
				}
				writeLogFromThread("�ɹ�");

				writeLogFromThread("�������");
				
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
		Log.e("����activity��������", "oncreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		logViewAppendHandler = new LogViewAppendHandler();
		imageChangeHandler= new ImageChangeHandler();
		doPrint = new DoPrint(this);
		pinpad = new Pinpad();
		mifCard = new MifCard();
		fingerprint = new Fingerprint();
		hxRecorder = new HxRecorder();
		
		//���� asset�ļ���
		CopyAssets("wltlib",wltlibDirectory);
		
	
		//init  bluetooth
    	if (!threadStarted) {
			BluetoothAdapter bluetoothAdapter = BluetoothAdapter
					.getDefaultAdapter();
			bluetoothThread = new BluetoothThread(bluetoothAdapter);
			bluetoothThread.start();
			threadStarted = true;
		}
    	
		//����Ԫ��
		// 1. ����һ����Ŀ
		List<Map<String,String>>groups = new ArrayList<Map<String,String>>();
		// ����һ����Ŀ�����ƺ�����,������д��ı��⣬��ӦдbuttonXX����������Ӧ����¼�
		String groupTitle[] = new String[]{"ͨ�ò���","ָ�Ʋ���","������̲���","PBOC����","��Ƶ������","�߼����ܿ�����","��̨¼��"};
		
		// ������Ŀ������
		String childTitle[][] = new String[][]
				{
				{"�����������","�鿴�̼��汾���������ն����к�","������֤","ˢ��","PSAM������","������ӡ����","��������������ӡ��","�ȴ��¼�","ɨ����","����"},
				{"iMateָ��ģ�����(JSABC)","iMateָ��ģ�����(Shengteng)","iMateָ��ģ�����(ZhongZheng)"},
				{"KMY������̲���","XYD������̲���","test usb fingger"},
				{"Pboc��ȡIC����Ϣ","Pboc���׶��ƽӿ�","����PBOCӦ����ʾ"},
				{"Mifware one������","Pboc��ȡIC����Ϣ(��Ƶ)","Pboc���׶��ƽӿ�(��Ƶ)"},
				{"�����ͼ��","SLE4442","SLE4428","AT88SC102","AT88SC1604","AT88SC1608", "AT24Cxx"},
				{"��ʼ��̨¼��", "ֹͣ��̨¼��", "����¼��", "ֹͣ����¼��"},
				};

		//��group title ���� groups
		for(int i = 0; i < groupTitle.length; i++)
		{
			Map<String,String> group=new HashMap<String,String>();
			group.put("group", groupTitle[i]);
			groups.add(group);
		}	
		//==================================================
		//2. ����һ����Ŀ�µĶ�����Ŀ (ͨ�ò�����)
		//�ڶ�����Ŀ�´��������Ŀ
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
		 * ���Ĳ���
         * ʹ��SimpleExpandableListAdapter��ʾExpandableListView
         * ����1.�����Ķ���Context
         * ����2.һ����ĿĿ¼����
         * ����3.һ����Ŀ��Ӧ�Ĳ����ļ�
         * ����4.fromto������map�е�key��ָ��Ҫ��ʾ�Ķ���
         * ����5.�����4��Ӧ��ָ��Ҫ��ʾ��groups�е�id
         * ����6.������ĿĿ¼����
         * ����7.������Ŀ��Ӧ�Ĳ����ļ�
         * ����8.fromto������map�е�key��ָ��Ҫ��ʾ�Ķ���
         * ����9.�����8��Ӧ��ָ��Ҫ��ʾ��childs�е�id
         */
        SimpleExpandableListAdapter adapter = new SimpleExpandableListAdapter(
                this, groups, R.layout.groups, new String[] { "group" },
                new int[] { R.id.group }, childs, R.layout.childs,
                new String[] { "child" }, new int[] { R.id.child });
        setListAdapter(adapter);
        
        //չ��������
        int groupCount = adapter.getGroupCount();
        for (int i=0; i<groupCount; i++) 
        {
            getExpandableListView().expandGroup(i);
        }
        //	����Ԫ�� ����
	}
	
	// ���߳�дlog��logview�ķ�����
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
	
	
	// ���̸߳���ͼƬ��imageview�ķ�����
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
		Log.e("����activity��������", "onstart");
		super.onStart();
		ActionBar actionBar = this.getActionBar();
		actionBar.setDisplayShowHomeEnabled(true);
		actionBar.setHomeButtonEnabled(false);
		actionBar.setTitle(" iMate POC ����");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		//menu.add(1,2,0,"�˳�����");// ��id ��item id ����ʾ˳�� ������	
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
		    overridePendingTransition(R.anim.in_from_right,  R.anim.out_to_left);//�л�����
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		Log.e("����activity��������", "onresume");

		// App����ǰ̨��������������
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
		Log.e("����activity��������", "onpause");
		super.onPause();
		// App�����̨���ر��������ӣ��ͷ���Դ
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
		Log.e("����activity��������", "onDestroy");
		super.onDestroy();
		// App�˳��󣬹ر��������ӣ��ͷ���Դ
		if (bluetoothThread != null)
			bluetoothThread.pauseThread();
		//�ر�������ӡ������
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
	
    // assetDir  -----assetĿ¼
    //dir       -----Ŀ��Ŀ¼
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
			  	// ����Ŀ¼
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
	
	//ȡ�������豸�ĵȴ�ʱ��
	public void Peripheralcancel()
	{
		//ȡ��������ӡ�ȴ�ʱ�䡢������̵ȴ�ʱ�䣨���ҹرգ���ָ���ǵĵȴ�ʱ�䣨�����µ磩
		doPrint.cancel();
		pinpad.cancel();
		fingerprint.cancel();
		//ȡ�������ĺ�ʱ������IC�������������֤��
		bluetoothThread.cancel();
	}
	
	/**
     * ��������Ŀ�����ʱ��Ӧ
     */
    @Override
    public boolean onChildClick(ExpandableListView parent, View v,
            int groupPosition, int childPosition, long id) {
    	
    	//System.out.println("group:"+groupPosition+",child:"+childPosition+",id:"+id);
    	int whichclick = groupPosition*10+childPosition;
    	
    	//������Ϣ��,���öԻ���ĸ�ʽ

    		inflater = getLayoutInflater();
    		layout = inflater.inflate(R.layout.dialog, (ViewGroup)findViewById(R.id.dialog));
    		
    		logview= (EditText) layout.findViewById(R.id.tvname);
    		
    		logview.setMinHeight(1000);
    		logview.setMinWidth(2000);
    		logview.setTextSize(18.0f);
    		logview.setMovementMethod(ScrollingMovementMethod.getInstance());
    		
    		webView = (WebView) layout.findViewById(R.id.webview);
    		WebSettings settings = webView.getSettings();
    		webView.setHorizontalScrollBarEnabled(false);//������ˮƽ����ʾ
    		webView.setVerticalScrollBarEnabled(false); //��������ֱ����ʾ 
    		webView.setVisibility(View.GONE);
    		settings.setLayoutAlgorithm(LayoutAlgorithm.SINGLE_COLUMN);//ͼƬ�Զ���Ӧ��
    		
    		image=(ImageView) layout.findViewById(R.id.verify);
    		
    		//��ȡres�ļ��е�ͼƬ
    		//Bitmap  bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.g1); 
    		//image.setImageBitmap(bitmap);
    			
    		new AlertDialog.Builder(this).setTitle("������־").setView(layout)
    		.setNegativeButton("���ز���",new DialogInterface.OnClickListener(){
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
    	//��id*10+��id,��ƹ���40���˵����յ��Ǳ��õġ�
    	//ÿ����֧���ú���������log��ʾ��������ҳ�������Ϣ��
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
