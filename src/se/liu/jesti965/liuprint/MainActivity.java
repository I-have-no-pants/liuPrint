package se.liu.jesti965.liuprint;

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
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

public class MainActivity extends Activity {
	
	private static String password;
	private static String user;
	private static String host;
	private static int port = 22;
	private static String url;

	/**
	 * User clicks print button
	 * @param view
	 */
	public void printClick(View view) {
		if (url != null) {
			getPassword();
			
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

		if (Intent.ACTION_SEND.equals(action) && type != null) {
			
			// Started via send to -> liuPrint
			if ("text/plain".equals(type)) {
				

				try {
					handleSendText(intent);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					setStatus(e.toString());
				} // Handle text being sent
			} else if (type.startsWith("image/")) {
				handleSendImage(intent); // Handle single image being sent
			}
		} else if (Intent.ACTION_SEND_MULTIPLE.equals(action) && type != null) {
			if (type.startsWith("image/")) {
				handleSendMultipleImages(intent); // Handle multiple images
													// being sent
			}
			
		} else {
			// Started via other (main menu)
			
			// Show settings
			Intent settingsActivity = new Intent(getBaseContext(),
					SettingsActivity.class);
			startActivity(settingsActivity);
		}

	}

	void handleSendText(Intent intent) throws Exception {
			
			// Reads URL and changes command to send
			url = intent.getStringExtra(Intent.EXTRA_TEXT);

			if (url != null) {
				// Change UI
				TextView text = (TextView) findViewById(R.id.url);
				text.setText(url);
			}
			// String command = new String("wget \"" + sharedText +"\" -O out");
			// String command = new String("nohup ./printScript " + sharedText +
			// " &");
	
	}

	private void getPassword() {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle("liuPrint");
		alert.setMessage("Password for " + user + "@" + host + ":" + port);

		// Set an EditText view to get user input
		final EditText input = new EditText(this);
		input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
		alert.setView(input);
		
		
		alert.setPositiveButton("Print", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				MainActivity.password = input.getText().toString();
				

				// Make it Unix friendly
				/*
				String unixPassword = password.replace("\\", "\\\\");
				unixPassword = unixPassword.replace("(", "\\(");
				unixPassword = unixPassword.replace("*", "\\*");
				unixPassword = unixPassword.replace("?", "\\?");
				unixPassword = unixPassword.replace(" ", "\\ ");
				*/
				
				// Print!
				//String command = new String("./printScript \"" + url + "\" " + user + " " + unixPassword + "");
				String command = new String("./printScript \"" + url + "\"");
				
				
				try {
					executeRemoteCommand(user,password, host, port, command);
				} catch (Exception e) {
					setStatus(e.toString());
				}
			}
		});

		alert.setNegativeButton("Cancel",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// Do nothing...
					}
				});

		alert.show();
		
	}
	
	private void setStatus(String stat) {
		if (stat != null) {
			// Change UI
			TextView text = (TextView) findViewById(R.id.status);
			text.setText(stat);
		}
	}

	public String executeRemoteCommand(String username, String password,
			String hostname, int port, String command) throws Exception {

		JSch jsch = new JSch();
		Session session = jsch.getSession(username, hostname, port);
		session.setPassword(password);

		// Avoid asking for key confirmation
		Properties prop = new Properties();
		prop.put("StrictHostKeyChecking", "no");
		session.setConfig(prop);

		session.connect();

		// SSH Channel
		ChannelExec channelssh = (ChannelExec) session.openChannel("exec");
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		channelssh.setOutputStream(baos);

		// Execute command
		channelssh.setCommand(command);
		channelssh.connect();

		channelssh.disconnect();

		String s = baos.toString();
		setStatus(s);
		return s;
	}

	void handleSendImage(Intent intent) {
		// Uri imageUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
		// if (imageUri != null) {
		// Update UI to reflect image being shared
		// }
	}

	void handleSendMultipleImages(Intent intent) {
		/*
		 * ArrayList<Uri> imageUris =
		 * intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM); if
		 * (imageUris != null) { // Update UI to reflect multiple images being
		 * shared }
		 */
	}

}
