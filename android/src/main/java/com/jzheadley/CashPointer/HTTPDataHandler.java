package com.jzheadley.CashPointer;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class HTTPDataHandler {

	static String stream = null;

	public HTTPDataHandler() {
	}

	public String GetHTTPData(String urlString) {
		try {
			URL url = new URL(urlString);
			HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

			if (urlConnection.getResponseCode() == 200) {
				// if response code = 200 ok
				InputStream in = new BufferedInputStream(urlConnection.getInputStream());

				// Read the BufferedInputStream
				BufferedReader r = new BufferedReader(new InputStreamReader(in));
				StringBuilder sb = new StringBuilder();
				String line;
				while ((line = r.readLine()) != null) {
					sb.append(line);
				}
				stream = sb.toString();
				// End reading

				urlConnection.disconnect();
			} else {

			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {

		}
		return stream;
	}
}