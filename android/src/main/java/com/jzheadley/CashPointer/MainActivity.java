package com.jzheadley.CashPointer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.PebbleKit.PebbleDataReceiver;
import com.getpebble.android.kit.util.PebbleDictionary;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends Activity {
	private static final UUID WATCHAPP_UUID = UUID.fromString("5ec1ba20-9b7f-4bbf-af47-6818a16e1a30");
	// private static final UUID WATCHAPP_UUID =
	// UUID.fromString("d82c6b67-68a6-4edd-93c3-ffeb5229acb6");
	private static final String WATCHAPP_FILENAME = "CashPointer.pbw";
	private static final double AVERAGE_WALK_SPEED = 3.5;
	private GPSTracker gps;
	private String url;
	private static final int RAD = 50;
	private static final String KEY = "b26f736120f2ea2718bb3f280a7d1cf9";
	private Location location;
	private Location nearestATM;
	private static final int KEY_BUTTON = 0, KEY_VIBRATE = 1, BUTTON_UP = 0, BUTTON_SELECT = 1, BUTTON_DOWN = 2,
			KEY_DISTANCE = 2, KEY_ETA = 3;
	private Handler handler = new Handler();
	private PebbleDataReceiver appMessageReciever;
	private TextView whichButtonView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		gps = new GPSTracker(this);
		// Customize ActionBar
		ActionBar actionBar = getActionBar();
		if (!(actionBar.equals(null)))
			actionBar.setTitle("CashPointer");
		actionBar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.actionbar_orange)));
		location = gps.getLocation();
		url = "http://api.reimaginebanking.com/atms?" + "lat=" + gps.getLatitude() + "&" + "lng=" + gps.getLongitude()
				+ "&" + "rad=" + RAD + "&key=" + KEY;
		new ProcessJSON(location).execute(url);
		setupButtonListeners();
	}

	private void setupButtonListeners() {
		// Add Install Button behavior
		Button installButton = (Button) findViewById(R.id.button_install);
		installButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// Install
				Toast.makeText(getApplicationContext(), "Installing watchapp...", Toast.LENGTH_SHORT).show();
				sideloadInstall(getApplicationContext(), WATCHAPP_FILENAME);
			}
		});
		// Add Debug Button behavior
		Button debugButton = (Button) findViewById(R.id.button_debug);
		debugButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent debug = new Intent(getApplicationContext(), DebugActivity.class);
				startActivity(debug);
			}
		});
		// Add vibrate Button behavior
		Button vibrateButton = (Button) findViewById(R.id.button_vibrate);
		vibrateButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// Send KEY_VIBRATE to Pebble
				PebbleDictionary out = new PebbleDictionary();
				out.addInt32(KEY_VIBRATE, 0);
				PebbleKit.sendDataToPebble(getApplicationContext(), WATCHAPP_UUID, out);
			}
		});
		// Add output TextView behavior
		whichButtonView = (TextView) findViewById(R.id.which_button);
	}

	@Override
	protected void onResume() {
		super.onResume();

		// Define AppMessage behavior
		if (appMessageReciever == null) {
			appMessageReciever = new PebbleDataReceiver(WATCHAPP_UUID) {

				@Override
				public void receiveData(Context context, int transactionId, PebbleDictionary data) {
					// Always ACK
					PebbleKit.sendAckToPebble(context, transactionId);

					// What message was received?
					if (data.getInteger(KEY_BUTTON) != null) {
						// KEY_BUTTON was received, determine which button
						final int button = data.getInteger(KEY_BUTTON).intValue();

						// Update UI on correct thread
						handler.post(new Runnable() {

							@Override
							public void run() {
								switch (button) {
								case BUTTON_UP:
									whichButtonView.setText("UP");
									break;
								case BUTTON_SELECT:
									whichButtonView.setText("SELECT");
									break;
								case BUTTON_DOWN:
									whichButtonView.setText("DOWN");
									break;
								default:
									Toast.makeText(getApplicationContext(), "Unknown button: " + button,
											Toast.LENGTH_SHORT).show();
									break;
								}
							}

						});
					}
				}
			};
			// Add AppMessage capabilities
			PebbleKit.registerReceivedDataHandler(this, appMessageReciever);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();

		// Unregister AppMessage reception
		if (appMessageReciever != null) {
			unregisterReceiver(appMessageReciever);
			appMessageReciever = null;
		}
	}

	public void sideloadInstall(Context ctx, String assetFilename) {
		try {
			// Read .pbw from assets/
			Intent intent = new Intent(Intent.ACTION_VIEW);
			File file = new File(ctx.getExternalFilesDir(null), assetFilename);
			InputStream is = ctx.getResources().getAssets().open(assetFilename);
			OutputStream os = new FileOutputStream(file);
			byte[] pbw = new byte[is.available()];
			is.read(pbw);
			os.write(pbw);
			is.close();
			os.close();
			// Install via Pebble Android app
			intent.setDataAndType(Uri.fromFile(file), "application/pbw");
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			ctx.startActivity(intent);
		} catch (IOException e) {
			Toast.makeText(ctx, "App install failed: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
		}
		PebbleKit.startAppOnPebble(getApplicationContext(), WATCHAPP_UUID);
	}

	private class ProcessJSON extends AsyncTask<String, Void, String> {
		private Location location;

		ProcessJSON(Location location) {
			this.location = location;
		}

		protected String doInBackground(String... strings) {
			String stream = null;
			String urlString = strings[0];
			HTTPDataHandler dataHandler = new HTTPDataHandler();
			stream = dataHandler.GetHTTPData(urlString);
			return stream;
		}

		protected void onPostExecute(String stream) {
			JSONObject obj = null;
			try {
				obj = new JSONObject(stream);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			try {
				JSONArray arr = obj.getJSONArray("data");
				double c[] = new double[arr.length()];
				for (int i = 0; i < arr.length(); i++) {
					JSONObject geo = arr.getJSONObject(i).getJSONObject("geocode");
					double alat = geo.getDouble("lat");
					double alon = geo.getDouble("lng");
					double a2 = Math.pow(alat - location.getLatitude(), 2);
					double b2 = Math.pow(alon - location.getLongitude(), 2);
					c[i] = Math.sqrt(a2 + b2);
				}
				double min = Integer.MAX_VALUE;
				int iclose = 0;
				for (int i = 0; i < c.length; i++) {
					if (c[i] < min) {
						min = c[i];
						iclose = i;
					}
				}
				JSONObject geo = arr.getJSONObject(iclose).getJSONObject("geocode");
				nearestATM = new Location(location);
				nearestATM.setLatitude((Double) geo.get("lat"));
				nearestATM.setLatitude((Double) geo.get("lng"));
				double distance = location.distanceTo(nearestATM);
				double ETA = (distance * 0.00062137) / AVERAGE_WALK_SPEED;
				// Bearing, Distance, Heading,
				PebbleDictionary out = new PebbleDictionary();
				out.addUint32(KEY_DISTANCE, (int) Math.round(distance));
				out.addInt32(KEY_ETA, (int) ETA);
				PebbleKit.sendDataToPebble(getApplicationContext(), WATCHAPP_UUID, out);
			} catch (JSONException e) {
				e.printStackTrace();
			}

		}
	}
}
