package se.liu.jesti965.liuprint;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Properties;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

public class MainActivity extends Activity {

	private static String password;
	private static String user;
	private static String host;
	private static int port = 22;
	private static String url;

	/**
	 * Command to send to SSH server
	 * @return
	 */
	private String generateCommand() {
		return new String("./printScript \"" + url + "\" " + getPrintCount() + "\n");
	}
	
	/**
	 * Returns the print count, will be at least 1
	 * @return
	 */
	private int getPrintCount() {
		TextView text = (TextView) findViewById(R.id.copiesNr);
		return Math.max(1, Integer.parseInt(text.getText().toString()));
	}
	
	/**
	 * User clicks print button
	 * 
	 * @param view
	 */
	public void printClick(View view) {
		if (url != null) {		
			if (password != null) { // Already have been given a password
				try {
					executeRemoteCommand(user, password, host, port, generateCommand());
				} catch (Exception e) {
					// Something is wrong, probably a auth error, forget password
					password = null;
					setStatus(e.toString());
				}
			} else { // Not know password
				getPassword();
			}
		}
	}
	
	/**
	 * User clicks print button
	 * 
	 * @param view
	 */
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	        case R.id.menu_settings:
	    		// Show settings
	    		Intent settingsActivity = new Intent(getBaseContext(),
	    				SettingsActivity.class);
	    		startActivity(settingsActivity);
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}

	/**
	 * Reads user data from config
	 */
	private void readUserData() {
		SharedPreferences sharedPref = PreferenceManager
				.getDefaultSharedPreferences(this);
		user = sharedPref.getString("user", "");
		host = sharedPref.getString("server", "");
		port = Integer.parseInt(sharedPref.getString("port", "22"));
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Get intent, action and MIME type
		Intent intent = getIntent();
		String action = intent.getAction();
		String type = intent.getType();

		readUserData();

		if (Intent.ACTION_SEND.equals(action) && type != null
				&& "text/plain".equals(type)) {
			// Started via share text command

			setURL(intent.getStringExtra(Intent.EXTRA_TEXT));
			
		} else {
			// Started via other (main menu)

		}

	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}


	/**
	 * Set URL to send to printserver
	 * @param u
	 */
	void setURL(String u) {
		if (u != null) {
			url = u;
			// Change UI
			TextView text = (TextView) findViewById(R.id.url);
			text.setText(url);
		}
	}

	/**
	 * Display a password dialog, if password is entered, send away SSH command to server.
	 */
	private void getPassword() {

			AlertDialog.Builder alert = new AlertDialog.Builder(this);

			alert.setTitle("liuPrint");
			alert.setMessage("Password for " + user + "@" + host + ":" + port);

			// Set an EditText view to get user input
			final EditText input = new EditText(this);
			input.setInputType(InputType.TYPE_CLASS_TEXT
					| InputType.TYPE_TEXT_VARIATION_PASSWORD);
			alert.setView(input);

			alert.setPositiveButton("Print",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							password = input.getText().toString();

							try {
								executeRemoteCommand(user, password, host,
										port, generateCommand());
							} catch (Exception e) {
								// Something is wrong, probably a auth error,
								// forget password
								password = null;
								setStatus(e.toString());
							}
						}
					});

			alert.setNegativeButton("Cancel",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							// Do nothing...
						}
					});

			alert.show();
	}

	/**
	 * Set status text on GUI
	 * @param stat
	 */
	private void setStatus(String stat) {
		if (stat != null) {
			// Change UI
			TextView text = (TextView) findViewById(R.id.status);
			text.setText(stat);
		}
	}

	/**
	 * Send remote command to SSH server
	 * @param username
	 * @param password
	 * @param hostname
	 * @param port
	 * @param command
	 * @return
	 * @throws Exception
	 */
	public String executeRemoteCommand(String username, String password,
			String hostname, int port, String command) throws Exception {
		setStatus(new String("Printing..."));

		JSch jsch = new JSch();
		Session session = jsch.getSession(username, hostname, port);
		session.setPassword(password);

		// Avoid asking for key confirmation
		Properties prop = new Properties();
		prop.put("StrictHostKeyChecking", "no");
		session.setConfig(prop);

		session.connect();

		// SSH Channel
		ChannelShell channelssh = (ChannelShell) session.openChannel("shell");
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ByteArrayInputStream in = new ByteArrayInputStream(command.getBytes());

		channelssh.setOutputStream(baos);

		channelssh.setInputStream(in);


		// Execute command
		channelssh.connect();

		while (baos.size()<100) {
			
		}
		
		String s = new String(baos.toByteArray());
		setStatus(baos.toString());
		channelssh.disconnect();

		return s;
	}

}
