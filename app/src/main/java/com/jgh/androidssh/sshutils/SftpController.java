package com.jgh.androidssh.sshutils;

import android.os.AsyncTask;
import android.util.Log;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.SftpProgressMonitor;

import java.io.File;
import java.util.Vector;

/**
 * Controller class for SFTP functions. Performs SFTP
 * ls, get, put commands between local device and remote SSH
 * server. For each process a new sftpchannel is opened and closed
 * after completion.
 */
public class SftpController {

    private static final String TAG = "SftpController";

    private static String mCurrentPath = "/";

    SftpController() {

    }

    public SftpController(String path) {
        mCurrentPath = path;
    }

    public void resetPathToRoot() {
        mCurrentPath = "/";
    }

    public String getPath() {
        return mCurrentPath;
    }

    public void setPath(String path) {
        mCurrentPath = path;
    }

    public void appendToPath(String relPath) {
        if (mCurrentPath == null) {
            mCurrentPath = relPath;
        } else mCurrentPath += relPath;
    }

    public void disconnect() {
        //nothing yet
    }

    public static class UploadTask extends AsyncTask<Void, Void, Boolean> {

        private Session mSession;

        private SftpProgressMonitor mProgressDialog;

        private File[] mLocalFiles;

        UploadTask(Session session, File[] localFiles, SftpProgressMonitor spd) {

            mProgressDialog = spd;
            mLocalFiles = localFiles;
            mSession = session;
        }

        @Override
        protected void onPreExecute() {
            //nothing
        }

        @Override
        protected Boolean doInBackground(Void... voids) {

            try {
                uploadFiles(mSession, mLocalFiles, mProgressDialog);

                return true;
            } catch (JSchException e) {
                e.printStackTrace();
                Log.e(TAG, "JSchException " + e.getMessage());
                return false;
            } catch (SftpException e) {
                e.printStackTrace();
                Log.e(TAG, "SftpException " + e.getMessage());
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean b) {
            //TODO
        }
    }

    public static void invokeUpload(Session session, File[] localFiles, SftpProgressMonitor spd) {
        new UploadTask(session, localFiles, spd).execute();
    }

    static void uploadFiles(Session session, File[] localFiles, SftpProgressMonitor spm)
            throws JSchException, SftpException {

        if (session == null || !session.isConnected()) {
            session.connect();
        }

        Channel channel = session.openChannel("sftp");
        channel.setInputStream(null);
        channel.connect();
        ChannelSftp channelSftp = (ChannelSftp) channel;
        channelSftp.cd(mCurrentPath);
        for (File file : localFiles) {
            channelSftp.put(file.getPath(), file.getName(), spm, ChannelSftp.APPEND);
        }

        channelSftp.disconnect();
    }

    public void lsRemoteFiles(Session session, TaskCallbackHandler taskCallbackHandler, String path) {
        mCurrentPath = path == null || "".equals(path) ? mCurrentPath : mCurrentPath + path + "/";
        new LsTask(session, taskCallbackHandler).execute();
    }

    public static void invokeDownload(Session session, String srcPath, String out, SftpProgressMonitor spm){
        new DownloadTask(session, srcPath, out, spm).execute();
    }

    private static class LsTask extends AsyncTask<Void, Void, Boolean> {

        private Vector<ChannelSftp.LsEntry> mRemoteFiles;

        private TaskCallbackHandler mTaskCallbackHandler;

        private Session mSession;

        LsTask(Session session, TaskCallbackHandler tch) {
            mSession = session;
            mTaskCallbackHandler = tch;
        }

        @Override
        protected void onPreExecute() {
            if (mTaskCallbackHandler != null)
                mTaskCallbackHandler.OnBegin();
        }

        @Override
        protected Boolean doInBackground(Void... voids) {

            Log.d(TAG, "current path is " + mCurrentPath);

            Channel channel;
            try {
                mRemoteFiles = null;
                channel = mSession.openChannel("sftp");
                channel.setInputStream(null);
                channel.connect();
                ChannelSftp channelsftp = (ChannelSftp) channel;
                String path = mCurrentPath == null ? "/" : mCurrentPath;
                mRemoteFiles = channelsftp.ls(path);
                if (mRemoteFiles == null) {
                    Log.d(TAG, "remote file list is null");
                } else {
                    for (ChannelSftp.LsEntry e : mRemoteFiles) {
                        Log.v(TAG, " file " + e.getFilename());
                    }
                }
            } catch (Exception e) {
                Log.v(TAG, "sftprunnable exptn " + e.getCause());
                return false;
            }
            channel.disconnect();

            return true;
        }


        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                if (mTaskCallbackHandler != null) {
                    mTaskCallbackHandler.onTaskFinished(mRemoteFiles);
                }
            } else {
                if (mTaskCallbackHandler != null) {
                    mTaskCallbackHandler.onFail();
                }
            }
        }
    }

    public static void downloadFile(Session session, String srcPath, String out, SftpProgressMonitor spm) throws JSchException, SftpException {
        if (session == null || !session.isConnected()) {
            session.connect();
        }

        Channel channel = session.openChannel("sftp");
        ChannelSftp sftpChannel = (ChannelSftp) channel;
        sftpChannel.connect();
        sftpChannel.get(srcPath, out, spm, ChannelSftp.OVERWRITE);
        sftpChannel.disconnect();
    }

    public static class DownloadTask extends AsyncTask<Void, Void, Boolean> {

        Session mSession;
        String mSrcPath;
        String mOut;
        SftpProgressMonitor mSpm;

        DownloadTask(Session session, String srcPath, String out, SftpProgressMonitor spm) {
            mSession = session;
            mSrcPath = srcPath;
            mOut = out;
            mSpm = spm;
        }

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            Boolean result = false;
            try {
                Log.v(TAG, " path " + mSrcPath + ", out path " + mOut);
                downloadFile(mSession, mCurrentPath + mSrcPath, mOut, mSpm);
                result = true;

            } catch (Exception e) {
                Log.e(TAG, "EXCEPTION " + e.getMessage());

            }
            return result;

        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success == null) return;
            if (success) {
                mSpm.end();
            }
        }
    }
}
