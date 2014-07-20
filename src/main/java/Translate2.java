import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;

@SuppressWarnings("serial")
public class Translate2 extends HttpServlet {
	private Context androidContext;
	private PrintWriter out;
	
	private String Result;
	private Handler mHandler;
	public 	Handler iHandler;
	private Thread hthread;
    private static final int OUTPUT = 0xFFF;
    
    private WSComponentParser TranslateJPWSCParser;
    private WSComponentParser TranslateZHWSCParser;
    private WSComponentParser TranslateFRWSCParser;
    private WSComponentParser TranslateESWSCParser;
    
    private Thread threadTranslateJP;
	private Parcel replyTranslateJP;
	private String TranslateJP_Word;
	private static final int WS_TranslateJP_SUCCEED = 0x101;
	private static final int WS_TranslateJP_FAILED = 0x10F;
	
	private Thread threadTranslateZH;
	private Parcel replyTranslateZH;
	private String TranslateZH_Word;
	private static final int WS_TranslateZH_SUCCEED = 0x201;
	private static final int WS_TranslateZH_FAILED = 0x20F;
    
	private Thread threadTranslateFR;
	private Parcel replyTranslateFR;
	private String TranslateFR_Word;
	private static final int WS_TranslateFR_SUCCEED = 0x301;
	private static final int WS_TranslateFR_FAILED = 0x30F;
	
	private Thread threadTranslateES;
	private Parcel replyTranslateES;
	private String TranslateES_Word;
	private static final int WS_TranslateES_SUCCEED = 0x401;
	private static final int WS_TranslateES_FAILED = 0x40F;
	
	/*private static final int SYNCHRONIZE_1 = 0xFF1;
    private boolean Synchronize_1_TranslateJP = false;
    private boolean Synchronize_1_TranslateZH = false;
    private boolean Synchronize_1_TranslateFR = false;
    private boolean Synchronize_1_TranslateES = false;*/
	
