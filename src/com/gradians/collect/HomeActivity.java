package com.gradians.collect;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class HomeActivity extends Activity implements IConstants, ITaskResult {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);        
        checkAuth();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        if (item.getItemId() == R.id.action_sign_out)
            initiateAuthActivity();
        else if (item.getItemId() == R.id.action_settings) {
            String vers = null;
            try {
                vers = String.format("Scanbot %s", getPackageManager()
                    .getPackageInfo(getPackageName(), 0).versionName);
            } catch (Exception e) { }
            String[] settings = new String[2];
            settings[0] = "Current Balance: " + balance + "₲";
            settings[1] = "Version: " + vers;
            AlertDialog.Builder builder = new AlertDialog.Builder(this, 
                R.style.RobotoDialogTitleStyle);
            builder.setTitle("Settings").setItems(settings, null);
            builder.show();
        } else {
            checkAuth();
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    public void onTaskResult(int requestCode, int resultCode, String resultData) {
        if (peedee != null) 
            peedee.dismiss();
        try {
            if (resultCode == RESULT_OK) {
                initialize(resultData);
            } else if (resultCode == Activity.RESULT_CANCELED) {
                Toast.makeText(getApplicationContext(), 
                    "No Internet :/ soldiering on regardless", 
                    Toast.LENGTH_LONG).show();
                initialize(retrieveResponse());
            } else if (resultCode == RESULT_FIRST_USER) {
                this.finish();
            }
        } catch (Exception e) {
            handleError("Oops, Verify auth task failed", e.getMessage());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            try {
                String json = data.getStringExtra(TAG);
                initialize(json);
            } catch (Exception e) {
                handleError("Oops, Auth activity request failed",
                        e.getMessage());
            }
        } else if (resultCode != Activity.RESULT_CANCELED) {
            Toast.makeText(getApplicationContext(),
                    "Oops.. auth check failed. Please try again",
                    Toast.LENGTH_SHORT).show();
        } else {
            this.finish();
        }
    }

    public void onClick(View v) {
        Intent intent = null;
        try {
            switch (v.getId()) {
            case R.id.btnSchool:
                intent = new Intent(getApplicationContext(),
                    com.gradians.collect.TeacherActivity.class);
                break;
            case R.id.btnAsk:
                break;
            case R.id.btnBrowse:
                intent = new Intent(getApplicationContext(),
                    com.gradians.collect.PracticeActivity.class);
            }
            intent.putExtra(PZL_KEY, potd);
            intent.putExtra(TOPICS_KEY, topics);
            startActivity(intent);
        } catch (Exception e) {
            handleError("Oops, Activity launch failed",
                e.getMessage());
        }
    }
    
    private void checkAuth() {
        SharedPreferences prefs = getSharedPreferences(TAG, Context.MODE_PRIVATE);
        String token = prefs.getString(TOKEN_KEY, null);
        if (token == null) {
            initiateAuthActivity();
        } else {
            String email = prefs.getString(EMAIL_KEY, null);
            initiateVerifActivity(email, token);
        }
    }

    private void initiateAuthActivity() {
        resetPreferences();
        Intent checkAuthIntent = new Intent(this, 
            com.gradians.collect.LoginActivity.class);
        checkAuthIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivityForResult(checkAuthIntent, 
            ITaskResult.AUTH_ACTIVITY_REQUEST_CODE);
    }
    
    private void initiateVerifActivity(String email, String token) {
        peedee = new ProgressDialog(this, R.style.RobotoDialogTitleStyle);
        peedee.setTitle("Refreshing");
        peedee.setMessage("Please wait...");
        peedee.setIndeterminate(true);
        peedee.setIcon(ProgressDialog.STYLE_SPINNER);
        peedee.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        peedee.show();
        
        String refreshUrl = "http://%s/tokens/verify?email=%s&token=%s";
        Uri src = Uri.parse(String.format(refreshUrl, WEB_APP_HOST_PORT, email, token));
        Download download = new Download(null, src, null);
        new HttpCallsAsyncTask(this, REFRESH_WS_TASK_REQUEST_CODE).
            execute(new Download[] { download });
    }

    private void initialize(String json) throws Exception {
        JSONParser jsonParser = new JSONParser();
        JSONObject respObject = (JSONObject)jsonParser.parse(json);
        
        String name, email, token, id;
        boolean enrolled;
        token = (String)respObject.get(TOKEN_KEY);
        name = (String)respObject.get(NAME_KEY);
        email = (String)respObject.get(EMAIL_KEY);
        id = String.valueOf((Long)respObject.get(ID_KEY));
        potd = (String)respObject.get(PZL_KEY);
        balance = ((Long)respObject.get(BALANCE_KEY)).intValue();
        enrolled = (Boolean)respObject.get(ENRL_KEY);
        
        File studentDir = new File(getExternalFilesDir(null), 
            email.replace('@', '.'));
        studentDir.mkdir();
        (new File(studentDir, "problems")).mkdir();
        (new File(studentDir, "files")).mkdir();
        
        SharedPreferences prefs = getSharedPreferences(TAG, Context.MODE_PRIVATE);
        Editor edit = prefs.edit();
        edit.putString(NAME_KEY, name);
        edit.putString(EMAIL_KEY, email);
        edit.putString(TOKEN_KEY, token);
        edit.putString(ID_KEY, id);
        edit.putString(DIR_KEY, studentDir.getPath());
        edit.putInt(BALANCE_KEY, balance);
        edit.commit();        
        setTitle(String.format("Hi %s", name));
        
        HugeButton school, ask, browse;
        school = (HugeButton)findViewById(R.id.btnSchool);
        ask = (HugeButton)findViewById(R.id.btnAsk);
        browse = (HugeButton)findViewById(R.id.btnBrowse);
        
        browse.setText(R.string.home_button_browse, R.drawable.ic_action_rotate_right);
        school.setText(R.string.home_button_teacher, R.drawable.ic_action_sent);
        school.setEnabled(enrolled);
        ask.setText(R.string.home_button_aaq, R.drawable.ic_action_chat);
        ask.setEnabled(false);
        
        topics = getTopics((JSONArray)respObject.get(TOPICS_KEY)); 
        saveResponse(json);
    }

    private void resetPreferences() {
        SharedPreferences prefs = getSharedPreferences(TAG, Context.MODE_PRIVATE);
        Editor edit = prefs.edit();
        edit.clear();
        edit.commit();
    }
    
    private void saveResponse(String json) throws Exception {
        FileOutputStream fos = 
            openFileOutput(filename, Context.MODE_PRIVATE);
        fos.write(json.getBytes());
        fos.close();
    }
    
    private String retrieveResponse() throws Exception {
        FileInputStream fis = openFileInput(filename);
        StringBuffer sb = new StringBuffer();
        byte[] buffer = new byte[1024];
        int bytesRead = 0;
        while ((bytesRead = fis.read(buffer)) != -1) {
            sb.append(new String(buffer, 0, bytesRead));
        }
        fis.close();
        return sb.toString();
    }
    
    private Topic[] getTopics(JSONArray items) {
        ArrayList<Topic> topics = new ArrayList<Topic>();
        JSONObject item;
        for (int i = 0; i < items.size(); i++) {
            item = (JSONObject)items.get(i);            
            topics.add(new Topic(
                (Long)item.get(ID_KEY),
                (String)item.get(NAME_KEY),
                (Long)item.get(VERT_ID_KEY),
                (String)item.get(VERT_NAME_KEY)));
        }
        return topics.toArray(new Topic[topics.size()]);
    }
    
    private void handleError(String error, String message) {
        Log.e(TAG, error + " " + message);
    }
 
    private Topic[] topics;
    private int balance;
    private String potd;
    private ProgressDialog peedee;
    private final String filename = "init.json";
}

class HugeButton extends RelativeLayout {

    public HugeButton (Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater)context.
            getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            inflater.inflate(R.layout.layout_btn_huge, this, true);
    }
    
    public void setText(int textId, int drawable) {
        ((TextView)findViewById(R.id.tvLabel)).setText(textId);
        ((ImageView)findViewById(R.id.ivIcon)).setImageResource(drawable);
    }
    
    public void setText(String text, int drawable) {
        ((TextView)findViewById(R.id.tvLabel)).setText(text);
        ((ImageView)findViewById(R.id.ivIcon)).setImageResource(drawable);
    }
    
    public void setCount(int count, String text, int drawable) {
        ((TextView)findViewById(R.id.tvLabel)).setText(text);
        ((TextView)findViewById(R.id.tvCount)).setText(String.valueOf(count));
        ((TextView)findViewById(R.id.tvCount)).setVisibility(View.VISIBLE);
        ((ImageView)findViewById(R.id.ivIcon)).setImageResource(drawable);
    }
    
}
