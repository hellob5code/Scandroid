package com.gradians.collect;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

/**
 * Activity which displays a login screen to the user, offering registration as
 * well.
 */
public class LoginActivity extends Activity implements IConstants {
    
    /** POST parameter name for the user's account name */
    private static final String 
        PARAM_EMAIL = "email", 
        PARAM_PASSWORD = "password",
        PARAM_SIG = "signature";

    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask         mAuthTask         = null;

    // Values for email and password at the time of the login attempt.
    private String                mEmail;
    private String                mPassword;

    // UI references.
    private EditText              mEmailView;
    private EditText              mPasswordView;
    private View                  mLoginFormView;
    private View                  mLoginStatusView;
    private TextView              mLoginStatusMessageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);

        // Set up the login form.
        mEmailView = (EditText) findViewById(R.id.email);
        mEmailView.setText(mEmail);

        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView
                .setOnEditorActionListener(new TextView.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView textView, int id,
                            KeyEvent keyEvent) {
                        if (id == R.id.login || id == EditorInfo.IME_NULL) {
                            attemptLogin();
                            return true;
                        }
                        return false;
                    }
                });

        mLoginFormView = findViewById(R.id.login_form);
        mLoginStatusView = findViewById(R.id.login_status);
        mLoginStatusMessageView =
                (TextView) findViewById(R.id.login_status_message);

        findViewById(R.id.sign_in_button).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        attemptLogin();
                    }
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.login, menu);
        return true;
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {        
        if (requestCode == ITaskResult.REGISTRATION_ACTIVITY_REQUEST_CODE) {
            if (resultCode == RESULT_FIRST_USER) {
                finish();
            }
        } else if (requestCode == ITaskResult.FORGOT_PWD_ACTIVITY_REQUEST_CODE) {
            if (resultCode == RESULT_FIRST_USER) {
                String email = data.getStringExtra(EMAIL_KEY);
                mEmailView.setText(email);
            }            
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)  {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.ECLAIR
                && keyCode == KeyEvent.KEYCODE_BACK
                && event.getRepeatCount() == 0) {
            // Take care of calling this method on earlier versions of
            // the platform where it doesn't exist.
            onBackPressed();
        }
        
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        // This will be called either automatically for you on 2.0
        // or later, or by the code above on earlier versions of the
        // platform.
        this.setResult(RESULT_CANCELED);
        super.onBackPressed();
        return;
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    public void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        mEmail = mEmailView.getText().toString();
        mPassword = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password.
        if (TextUtils.isEmpty(mPassword)) {
            mPasswordView.setError(getString(R.string.error_field_required));
            focusView = mPasswordView;
            cancel = true;
        } else if (mPassword.length() < 6) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(mEmail)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        } else if (!mEmail.contains("@")) {
            mEmailView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            mLoginStatusMessageView.setText(R.string.login_progress_signing_in);
            showProgress(true);
            mAuthTask = new UserLoginTask();
            mAuthTask.execute(WEB_APP_HOST_PORT);
        }
    }
    
    public void launchForgotPasswordActivity(View view) {
        Intent registration = new Intent(getApplicationContext(),
            com.gradians.collect.ForgotPasswordActivity.class);
        startActivityForResult(registration, 
            ITaskResult.FORGOT_PWD_ACTIVITY_REQUEST_CODE);
    }
    
    public void launchRegistrationActivity(View view) {
        Intent registration = new Intent(getApplicationContext(),
            com.gradians.collect.RegistrationActivity.class);
        startActivityForResult(registration, 
        ITaskResult.REGISTRATION_ACTIVITY_REQUEST_CODE);
    }
    
    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime =
                    getResources().getInteger(
                            android.R.integer.config_shortAnimTime);

            mLoginStatusView.setVisibility(View.VISIBLE);
            mLoginStatusView.animate().setDuration(shortAnimTime)
                    .alpha(show ? 1 : 0)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mLoginStatusView.setVisibility(show ? View.VISIBLE
                                    : View.GONE);
                        }
                    });

            mLoginFormView.setVisibility(View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime)
                    .alpha(show ? 0 : 1)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mLoginFormView.setVisibility(show ? View.GONE
                                    : View.VISIBLE);
                        }
                    });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mLoginStatusView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLoginTask extends AsyncTask<String, Void, String> {
        
        protected int httpResponseCode = 0;
        
        @Override
        protected String doInBackground(String... params) {
            String result = null;
            String hostport = params[0];
            try {                
                boolean hasMonochrome = false;
                Camera c = Camera.open();
                if (c != null) {
                    for (String s : c.getParameters().getSupportedColorEffects()) {
                        if (s.equals(Camera.Parameters.EFFECT_MONO)) {
                            hasMonochrome = true; break;
                        }                        
                    }
                }
                c.release();
                PackageManager pm = getApplicationContext().getPackageManager();
                String signature = String.format("OS=Android %s; AppVers=%s; Autofocus=%s; Monchrome=%s", 
                    Build.VERSION.SDK_INT,
                    pm.getPackageInfo(getApplicationContext().getPackageName(), 0).versionName,
                    pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_AUTOFOCUS), hasMonochrome);
                
                String charset = Charset.defaultCharset().name();
                URL createToken = new URL(String.format("http://%s/tokens", hostport));
                String authParams = String.format("%s=%s&%s=%s&%s=%s", 
                        PARAM_EMAIL, URLEncoder.encode(mEmail, charset),
                        PARAM_PASSWORD, URLEncoder.encode(mPassword, charset),
                        PARAM_SIG, URLEncoder.encode(signature, charset));
                HttpURLConnection conn = (HttpURLConnection)createToken.openConnection();
                conn.setDoOutput(true); // Triggers HTTP POST
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=" + charset);
                conn.setRequestProperty("Cache-Control", "no-cache");
                conn.getOutputStream().write(authParams.getBytes(charset));
                conn.getOutputStream().close();

                httpResponseCode = conn.getResponseCode();
                InputStream istream = conn.getInputStream();
                BufferedReader ireader = new BufferedReader(new InputStreamReader(istream));
                result = ireader.readLine();
            } catch (Exception e){ }
            return result;
        }

        @Override
        protected void onPostExecute(final String result) {
            mAuthTask = null;
            showProgress(false);
            if (httpResponseCode == 0) {
                mPasswordView.setError(getString(R.string.error_network_down));
            } else if (httpResponseCode == HttpURLConnection.HTTP_OK) {
                final Intent intent = new Intent();
                intent.putExtra(TAG, result);
                setResult(RESULT_OK, intent);
                finish();
            } else {
                int errorId;
                switch (httpResponseCode) {
                case 550:
                    errorId = R.string.error_invalid_email;
                default:
                    errorId = R.string.error_incorrect_password;
                }
                mPasswordView.setError(getString(errorId));
                mPasswordView.requestFocus();
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
        
    }
}
