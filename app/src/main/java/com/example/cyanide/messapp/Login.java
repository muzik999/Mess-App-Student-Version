package com.example.cyanide.messapp;


import com.example.cyanide.messpp.R;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import android.os.Handler;
import java.util.Map;
import java.util.HashMap;


public class Login extends Activity{

    private Button btnSignIn;
    private EditText etUserName, etPass;
    private String login_status;
    private String user_login_table;
    private String password_child, session_child;
    private String firebase_Url,database_Url;

    private String entered_user;
    private String entered_pass;


    private class firebase_async extends AsyncTask<String, Void, Void> {
        //An Async class to deal with the synchronisation of listener
        private Context async_context;
        private ProgressDialog pd;
        private String actual_pass;

        public firebase_async(Context context){
            this.async_context = context;
            pd = new ProgressDialog(async_context);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pd.setMessage("Logging in");
            pd.setCancelable(false);
            pd.show();
        }

        @Override
        protected Void doInBackground(final String... params) {

            final String entered_user = params[0];
            final String entered_pass = params[1];

            final Firebase ref = new Firebase(firebase_Url);
            final Object lock = new Object();


            ref.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {

                    synchronized (lock) {
                        login_status = "0";

                        for (DataSnapshot ds : dataSnapshot.getChildren()) {
                            Map<String, Object> existUser = (HashMap<String, Object>) ds.getValue();

                            if (entered_user.equals(ds.getKey())) {

                                actual_pass = existUser.get(password_child).toString();
                                int session_val = Integer.parseInt(existUser.get(session_child).toString());

                                if (entered_pass.equals(actual_pass)) {
                                    if (session_val == 0)
                                        login_status = "-1";

                                    else {
                                        login_status = "1";
                                        System.out.println("Successful match");
                                    }
                                    lock.notifyAll();
                                    break;
                                }
                            }
                        }
                        lock.notifyAll();
                    }
                }

                @Override
                public void onCancelled(FirebaseError firebaseError) {

                }
            });

            synchronized (lock){
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            //Handles the stuff after the synchronisation with the firebase listener has been achieved
            //The main UI is already idle by this moment

            super.onPostExecute(aVoid);
            System.out.println("After async: " + login_status);

            //Show the log in progress_bar for at least 2 seconds.
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {
                    pd.dismiss();
                    if (login_status.equals("1")) {

                        //Reset the session variable
                        Firebase updated_ref = new Firebase(firebase_Url).child(entered_user);
                        Map<String, Object> existUser = new HashMap<String, Object>();
                        Map<String, Object> user_args = new HashMap<String, Object>();

                        existUser.put(session_child, "0");
                        existUser.put(password_child, actual_pass);
                        updated_ref.updateChildren(existUser);

                        user_args.put("EXTRA_FireBase_Node_Ref", database_Url + updated_ref.getPath().toString());
                        user_args.put("EXTRA_Node_Session_Field", session_child);
                        user_args.put("EXTRA_Node_Password_Field", password_child);

                        Toast.makeText(getApplicationContext(), "You are successfully logged in", Toast.LENGTH_SHORT).show();
                        StaticUserMap.getInstance().setUserMap(existUser);
                        StaticUserMap.getInstance().setUserViewExtras(user_args);

                        Intent launchUser = new Intent(Login.this, UserView.class);
                        startActivity(launchUser);

                    } else if (login_status.equals("-1")) {
                        Toast.makeText(getApplicationContext(), "Maximum Login Limit Reached. Try Again Later", Toast.LENGTH_SHORT).show();
                        etPass.setText("");

                    } else if (login_status.equals("0")) {
                        Toast.makeText(getApplicationContext(), "Password/User combination doesn't match", Toast.LENGTH_SHORT).show();
                        etPass.setText("");
                    } else
                        Toast.makeText(getApplicationContext(), "Please Report the issue", Toast.LENGTH_SHORT).show();


                }
            }, 500);  // 500 milliseconds

        }
        //end firebase_async_class
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        btnSignIn = (Button) findViewById(R.id.btnSingIn);
        etUserName = (EditText) findViewById(R.id.etUserName);
        etPass = (EditText) findViewById(R.id.etPass);

        user_login_table = "login_data";
        password_child   = "password";
        session_child    = "session_valid";
        database_Url     = "https://sweltering-heat-4362.firebaseio.com/";
        firebase_Url     = database_Url + user_login_table;
        entered_user     = " ";
        entered_pass     = " ";

        Firebase.setAndroidContext(this);

        btnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                entered_user = etUserName.getText().toString();
                entered_pass = etPass.getText().toString();

                System.out.println("Entered id: " + entered_user);
                System.out.println("Entered Pass: " + entered_pass);

                //Let the main UI run independently of the async listener
                firebase_async authTask = new firebase_async(Login.this);
                authTask.execute(entered_user,entered_pass);

                //in this case the main UI does practically nothing
                //but the catch is that it's not waiting for anyone. Fully responsive
                System.out.println("Back in UI Thread: " + Thread.currentThread());
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_login, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //class ends

}

