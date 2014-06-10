package com.gradians.collect;

import java.io.File;
import java.net.URL;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

public class ListActivity extends Activity implements OnItemClickListener, ITaskResult, IConstants {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        checkAuth();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        if (manifest != null) {
            try {
                manifest.commit();
            } catch (Exception e) {
                handleError("Oops.. persist file thing failed", e.getMessage());
            }
        }
    }
    
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == AUTH_ACTIVITY_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                try {
                    manifest = new QuizManifest(this.getApplicationContext(), 
                            data.getStringExtra(TAG));
                    setPreferences(manifest);
                    setManifest(manifest);
                } catch (Exception e) {
                    handleError("Oops, Auth activity request failed", 
                            e.getMessage());
                }                
            } else if (resultCode != Activity.RESULT_CANCELED){
                Toast.makeText(getApplicationContext(),
                        "Oops.. auth check failed. Please try again",
                        Toast.LENGTH_SHORT).show();
            } else {
                this.finish();
            }
        } else if (requestCode == FLOW_ACTIVITY_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                try {
                    String[] name_state_ids = data.getStringArrayExtra(TAG);
                    int itemPosition = 0;
                    for (String name_state_id : name_state_ids) {
                        short state;
                        String[] tokens = name_state_id.split("-");
                        state = Short.parseShort(tokens[1]);
                        manifest.update(selectedQuizPosition, itemPosition++, state);
                    }
                } catch (Exception e) {
                    handleError("Oops, Flow activity request failed", 
                            e.getMessage());
                }
            } else if (resultCode != Activity.RESULT_CANCELED){
                Toast.makeText(getApplicationContext(),
                        "Oops we had an error, you may have lost work :/",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onTaskResult(int requestCode, int resultCode, String resultData) {
        if (requestCode == VERIFY_AUTH_TASK_RESULT_CODE) {
            if (peedee != null) peedee.dismiss();
            if (resultCode == RESULT_OK) {  
                try {
                    manifest = new QuizManifest(this.getApplicationContext(), 
                            resultData);
                    setManifest(manifest);
                } catch (Exception e) { 
                    handleError("Oops, Verify auth task failed", e.getMessage());
                }
            } else {
                initiateAuthActivity();
            }
        }
    }
    
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Quij quiz = (Quij)manifest.getItem(position);
        selectedQuizPosition = position;
        Question[] questions = quiz.getQuestions();
        String[] name_state_ids = new String[questions.length];
        for (int i = 0; i < questions.length; i++) {
            name_state_ids[i] = questions[i].getNameStateId();
        }
        Intent flowIntent = new Intent(this.getApplicationContext(), 
                com.gradians.collect.FlowActivity.class);
        flowIntent.putExtra(TAG_ID, name_state_ids);
        flowIntent.putExtra(TAG, this.studentDir.getPath());
        startActivityForResult(flowIntent, FLOW_ACTIVITY_REQUEST_CODE);
    }

    private void setManifest(QuizManifest manifest) {
        setTitle(String.format("Hi %s", manifest.getName()));
        ListView lv = (ListView)this.findViewById(R.id.lvQuiz);
        if (manifest.getCount() > 0) {
            lv.setAdapter(manifest);
            lv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            lv.setOnItemClickListener(this);
        }        
        mkdirs(manifest.getEmail());
        triggerAllDownloads();
    }
    
    private void triggerDownloads(DownloadMonitor dlm, Quij quiz) {
        Question[] questions = quiz.getQuestions();
        for (Question question : questions) {
            Uri src, dest;
            File image = null;
            if (question != null) {
                switch (question.getState()) {
                case GRADED:
                case RECEIVED:
                    image = new File(solutionsDir, question.getId()); 
                    if (!image.exists()) {                        
                        src = Uri.parse(String.format(URL, BANK_HOST_PORT, "vault", 
                                question.getImgLocn() + "/pg-1.jpg"));
                        dest = Uri.fromFile(image);
                        dlm.add(question.getId(), src, dest);
                    }
                case SENT:
                    image = new File(answersDir, question.getId());
                    if (!image.exists()) {
                        src = Uri.parse(String.format(URL, BANK_HOST_PORT, "locker", 
                                question.getScanLocn()));
                        dest = Uri.fromFile(image);
                        dlm.add(question.getId(), src, dest);
                    }
                    break;
                case CAPTURED:
                    image = new File(answersDir, question.getId());
                    if (!image.exists()) {
                        question.setState(DOWNLOADED);
                    }
                case DOWNLOADED:
                case WAITING:
                    image = new File(questionsDir, question.getId());
                    if (!image.exists()) {
                        src = Uri.parse(String.format(URL, BANK_HOST_PORT, "vault", 
                                question.getImgLocn() + "/notrim.jpg"));
                        dest = Uri.fromFile(image);
                        dlm.add(question.getId(), src, dest);
                    }
                    if (question.getState() == WAITING) {
                        question.setState(DOWNLOADED);
                    }
                }
            }
        }
    }
    
    private void triggerAllDownloads() {
        DownloadMonitor dlm = new DownloadMonitor(this);        
        for (int j = 0; j < manifest.getCount(); j++) {
            Quij quiz = (Quij)manifest.getItem(j);
            triggerDownloads(dlm, quiz);
        }
        dlm.start("Synchronizing Files", "Please wait...");
    }    

    private void checkAuth() {
        SharedPreferences prefs = getSharedPreferences(TAG, 
                Context.MODE_PRIVATE);        
        String email = prefs.getString(EMAIL_KEY, null);
        String token = prefs.getString(TOKEN_KEY, null);        
        if (token == null) {
            initiateAuthActivity();
        } else {
            String urlString = String.format(
                    "http://%s/tokens/verify?email=%s&token=%s",
                    WEB_APP_HOST_PORT, email, token);
            try {
                peedee = ProgressDialog.show(this, "Initializing", 
                        "Please wait...");
                peedee.setIndeterminate(true);
                peedee.setIcon(ProgressDialog.STYLE_SPINNER);        
                URL[] urls = { new URL(urlString) };
                new HttpCallsAsyncTask(this, this, 
                        VERIFY_AUTH_TASK_RESULT_CODE).execute(urls);
            } catch (Exception e) {
                handleError("Auth Check Failed", e.getMessage());
            }
        }
    }

    private void initiateAuthActivity() {
        resetPreferences();
        Intent checkAuthIntent = new Intent(this, 
                com.gradians.collect.LoginActivity.class);
        checkAuthIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivityForResult(checkAuthIntent, AUTH_ACTIVITY_REQUEST_CODE);
    }

    private void setPreferences(QuizManifest manifest) {
        SharedPreferences prefs = this.getSharedPreferences(TAG, 
                Context.MODE_PRIVATE);
        Editor edit = prefs.edit();
        edit.putString(TOKEN_KEY, manifest.getAuthToken());
        edit.putString(NAME_KEY, manifest.getName());
        edit.putString(EMAIL_KEY, manifest.getEmail());
        edit.commit();       
    }
    
    private void resetPreferences() {
        SharedPreferences prefs = getSharedPreferences(TAG, 
                Context.MODE_PRIVATE);
        Editor edit = prefs.edit();
        edit.clear();
        edit.commit();        
    }

    private void mkdirs(String studentDirName) {
        (studentDir = new File(this.getExternalFilesDir(null), studentDirName)).mkdir();
        (questionsDir = new File(studentDir, QUESTIONS_DIR_NAME)).mkdir();
        (answersDir = new File(studentDir, ANSWERS_DIR_NAME)).mkdir();
        (solutionsDir = new File(studentDir, SOLUTIONS_DIR_NAME)).mkdir();
        (new File(studentDir, FEEDBACK_DIR_NAME)).mkdir();
        (new File(studentDir, UPLOAD_DIR_NAME)).mkdir();
    }
    
    
    private void handleError(String error, String message) {
        Log.d(TAG, error + " " + message);
    }

    private int selectedQuizPosition;
    private QuizManifest manifest;
    private File studentDir, questionsDir, answersDir, solutionsDir;    
    ProgressDialog peedee;
    
    private final String URL = "http://%s/%s/%s";    

}