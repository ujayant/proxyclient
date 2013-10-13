package net.jayantupadhyaya.proxyclient;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.lang.String;

import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebSettings.LayoutAlgorithm;
import android.webkit.WebSettings.RenderPriority;
import android.webkit.WebSettings.ZoomDensity;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.Toast;

public class HomeScreenActivity extends Activity {

	private static final String TAG = "HomeScreenActivity";
	private BroadcastReceiver clientReceiver;
	private EditText editText;
	private IntentFilter clientFilter;
	String absoluteURL;
	URL convertedURL = null;
	URI receivedLink = null;
	URI absoluteURI = null;
	String clickedURL = null;
	private WebView webView;
	private ProgressDialog progressBar;
	String response;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_home_screen);

		webView = (WebView) findViewById(R.id.webView);

		WebSettings settings = webView.getSettings();
		settings.setJavaScriptEnabled(true);
		settings.setUseWideViewPort(true);
		settings.setLoadsImagesAutomatically(true);
		settings.setBuiltInZoomControls(true);
		settings.setLayoutAlgorithm(LayoutAlgorithm.NORMAL);
		settings.setLoadWithOverviewMode(true);
		settings.setSupportZoom(false);
		settings.setDefaultZoom(ZoomDensity.MEDIUM);
		settings.setRenderPriority(RenderPriority.HIGH);
		settings.setUserAgentString(settings.getUserAgentString());
		

		webView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
		webView.setInitialScale(60);
		webView.setScrollbarFadingEnabled(true);

		clientReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				Log.i(TAG, "Response Received");
				response = intent.getStringExtra("RESPONSE");
				Log.d(TAG, response);
				if (response != null) {
					startWebView();
				}
			}
		};

		clientFilter = new IntentFilter();
		clientFilter.addAction("net.jayantupadhyaya.proxyclient.client");

		registerReceiver(clientReceiver, clientFilter);
	}
	
	public void startWebView() {
		final AlertDialog alertDialog = new AlertDialog.Builder(this).create();
		
		Log.d(TAG, webView.getSettings().getUserAgentString());

		progressBar = ProgressDialog.show(HomeScreenActivity.this, "", "Loading");
		progressBar.setProgress(ProgressDialog.STYLE_SPINNER);

		webView.setWebViewClient(new WebViewClient() {
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				Log.i(TAG, "Clicked Link :" + url);
				try {
					receivedLink = new URI(url);
				} catch (URISyntaxException e1) {
					e1.printStackTrace();
				}
				if (!receivedLink.isAbsolute()) {
					try {
						absoluteURI = new URI(absoluteURL);
						Log.i(TAG, "NOT Absolute");
					} catch (URISyntaxException e) {
						e.printStackTrace();
					}
					receivedLink = absoluteURI.resolve(receivedLink);
					Log.i(TAG, "Resolved Link:" + receivedLink);
					try {
						convertedURL = new URL(receivedLink.toString());
						clickedURL = convertedURL.toString();
					} catch (MalformedURLException e) {
						e.printStackTrace();
					}
				} else {
					try {
						Log.i(TAG, "Clicked Absolute:"+receivedLink.getAuthority());
						absoluteURL = "http://"+receivedLink.getAuthority();
						Log.i(TAG, "ABSOLUTE NOW:"+absoluteURL);
						convertedURL = new URL(receivedLink.toString());
						clickedURL = convertedURL.toString();
					} catch (MalformedURLException e) {
						e.printStackTrace();
					}
				}
				Intent intent = new Intent();
				intent.setAction("net.jayantupadhyaya.proxyserver.server");
				intent.putExtra("SEND_URL", clickedURL);
				sendBroadcast(intent);
				Log.i(TAG, "Clicked Link Sent");
				return true;
			}

			public void onPageFinished(WebView view, String url) {
				Log.i(TAG, "Finished loading URL: " + url);
				if (progressBar.isShowing()) {
					progressBar.dismiss();
				}
			}

			public void onReceivedError(WebView view, int errorCode,
					String description, String failingUrl) {
				Log.e(TAG, "Error: " + description);
				alertDialog.setTitle("Error");
				alertDialog.setMessage(description);
				alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								return;
							}
						});
				alertDialog.show();
			}

		});
		webView.loadDataWithBaseURL(absoluteURL, response, "text/html", "UTF-8", null);
		Log.i(TAG, absoluteURL);
		Log.i(TAG, "Webpage Loaded");
	}

	protected void onResume() {
		super.onResume();
		Log.i(TAG, "RESUMED");
		registerReceiver(clientReceiver, clientFilter);
	}

	protected void onPause() {
		super.onPause();
		Log.i(TAG, "PAUSED");
		unregisterReceiver(clientReceiver);
	}

	public void browse(View view) {

		editText = (EditText) findViewById(R.id.homescreen_url_field);
		absoluteURL = editText.getText().toString();
		Log.i(TAG, absoluteURL);
		if (absoluteURL.equals("")) {
			Toast.makeText(getApplicationContext(), "Please enter URL", Toast.LENGTH_SHORT).show();
		}
		else if (Patterns.WEB_URL.matcher(absoluteURL).matches()) {
			try {
				URI uri = new URI(absoluteURL);
				if (uri.getScheme() == null) {
					absoluteURL = "http://" + absoluteURL;
					Log.d(TAG, "http added" + ' ' + absoluteURL);
				}
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
			
			Intent intent = new Intent();
			intent.setAction("net.jayantupadhyaya.proxyserver.server");
			intent.putExtra("SEND_URL", absoluteURL);
			sendBroadcast(intent);
			Log.i(TAG, "URL Sent");
		}
		else {
			Toast.makeText(getApplicationContext(), "Please enter a valid URL", Toast.LENGTH_SHORT).show();			
		}
	}
}