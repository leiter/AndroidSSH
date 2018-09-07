package com.jgh.androidssh.dialogs;

import android.app.Activity;
import android.app.DialogFragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import com.jgh.androidssh.R;
import com.jgh.androidssh.domain.SessionUserInfo;
import com.jgh.androidssh.sshutils.ConnectionStatusListener;
import com.jgh.androidssh.sshutils.SessionController;

import java.util.List;


public class SshConnectFragmentDialog extends DialogFragment
        implements View.OnClickListener {

    private static final String USER_INFO_LIST = "userInfoList";
    private List<SessionUserInfo> userInfoList;
    private EditText mUserEdit;
    private EditText mHostEdit;
    private EditText mPasswordEdit;
    private EditText mPortNumEdit;
    private Button mButton;
    private ConnectionStatusListener mListener;

    public void setListener(ConnectionStatusListener listenr) {
        mListener = listenr;
    }

    public static SshConnectFragmentDialog newInstance(@Nullable SessionUserInfo info) {
        SshConnectFragmentDialog fragment = new SshConnectFragmentDialog();
        if (info != null) {
            Bundle userInfo = new Bundle();
            userInfo.putBundle(USER_INFO_LIST, userInfo);
        }
        return fragment;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

    }

    private void setupSpinner() {
        Activity activity = getActivity();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (SessionController.getSessionController().getUserInfos() != null) {
            userInfoList = SessionController.getSessionController().getUserInfos();
            setupSpinner();
        }

        View v = inflater.inflate(R.layout.fragment_main, container, false);
        mUserEdit = v.findViewById(R.id.username);
        mHostEdit = v.findViewById(R.id.hostname);
        mPasswordEdit = v.findViewById(R.id.password);
        mPortNumEdit = v.findViewById(R.id.portnum);
        mButton = v.findViewById(R.id.enterbutton);
        mButton.setOnClickListener(this);

        Spinner spinner = v.findViewById(R.id.user_selector);
        spinner.setVisibility(View.VISIBLE);
        spinner.setAdapter(new UserSpinnerAdapter(userInfoList));
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                SessionUserInfo info = userInfoList.get(i);
                if (info != null) {
                    mUserEdit.setText(info.getUser());
                    mHostEdit.setText(info.getHost());
                    mPasswordEdit.setText(info.getPassword());
                    mPortNumEdit.setText(String.valueOf(info.getPort()));
                    // maybe close ime
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        setText();
        return v;
    }

    private void setText(){
        mUserEdit.setText(  R.string.myUsername);
        mHostEdit.setText(R.string.myHostname);
        mPasswordEdit.setText(R.string.myPassword);
        mPortNumEdit.setText(R.string.myPortPassword);
    }

    private boolean isEditTextEmpty(EditText editText) {
        return editText.getText() == null ||
                editText.getText().toString().equalsIgnoreCase("");
    }

    @Override
    public void onClick(View v) {

        if (v.getId() == R.id.enterbutton) {
            if (isEditTextEmpty(mUserEdit) || isEditTextEmpty(mHostEdit)
                    || isEditTextEmpty(mPasswordEdit) || isEditTextEmpty(mPortNumEdit)) {
                return;
            }
            int port = Integer.valueOf(mPortNumEdit.getText().toString());
            SessionUserInfo mSUI = new SessionUserInfo(mUserEdit.getText().toString().trim(), mHostEdit.getText()
                    .toString().trim(),
                    mPasswordEdit.getText().toString().trim(), port);

            SessionController.getSessionController().setUserInfo(mSUI);
            SessionController.getSessionController().connect();

            if (mListener != null)
                SessionController.getSessionController().setConnectionStatusListener(mListener);
            dismiss();
        }
    }

}


