package com.michalmazur.walutomat;

import com.michalmazur.walutomat.CurrencyPair;

import java.io.IOException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class WalutomatActivity extends Activity {

	RateDownloadTask downloadTask;

	static final int DIALOG_IO_EXCEPTION = 0;
	static final int DIALOG_HTML_ERROR = 1;
	static final int DIALOG_PLEASE_WAIT = 2;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		findViewById(R.id.layout).setVisibility(View.GONE);
		disableCertificateValidation();
		System.setProperty("http.keepAlive", "false");
	}

	@Override
	protected void onResume() {
		super.onResume();
		loadAndDisplayRates();
	}

	private void loadAndDisplayRates() {
		downloadTask = new RateDownloadTask();
		downloadTask.execute();
	}

	/**
	 * Displays CurrencyPair objects in a tabular format.
	 */
	private void displayCurrencyPairRates(ArrayList<CurrencyPair> pairs) {
		findViewById(R.id.layout).setVisibility(View.VISIBLE);
		TableLayout table = (TableLayout) findViewById(R.id.table);
		TableRow headerRow = (TableRow) findViewById(R.id.headerRow);
		table.removeAllViews();
		table.addView(headerRow);
		for (CurrencyPair pair : pairs) {
			TableRow row = new TableRow(this);

			TextView currencies = new TextView(this);
			currencies.setText(pair.getName());
			row.addView(currencies);

			TextView buyingPrice = new TextView(this);
			buyingPrice.setText(pair.getPurchasePrice());
			row.addView(buyingPrice);

			TextView sellingPrice = new TextView(this);
			sellingPrice.setText(pair.getSalePrice());
			row.addView(sellingPrice);

			TextView rate = new TextView(this);
			rate.setText(pair.getRate());
			row.addView(rate);

			table.addView(row);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		downloadTask.cancel(true);
		tryDismissDialog(DIALOG_PLEASE_WAIT);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.refresh:
			loadAndDisplayRates();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * Retrieves most recent currency rates from www.walutomat.pl.
	 */
	class RateDownloadTask extends AsyncTask<Void, Void, ArrayList<CurrencyPair>> {

		protected Exception exception;

		@Override
		protected void onPreExecute() {
			showDialog(DIALOG_PLEASE_WAIT);
		}

		@Override
		protected ArrayList<CurrencyPair> doInBackground(Void... params) {
			ArrayList<CurrencyPair> pairs = new ArrayList<CurrencyPair>();
			try {
				Document homepage = Jsoup.connect("https://www.walutomat.pl/").get();
				String ratesSelector = "#best_curr div.bg:not(.strong)";
				Elements ratesHtml = homepage.select(ratesSelector);
				for (Element html : ratesHtml) {
					CurrencyPair pair = new CurrencyPair();
					pair.setName(html.getElementsByTag("a").text());
					pair.setPurchasePrice(html.getElementsByAttributeValue("class", "curr1").text());
					pair.setSalePrice(html.getElementsByAttributeValue("class", "curr2").text());
					pair.setRate(html.getElementsByAttributeValue("class", "forex").text());
					pairs.add(pair);
				}
			} catch (IOException e) {
				this.exception = e;
			}
			return pairs;
		}

		@Override
		protected void onPostExecute(ArrayList<CurrencyPair> result) {
			tryDismissDialog(DIALOG_PLEASE_WAIT);
			if (result.size() > 0) {
				displayCurrencyPairRates(result);
			} else if (exception != null) {
				showDialog(DIALOG_IO_EXCEPTION);
			} else {
				showDialog(DIALOG_HTML_ERROR);
			}
		}

	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_IO_EXCEPTION:
			return createSimpleAlertDialog(getString(R.string.connection_failure));

		case DIALOG_HTML_ERROR:
			return createSimpleAlertDialog(getString(R.string.html_parse_failure));

		case DIALOG_PLEASE_WAIT:
			ProgressDialog pd = new ProgressDialog(this);
			pd.setTitle(getString(R.string.loading));
			pd.setMessage(getString(R.string.please_wait));
			pd.setIndeterminate(true);
			return pd;

		default:
			return null;
		}
	}

	/**
	 * Wraps dismissDialog() and silences the IllegalArgumentException that is
	 * thrown when trying to dismiss a dialog that is not being displayed.
	 */
	protected void tryDismissDialog(int id) {
		try {
			dismissDialog(id);
		} catch (IllegalArgumentException e) {
			/*
			 * We attempted to dismiss a dialog that was not being displayed. It
			 * is acceptable because currently we do not keep track of every
			 * dialog's state.
			 */
		}
	}

	AlertDialog createSimpleAlertDialog(String message) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(message).setCancelable(true)
				.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						// nothing to do here
					}
				});
		return builder.create();
	}

	// Adapted from http://exampledepot.com/egs/javax.net.ssl/TrustAll.html
	public static void disableCertificateValidation() {
		// Create a trust manager that does not validate certificate chains
		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			public X509Certificate[] getAcceptedIssuers() {
				return null;
			}

			public void checkClientTrusted(X509Certificate[] certs, String authType) {
				return;
			}

			public void checkServerTrusted(X509Certificate[] certs, String authType) {
				return;
			}
		} };

		// Install the all-trusting trust manager
		try {
			SSLContext sc = SSLContext.getInstance("TLS");
			sc.init(null, trustAllCerts, new SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		} catch (Exception e) {
			/*
			 * If an exception occurs we can still try to proceed as normal. The
			 * user will be notified of a problem with the Internet connection
			 * if the RateDownloadTask fails to finish.
			 */
		}
	}
}