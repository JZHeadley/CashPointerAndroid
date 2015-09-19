package com.jzheadley.CashPointer;

import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

public class DebugActivity extends Activity {
	private static final int KEY_DISTANCE = 2;
	private static final int KEY_ETA = 3;
	private static final int KEY_BERING = 4;
	private static final double AVERAGE_WALK_SPEED = 3.5;
	// private static final UUID WATCHAPP_UUID =
	// UUID.fromString("d82c6b67-68a6-4edd-93c3-ffeb5229acb6");
	private static final UUID WATCHAPP_UUID = UUID.fromString("5ec1ba20-9b7f-4bbf-af47-6818a16e1a30");
	private final String RAD = "50";
	private final String KEY = "&key=b26f736120f2ea2718bb3f280a7d1cf9";
	private GPSTracker gps;
	private Location location;
	private Location nearestATM;
	private TextView latitudeField;
	private TextView longitudeField;
	private double curLat;
	private double curLong;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_debug);
		gps = new GPSTracker(this);
		location = gps.getLocation();
		curLong = gps.getLongitude();
		curLat = gps.getLatitude();

		latitudeField = (TextView) findViewById(R.id.latitude_display);
		longitudeField = (TextView) findViewById(R.id.longitude_display);
		latitudeField.setText(curLat + "");
		longitudeField.setText(curLong + "");
		String url = "http://api.reimaginebanking.com/atms?" + "lat=" + curLat + "&" + "lng=" + curLong + "&" + "rad="
				+ RAD + KEY;
		new ProcessJSON(location).execute(url);

		// Add Install Button behavior
		Button distanceButton = (Button) findViewById(R.id.distance_button);
		distanceButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				PebbleDictionary out = new PebbleDictionary();
				float distance = location.distanceTo(nearestATM);
				out.addInt32(KEY_DISTANCE, Math.round(distance));
				PebbleKit.sendDataToPebble(getApplicationContext(), WATCHAPP_UUID, out);
			}
		});
		// Add Install Button behavior
		Button eta_button = (Button) findViewById(R.id.eta_button);
		eta_button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				PebbleDictionary out = new PebbleDictionary();
				float distance = location.distanceTo(nearestATM);
				double ETA = (distance * 0.00062137) / AVERAGE_WALK_SPEED;
				out.addInt32(KEY_ETA, (int) ETA);
				PebbleKit.sendDataToPebble(getApplicationContext(), WATCHAPP_UUID, out);
			}
		});
		// Add Bering Button behavior
		Button bearing_button = (Button) findViewById(R.id.bearing_button);
		bearing_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PebbleDictionary out = new PebbleDictionary();
				double bearing = location.bearingTo(nearestATM);
				out.addUint32(KEY_BERING, (int) (bearing * 1000));
            }
        });
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_debug, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		// noinspection SimplifiableIfStatement
		if (id == R.id.action_settings) {
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	private class ProcessJSON extends AsyncTask<String, Void, String> {
		private Location location;

		ProcessJSON(Location location) {
			this.location = location;

		}

		protected String doInBackground(String... strings) {
			String stream;
			String urlString = strings[0];

			HTTPDataHandler dataHandler = new HTTPDataHandler();
			stream = dataHandler.GetHTTPData(urlString);
			// Return the data from specified url
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
					double a2 = Math.pow(alat - curLat, 2);
					double b2 = Math.pow(alon - curLong, 2);
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
				((TextView) findViewById(R.id.latitude_display)).setText(curLat + "");
				((TextView) findViewById(R.id.longitude_display)).setText(curLong + "");
				((TextView) findViewById(R.id.atm_lat_display)).setText("" + geo.get("lat"));
				((TextView) findViewById(R.id.atm_long_display)).setText("" + geo.get("lng"));
				nearestATM = new Location(location);
				nearestATM.setLatitude((Double) geo.get("lat"));
				nearestATM.setLatitude((Double) geo.get("lng"));
				// distance between user and atm
				double distance = location.distanceTo(nearestATM);

				double ETA = (distance * 0.00062137) / AVERAGE_WALK_SPEED;
				// Bearing, Distance, Heading,

			} catch (JSONException e) {
				e.printStackTrace();
			}

		}
	}
}
