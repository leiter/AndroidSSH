
package com.jgh.androidssh.overall;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.jgh.androidssh.R;
import com.jgh.androidssh.dialogs.SshConnectFragmentDialog;
import com.jgh.androidssh.domain.SessionUserInfo;
import com.jgh.androidssh.sshutils.ConnectionStatusListener;
import com.jgh.androidssh.sshutils.SessionController;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity implements ConnectionStatusListener,OnClickListener {

    private static final String TAG = "MainActivity";
    private TextView mConnectStatus;
    private SshEditText mCommandEdit;
    private Button mButton, mEndSessionBtn, mSftpButton;

    private String mLastLine;

    public MainActivity() {
        SessionController.getSessionController();
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_main);
        mButton = findViewById(R.id.enterbutton);
        mEndSessionBtn = findViewById(R.id.endsessionbutton);
        mSftpButton = findViewById(R.id.sftpbutton);
        mCommandEdit = findViewById(R.id.command);
        mConnectStatus = findViewById(R.id.connectstatus);
        // set onclicklistener
        mButton.setOnClickListener(this);
        mEndSessionBtn.setOnClickListener(this);
        mSftpButton.setOnClickListener(this);
        int connectionStatus = (SessionController.getSessionController().getSessionUserInfo() != null
                && SessionController.getSessionController().getSession().isConnected())
                ? R.string.connected : R.string.not_connected;

        mConnectStatus.setText(connectionStatus);

        //text change listener, for getting the current input changes.
        mCommandEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                String[] sr = editable.toString().split("\r\n");
                String s = sr[sr.length - 1];
                mLastLine = s;

            }
        });


        mCommandEdit.setOnEditorActionListener(
                new TextView.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        //Log.d(TAG, "editor action " + event);
                        if (isEditTextEmpty(mCommandEdit)) {
                            return false;
                        }

                        // run command
                        else {
                            if (event == null || event.getAction() != KeyEvent.ACTION_DOWN) {
                                return false;
                            }
                            // get the last line of terminal
                            String command = getLastLine();
                            mCommandEdit.AddLastInput(command);
                            if(!SessionController.getSessionController().executeCommand(mCommandEdit, command)){
                                makeToast(R.string.could_not_use_shell);
                            };
                            return false;
                        }
                    }
                }
        );


        loadUserListFromPrefs();
    }


    private void makeToast(int text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    private void startSftpActivity() {
        Intent intent = new Intent(this, FileListActivity.class);
        String[] info = {
                SessionController.getSessionController().getSessionUserInfo().getUser(),
                SessionController.getSessionController().getSessionUserInfo().getHost(),
                SessionController.getSessionController().getSessionUserInfo().getPassword()
        };

        intent.putExtra("UserInfo", info);
        startActivity(intent);
    }

    private String getLastLine() {
        int index = mCommandEdit.getText().toString().lastIndexOf("\n");
        if (index == -1) {
            return mCommandEdit.getText().toString().trim();
        }
        if (mLastLine == null) {
            Toast.makeText(this, "no text to process", Toast.LENGTH_LONG).show();
            return "";
        }
        String[] lines = mLastLine.split(Pattern.quote(mCommandEdit.getPrompt()));
        String lastLine = mLastLine.replace(mCommandEdit.getPrompt().trim(), "");
        Log.d(TAG, "command is " + lastLine + ", prompt is  " + mCommandEdit.getPrompt());
        return lastLine.trim();
    }

    private String getSecondLastLine() {

        String[] lines = mCommandEdit.getText().toString().split("\n");
        if (lines == null || lines.length < 2) return mCommandEdit.getText().toString().trim();

        else {
            int len = lines.length;
            String ln = lines[len - 2];
            return ln.trim();
        }
    }

    private boolean isEditTextEmpty(EditText editText) {
        return editText.getText() == null ||
                editText.getText().toString().equalsIgnoreCase("");
    }

    public void onClick(View v) {
        if (v == mButton) {
            showDialog();

        } else if (v == mSftpButton) {
            if (SessionController.isConnected()) {

                startSftpActivity();

            }
        } else if (v == this.mEndSessionBtn) {
            try {
                if (SessionController.isConnected()) {
                    SessionController.getSessionController().disconnect();
                }
            } catch (Throwable t) {
                Log.e(TAG, "Disconnect exception " + t.getMessage());
            }
        }
    }

    void showDialog() {

        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag("dialog");

        ft.addToBackStack(null);

        SshConnectFragmentDialog newFragment = SshConnectFragmentDialog.newInstance(null);
        newFragment.setListener(//new ConnectionStatusListener() {
          this
        //}
        );
        newFragment.show(ft, "dialog");
    }

    @Override
    public void onDisconnected() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectStatus.setText(R.string.not_connected);
            }
        });
    }

    @Override
    public void onConnected() {
        Log.e("onConnected","yes  ");

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                saveUserLogin();

                mConnectStatus.setText(R.string.connected);
            }
        });
    }

    private String getUserList() {
        List<SessionUserInfo> userInfos = SessionController.getSessionController().getUserInfos();

        StringBuilder stringBuilder = new StringBuilder("");
        if (userInfos != null)
            for (SessionUserInfo sUi :
                    userInfos) {
                stringBuilder.append(sUi.getUser()).append(",")
                        .append(sUi.getHost()).append(",")
                        .append(sUi.getPort()).append(",").append(sUi.getPassword()).append("###");
            }
        return stringBuilder.toString();
    }

    private void loadUserListFromPrefs() {

        SharedPreferences preferences = getSharedPreferences("userInfos", Context.MODE_PRIVATE);
        String[] users = preferences.getString("users", "").split("###");
        List<SessionUserInfo> result = new ArrayList<>();
        if (users.length>0)Log.e("MAIN_users","sdfs  " + users.length);
        if (users.length > 1) {
            for (String s : users) {
                String[] u = s.split(",");
                result.add(new SessionUserInfo(u[0], u[1], u[3], Integer.valueOf(u[2])));
            }
        }

        SessionController.getSessionController().setmUserInfos(result);
    }

    private void saveUserLogin() {
        SharedPreferences preferences =
                getSharedPreferences("userInfos", Context.MODE_PRIVATE);
        String payload = getUserList();
        Log.e("saveddd","no  " + payload);
        preferences.edit().putString("users", payload).apply();

    }


}