    private String q;
	
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		androidContext = (android.content.Context) config.getServletContext().getAttribute("org.mortbay.ijetty.context");
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response)throws ServletException, IOException {
		doGet(request, response);
	}
	
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{
		System.out.println("START_TIME:"+getTimeStamp());
		ServerThreadMonitor stm = ServerThreadMonitor.getInstance();
		if(!stm.isFree) waitserver();
		
		response.setContentType("text/html; charset=UTF-8");
		response.setCharacterEncoding("UTF-8");
		out = response.getWriter();
		
		q = request.getParameter("q"); //?isbn=
		
		hthread = new Thread() {
			public void run() {
				Looper.prepare();
				mHandler = new Handler() {
					@Override 
					public void handleMessage(Message msg){
						ThreadMonitor tm = ThreadMonitor.getInstance();
						JsonOutputBuilder job = new JsonOutputBuilder();
						Bundle tempBundle;
						String[] tempArray;
						switch (msg.what) {
						case WS_TranslateJP_SUCCEED:
							stopThread(threadTranslateJP);
							
		                	tempBundle = replyTranslateJP.readBundle();
		                	tempArray = tempBundle.getStringArray("WordJP");
		                	TranslateJP_Word = tempArray[0];
		                	
		                	tempBundle = new Bundle();
		    				tempBundle.putString("q", q);
		                	TranslateZHWSCParser = new WSComponentParser();
		    				TranslateZHWSCParser.setBundle(tempBundle);
		    				TranslateZHWSCParser.setXML(getTranslateZHXML());
		    				prepareTranslateZH();
		    				System.out.println("T1_TIME:"+getTimeStamp());
		    				threadTranslateZH.run();
							break;
						case WS_TranslateJP_FAILED:
							stopThread(threadTranslateJP);

							job.setErrorXML(getErrorXML());
							Result = job.getErrorJSON();

							stopThread(hthread);
							synchronized (tm){tm.notify();}
							break;
						case WS_TranslateZH_SUCCEED:
							stopThread(threadTranslateZH);
							
		                	tempBundle = replyTranslateZH.readBundle();
		                	tempArray = tempBundle.getStringArray("WordZH");
		                	TranslateZH_Word = tempArray[0];
		                	
		                	tempBundle = new Bundle();
		    				tempBundle.putString("q", q);
		                	TranslateFRWSCParser = new WSComponentParser();
		    				TranslateFRWSCParser.setBundle(tempBundle);
		    				TranslateFRWSCParser.setXML(getTranslateFRXML());
		    				prepareTranslateFR();
		    				System.out.println("T2_TIME:"+getTimeStamp());
		    				threadTranslateFR.run();
							break;
						case WS_TranslateZH_FAILED:
							stopThread(threadTranslateZH);

							job.setErrorXML(getErrorXML());
							Result = job.getErrorJSON();

							stopThread(hthread);
							synchronized (tm){tm.notify();}
							break;
						case WS_TranslateFR_SUCCEED:
							stopThread(threadTranslateFR);
							
		                	tempBundle = replyTranslateFR.readBundle();
		                	tempArray = tempBundle.getStringArray("WordFR");
		                	TranslateFR_Word = tempArray[0];
		                	
		                	tempBundle = new Bundle();
		    				tempBundle.putString("q", q);
		                	TranslateESWSCParser = new WSComponentParser();
		    				TranslateESWSCParser.setBundle(tempBundle);
		    				TranslateESWSCParser.setXML(getTranslateESXML());
		    				prepareTranslateES();
		    				System.out.println("T3_TIME:"+getTimeStamp());
		    				threadTranslateES.run();
							break;
						case WS_TranslateFR_FAILED:
							stopThread(threadTranslateFR);

							job.setErrorXML(getErrorXML());
							Result = job.getErrorJSON();

							stopThread(hthread);
							synchronized (tm){tm.notify();}
							break;
						case WS_TranslateES_SUCCEED:
							stopThread(threadTranslateES);
							
							System.out.println("T4_TIME:"+getTimeStamp());
							mHandler.sendEmptyMessage(OUTPUT);

							break;
						case WS_TranslateES_FAILED:
							stopThread(threadTranslateES);

							job.setErrorXML(getErrorXML());
							Result = job.getErrorJSON();

							stopThread(hthread);
							synchronized (tm){tm.notify();}
							break;
						
						case OUTPUT:
							tempBundle = new Bundle();
							tempBundle.putString("WordJP", TranslateJP_Word);
							tempBundle.putString("WordZH", TranslateZH_Word);
							tempBundle.putString("WordFR", TranslateFR_Word);
							tempBundle.putString("WordES", TranslateES_Word);
							
							job.setBundle(tempBundle);
		                	job.setXML(getOutputXML());
		                	Result = job.getJSON();
							
		                	stopThread(hthread);
							synchronized (tm){debug("tm.notified();");tm.notify();}
							break;
						default:
							super.handleMessage(msg);
						}
					}
				};
				iHandler = new Handler();
				Bundle tempBundle = new Bundle();
				tempBundle.putString("q", q);
				TranslateJPWSCParser = new WSComponentParser();
				TranslateJPWSCParser.setBundle(tempBundle);
				TranslateJPWSCParser.setXML(getTranslateJPXML());
				prepareTranslateJP();
				
				threadTranslateJP.run();
				
				Looper.loop();
			}
		};
		
		stm.isFree = false;
		hthread.start();
		waitfinal();
		out.print(Result);
		
		synchronized (stm){stm.isFree=true;debug("stm.notified();"); stm.notify();}
		System.out.println("FINISH_TIME:"+getTimeStamp());
	}
	
	public void debug(String msg){
		Log.d("AmazonPrice",msg);
	}
	
	public void waitfinal(){
		ThreadMonitor tm = ThreadMonitor.getInstance();
		synchronized (tm) {
			try {
				tm.wait();
			} catch (InterruptedException e) {
				debug("waitfinal()->error="+e.toString());
			}
		}
	}
	
	public void waitserver(){
		ServerThreadMonitor stm = ServerThreadMonitor.getInstance();
		synchronized (stm) {
			try {
				stm.wait();
			} catch (InterruptedException e) {
				debug("waitserver()->error="+e.toString());
			}
		}
	}
	
	public String getTimeStamp(){
		Calendar c = Calendar.getInstance();
		
        int hours = c.get(Calendar.HOUR);
        int minutes = c.get(Calendar.MINUTE);
        int seconds = c.get(Calendar.SECOND);
        int mseconds = c.get(Calendar.MILLISECOND);
        return  DateFormat.getDateInstance().format(new Date())+" "+hours + ":"+minutes + ":"+ seconds+"."+mseconds;
	}
	
	public synchronized void stopThread(Thread t) {
		if (t != null) {
			Thread moribund = t;
			t = null;
			moribund.interrupt();
		}
	}
	
	public String getOutputXML(){
		return 
		"<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"+
		"<object>\n"+
		"	<name>ResultStatus</name>\n"+
		"	<value>succeed</value>\n"+
		"	<name>WordJP</name>\n"+
		"	<value>TranslateJP.output.WordJP</value>\n"+
		"	<name>WordZH</name>\n"+
		"	<value>TranslateZH.output.WordZH</value>\n"+
		"	<name>WordFR</name>\n"+
		"	<value>TranslateFR.output.WordFR</value>\n"+
		"	<name>WordES</name>\n"+
		"	<value>TranslateFR.output.WordES</value>\n"+
		"</object>";
	}

	public String getErrorXML(){
		return 
		"<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"+
		"<error>\n"+
		"	<name>ResultStatus</name>\n"+
		"	<value>failed</value>\n"+
		"</error>";
	}
	
	public String getTranslateJPXML(){
		return 
		"<component>\n"+
		"	<name>ExchageRate</name>\n"+
		"	<role>\n"+
		"		<medium>\n"+
		"			<subscriber-id>001</subscriber-id>\n"+
		"			<publisher-id>002</publisher-id>\n"+
		"		</medium>\n"+
		"	</role>\n"+
		"	<execution>single</execution>\n"+
		"	<webservice>\n"+
		"		<base>http://ajax.googleapis.com/</base>\n"+
		"		<paths>\n"+
		"			<path>ajax</path>\n"+
		"			<path>services</path>\n"+
		"			<path>language</path>\n"+
		"			<path>translate</path>\n"+
		"		</paths>\n"+
		"		<keys>\n"+
		"			<key>q</key>\n"+
		"			<key>v</key>\n"+
		"			<key>langpair</key>\n"+
		"		</keys>\n"+
		"		<values>\n"+
		"			<value>input.q</value>\n"+
		"			<value>1.0</value>\n"+
		"			<value>en|ja</value>\n"+
		"		</values>\n"+
		"		<format>JSON</format>\n"+
		"		<results>\n"+
		"			<result>\n"+
		"				<result-name>WordJP</result-name>\n"+
		"				<type>single</type>\n"+
		"				<query>responseData.translatedText</query>\n"+
		"				<index>null</index>\n"+
		"				<filter>null</filter>\n"+
		"			</result>\n"+
		"		</results>\n"+
		"	</webservice>\n"+
		"</component>";
	}
	
	public void prepareTranslateJP(){
		threadTranslateJP = new Thread(new Runnable() {
			public void run() {
				replyTranslateJP = Parcel.obtain();
				Intent i = new Intent("com.prach.mashup.WSCService");
				boolean isConnected = androidContext.bindService(i,new ServiceConnection(){
					final int serviceCode = 0x101;
					public void onServiceConnected(ComponentName cname,IBinder service) {
						debug("TranslateJP WS Service connected: "+ cname.flattenToShortString());

						Parcel data = Parcel.obtain();
						Bundle bundle = new Bundle();

						bundle.putString("BASE", TranslateJPWSCParser.base);
						bundle.putStringArray("PATHS",TranslateJPWSCParser.paths);
						bundle.putStringArray("KEYS", TranslateJPWSCParser.keys);
						bundle.putStringArray("VALUES",TranslateJPWSCParser.values);
						bundle.putString("FORMAT", TranslateJPWSCParser.format);
						
						bundle.putStringArray("NAME", TranslateJPWSCParser.name);
						bundle.putStringArray("TYPE", TranslateJPWSCParser.type);
						bundle.putStringArray("QUERY", TranslateJPWSCParser.query);
						bundle.putStringArray("INDEX", TranslateJPWSCParser.index);
						
						data.writeBundle(bundle);
						boolean res = false;
						try {
							res = service.transact(serviceCode, data,replyTranslateJP, 0);
						} catch (RemoteException ex) {
							Log.e("onServiceConnected",
									"Remote exception when calling service",ex);
							res = false;
						}
						if (res)
							mHandler.sendEmptyMessage(WS_TranslateJP_SUCCEED);
						else
							mHandler.sendEmptyMessage(WS_TranslateJP_FAILED);
					}

					public void onServiceDisconnected(ComponentName name) {
						debug("ExchangeRate WS Service disconnected: "+ name.flattenToShortString());

					}
				}, Context.BIND_AUTO_CREATE);

				if (!isConnected) {
					debug("ExchangeRate WS Service could not be connected ");
					mHandler.sendEmptyMessage(WS_TranslateJP_FAILED);
				}
			}
		});
	}
	
	
	
	public String getTranslateZHXML(){
		return 
		"<component>\n"+
		"	<name>ExchageRate</name>\n"+
		"	<role>\n"+
		"		<medium>\n"+
		"			<subscriber-id>001</subscriber-id>\n"+
		"			<publisher-id>002</publisher-id>\n"+
		"		</medium>\n"+
		"	</role>\n"+
		"	<execution>single</execution>\n"+
		"	<webservice>\n"+
		"		<base>http://ajax.googleapis.com/</base>\n"+
		"		<paths>\n"+
		"			<path>ajax</path>\n"+
		"			<path>services</path>\n"+
		"			<path>language</path>\n"+
		"			<path>translate</path>\n"+
		"		</paths>\n"+
		"		<keys>\n"+
		"			<key>q</key>\n"+
		"			<key>v</key>\n"+
		"			<key>langpair</key>\n"+
		"		</keys>\n"+
		"		<values>\n"+
		"			<value>input.q</value>\n"+
		"			<value>1.0</value>\n"+
		"			<value>en|zh</value>\n"+
		"		</values>\n"+
		"		<format>JSON</format>\n"+
		"		<results>\n"+
		"			<result>\n"+
		"				<result-name>WordZH</result-name>\n"+
		"				<type>single</type>\n"+
		"				<query>responseData.translatedText</query>\n"+
		"				<index>null</index>\n"+
		"				<filter>null</filter>\n"+
		"			</result>\n"+
		"		</results>\n"+
		"	</webservice>\n"+
		"</component>";
	}
	
	public void prepareTranslateZH(){
		threadTranslateZH = new Thread(new Runnable() {
			public void run() {
				replyTranslateZH = Parcel.obtain();
				Intent i = new Intent("com.prach.mashup.WSCService");
				boolean isConnected = androidContext.bindService(i,new ServiceConnection(){
					final int serviceCode = 0x101;
					public void onServiceConnected(ComponentName cname,IBinder service) {
						debug("ExchangeRate WS Service connected: "+ cname.flattenToShortString());

						Parcel data = Parcel.obtain();
						Bundle bundle = new Bundle();

						bundle.putString("BASE", TranslateZHWSCParser.base);
						bundle.putStringArray("PATHS",TranslateZHWSCParser.paths);
						bundle.putStringArray("KEYS", TranslateZHWSCParser.keys);
						bundle.putStringArray("VALUES",TranslateZHWSCParser.values);
						bundle.putString("FORMAT", TranslateZHWSCParser.format);
						
						bundle.putStringArray("NAME", TranslateZHWSCParser.name);
						bundle.putStringArray("TYPE", TranslateZHWSCParser.type);
						bundle.putStringArray("QUERY", TranslateZHWSCParser.query);
						bundle.putStringArray("INDEX", TranslateZHWSCParser.index);
						
						data.writeBundle(bundle);
						boolean res = false;
						try {
							res = service.transact(serviceCode, data,replyTranslateZH, 0);
						} catch (RemoteException ex) {
							Log.e("onServiceConnected",
									"Remote exception when calling service",ex);
							res = false;
						}
						if (res)
							mHandler.sendEmptyMessage(WS_TranslateZH_SUCCEED);
						else
							mHandler.sendEmptyMessage(WS_TranslateZH_FAILED);
					}

					public void onServiceDisconnected(ComponentName name) {
						debug("ExchangeRate WS Service disconnected: "+ name.flattenToShortString());

					}
				}, Context.BIND_AUTO_CREATE);

				if (!isConnected) {
					debug("ExchangeRate WS Service could not be connected ");
					mHandler.sendEmptyMessage(WS_TranslateZH_FAILED);
				}
			}
		});
	}
	
	public String getTranslateFRXML(){
		return 
		"<component>\n"+
		"	<name>ExchageRate</name>\n"+
		"	<role>\n"+
		"		<medium>\n"+
		"			<subscriber-id>001</subscriber-id>\n"+
		"			<publisher-id>002</publisher-id>\n"+
		"		</medium>\n"+
		"	</role>\n"+
		"	<execution>single</execution>\n"+
		"	<webservice>\n"+
		"		<base>http://ajax.googleapis.com/</base>\n"+
		"		<paths>\n"+
		"			<path>ajax</path>\n"+
		"			<path>services</path>\n"+
		"			<path>language</path>\n"+
		"			<path>translate</path>\n"+
		"		</paths>\n"+
		"		<keys>\n"+
		"			<key>q</key>\n"+
		"			<key>v</key>\n"+
		"			<key>langpair</key>\n"+
		"		</keys>\n"+
		"		<values>\n"+
		"			<value>input.q</value>\n"+
		"			<value>1.0</value>\n"+
		"			<value>en|fr</value>\n"+
		"		</values>\n"+
		"		<format>JSON</format>\n"+
		"		<results>\n"+
		"			<result>\n"+
		"				<result-name>WordFR</result-name>\n"+
		"				<type>single</type>\n"+
		"				<query>responseData.translatedText</query>\n"+
		"				<index>null</index>\n"+
		"				<filter>null</filter>\n"+
		"			</result>\n"+
		"		</results>\n"+
		"	</webservice>\n"+
		"</component>";
	}
	
	public void prepareTranslateFR(){
		threadTranslateFR = new Thread(new Runnable() {
			public void run() {
				replyTranslateFR = Parcel.obtain();
				Intent i = new Intent("com.prach.mashup.WSCService");
				boolean isConnected = androidContext.bindService(i,new ServiceConnection(){
					final int serviceCode = 0x101;
					public void onServiceConnected(ComponentName cname,IBinder service) {
						debug("ExchangeRate WS Service connected: "+ cname.flattenToShortString());

						Parcel data = Parcel.obtain();
						Bundle bundle = new Bundle();

						bundle.putString("BASE", TranslateFRWSCParser.base);
						bundle.putStringArray("PATHS",TranslateFRWSCParser.paths);
						bundle.putStringArray("KEYS", TranslateFRWSCParser.keys);
						bundle.putStringArray("VALUES",TranslateFRWSCParser.values);
						bundle.putString("FORMAT", TranslateFRWSCParser.format);
						
						bundle.putStringArray("NAME", TranslateFRWSCParser.name);
						bundle.putStringArray("TYPE", TranslateFRWSCParser.type);
						bundle.putStringArray("QUERY", TranslateFRWSCParser.query);
						bundle.putStringArray("INDEX", TranslateFRWSCParser.index);
						
						data.writeBundle(bundle);
						boolean res = false;
						try {
							res = service.transact(serviceCode, data,replyTranslateFR, 0);
						} catch (RemoteException ex) {
							Log.e("onServiceConnected",
									"Remote exception when calling service",ex);
							res = false;
						}
						if (res)
							mHandler.sendEmptyMessage(WS_TranslateFR_SUCCEED);
						else
							mHandler.sendEmptyMessage(WS_TranslateFR_FAILED);
					}

					public void onServiceDisconnected(ComponentName name) {
						debug("ExchangeRate WS Service disconnected: "+ name.flattenToShortString());

					}
				}, Context.BIND_AUTO_CREATE);

				if (!isConnected) {
					debug("ExchangeRate WS Service could not be connected ");
					mHandler.sendEmptyMessage(WS_TranslateFR_FAILED);
				}
			}
		});
	}
	
	public String getTranslateESXML(){
		return 
		"<component>\n"+
		"	<name>ExchageRate</name>\n"+
		"	<role>\n"+
		"		<medium>\n"+
		"			<subscriber-id>001</subscriber-id>\n"+
		"			<publisher-id>002</publisher-id>\n"+
		"		</medium>\n"+
		"	</role>\n"+
		"	<execution>single</execution>\n"+
		"	<webservice>\n"+
		"		<base>http://ajax.googleapis.com/</base>\n"+
		"		<paths>\n"+
		"			<path>ajax</path>\n"+
		"			<path>services</path>\n"+
		"			<path>language</path>\n"+
		"			<path>translate</path>\n"+
		"		</paths>\n"+
		"		<keys>\n"+
		"			<key>q</key>\n"+
		"			<key>v</key>\n"+
		"			<key>langpair</key>\n"+
		"		</keys>\n"+
		"		<values>\n"+
		"			<value>input.q</value>\n"+
		"			<value>1.0</value>\n"+
		"			<value>en|es</value>\n"+
		"		</values>\n"+
		"		<format>JSON</format>\n"+
		"		<results>\n"+
		"			<result>\n"+
		"				<result-name>WordES</result-name>\n"+
		"				<type>single</type>\n"+
		"				<query>responseData.translatedText</query>\n"+
		"				<index>null</index>\n"+
		"				<filter>null</filter>\n"+
		"			</result>\n"+
		"		</results>\n"+
		"	</webservice>\n"+
		"</component>";
	}
	
	public void prepareTranslateES(){
		threadTranslateES = new Thread(new Runnable() {
			public void run() {
				replyTranslateES = Parcel.obtain();
				Intent i = new Intent("com.prach.mashup.WSCService");
				boolean isConnected = androidContext.bindService(i,new ServiceConnection(){
					final int serviceCode = 0x101;
					public void onServiceConnected(ComponentName cname,IBinder service) {
						debug("ExchangeRate WS Service connected: "+ cname.flattenToShortString());

						Parcel data = Parcel.obtain();
						Bundle bundle = new Bundle();

						bundle.putString("BASE", TranslateESWSCParser.base);
						bundle.putStringArray("PATHS",TranslateESWSCParser.paths);
						bundle.putStringArray("KEYS", TranslateESWSCParser.keys);
						bundle.putStringArray("VALUES",TranslateESWSCParser.values);
						bundle.putString("FORMAT", TranslateESWSCParser.format);
						
						bundle.putStringArray("NAME", TranslateESWSCParser.name);
						bundle.putStringArray("TYPE", TranslateESWSCParser.type);
						bundle.putStringArray("QUERY", TranslateESWSCParser.query);
						bundle.putStringArray("INDEX", TranslateESWSCParser.index);
						
						data.writeBundle(bundle);
						boolean res = false;
						try {
							res = service.transact(serviceCode, data,replyTranslateES, 0);
						} catch (RemoteException ex) {
							Log.e("onServiceConnected",
									"Remote exception when calling service",ex);
							res = false;
						}
						if (res)
							mHandler.sendEmptyMessage(WS_TranslateES_SUCCEED);
						else
							mHandler.sendEmptyMessage(WS_TranslateES_FAILED);
					}

					public void onServiceDisconnected(ComponentName name) {
						debug("ExchangeRate WS Service disconnected: "+ name.flattenToShortString());

					}
				}, Context.BIND_AUTO_CREATE);

				if (!isConnected) {
					debug("ExchangeRate WS Service could not be connected ");
					mHandler.sendEmptyMessage(WS_TranslateES_FAILED);
				}
			}
		});
	}
}
