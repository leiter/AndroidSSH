package com.jgh.androidssh.sshutils;

import android.os.Handler;
import android.util.Log;
import android.widget.EditText;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.SftpProgressMonitor;
import com.jgh.androidssh.domain.SessionUserInfo;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

public class SessionController {

    private static final String TAG = "SessionController";

    private Session mSession;

    private SessionUserInfo mSessionUserInfo;

    public List<SessionUserInfo> getUserInfos() {
        return mUserInfos;
    }

    public void setmUserInfos(List<SessionUserInfo> userInfos) {
        this.mUserInfos = userInfos;
    }

    private List<SessionUserInfo> mUserInfos;

    private Thread mThread;

    private final SftpController mSftpController = new SftpController();

    private final ShellController mShellController = new ShellController();

    private ConnectionStatusListener mConnectStatusListener;

    private static volatile SessionController sSessionController;

    private SessionController() {
    }

    public static SessionController getSessionController() {
        if (sSessionController == null) {
            synchronized (SessionController.class) {
                if (sSessionController == null) {
                    sSessionController = new SessionController();
                }
            }
        }
        return sSessionController;
    }

    public Session getSession() {
        return mSession;
    }

    private static boolean exists() {
        return sSessionController != null;
    }

    public static boolean isConnected() {
        Log.v(TAG, "session controller exists... " + exists());
        if (exists()) {
            Log.v(TAG, "disconnecting");
            return getSessionController().getSession() != null
                    && getSessionController().getSession().isConnected();
        }
        return false;
    }

    public void setUserInfo(SessionUserInfo sessionUserInfo) {
        mSessionUserInfo = sessionUserInfo;
        if(!mUserInfos.contains(sessionUserInfo)){
            mUserInfos.add(sessionUserInfo);
        }
    }

    public SessionUserInfo getSessionUserInfo() {
        return mSessionUserInfo;
    }

    public void connect() {
        if (mSession == null) {
            mThread = new Thread(new SshRunnable());
            mThread.start();
        } else if (!mSession.isConnected()) {
            mThread = new Thread(new SshRunnable());
            mThread.start();
        }
    }

    public SftpController getSftpController() {
        return mSftpController;
    }

    public void setConnectionStatusListener(ConnectionStatusListener csl) {
        mConnectStatusListener = csl;
    }

    public void uploadFiles(File[] files, SftpProgressMonitor spm) {
        mSftpController.new UploadTask(mSession, files, spm).execute();
    }

    public boolean downloadFile(String srcPath, String out, SftpProgressMonitor spm) throws JSchException, SftpException {
        mSftpController.new DownloadTask(mSession, srcPath, out, spm).execute();
        return true;
    }

    public void listRemoteFiles(TaskCallbackHandler taskCallbackHandler, String path) throws JSchException, SftpException {

        if (mSession == null || !mSession.isConnected()) {
            return;
        }
        mSftpController.lsRemoteFiles(mSession, taskCallbackHandler, path);
    }


    public void disconnect() throws IOException {

        if (mSession != null) {
            try {
                mShellController.disconnect();
            } catch (IOException e) {
                Log.e(TAG, "Exception closing shell controller. " + e.getMessage());
            }
            synchronized (mConnectStatusListener) {
                if (mConnectStatusListener != null) {
                    mConnectStatusListener.onDisconnected();
                }
            }

            mSession.disconnect();
        }
        if (mThread != null && mThread.isAlive()) {
            try {
                mThread.join();
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
        }

    }

    synchronized void disconnector(){
        if (mConnectStatusListener != null) {
            mConnectStatusListener.onDisconnected();
        }
    }

    public boolean executeCommand(Handler handler, EditText editText, ExecTaskCallbackHandler callback, String command) {
        if (mSession == null || !mSession.isConnected()) {
            return false;
        } else {

            try {
                mShellController.openShell(getSession(), handler, editText);

            } catch (Exception e) {
                Log.e(TAG, "Shell open exception " + e.getMessage());
                //TODO fix general exception catching
            }

            synchronized (mShellController) {
                mShellController.writeToOutput(command);
            }
        }

        return true;
    }

    public class SshRunnable implements Runnable {

        public void run() {
            JSch jsch = new JSch();
            mSession = null;
            try {
                mSession = jsch.getSession(mSessionUserInfo.getUser(), mSessionUserInfo.getHost(),
                        mSessionUserInfo.getPort()); // port 22

                mSession.setUserInfo(mSessionUserInfo);

                Properties properties = new Properties();
                properties.setProperty("StrictHostKeyChecking", "no");
                mSession.setConfig(properties);
                mSession.connect();

            } catch (JSchException jex) {
                Log.e(TAG, "JschException: " + jex.getMessage() +
                        ", Fail to get session " + mSessionUserInfo.getUser() +
                        ", " + mSessionUserInfo.getHost());
            } catch (Exception ex) {
                Log.e(TAG, "Exception:" + ex.getMessage());
            }

            Log.d("SessionController", "Session connected? " + mSession.isConnected());

            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        //keep track of connection status
                        try {
                            Thread.sleep(2000);
                            if (mConnectStatusListener != null) {
                                if (mSession.isConnected()) {
                                    mConnectStatusListener.onConnected();
                                } else mConnectStatusListener.onDisconnected();
                            }
                        } catch (InterruptedException e) {

                        }
                    }
                }
            }).start();
        }
    }
}
