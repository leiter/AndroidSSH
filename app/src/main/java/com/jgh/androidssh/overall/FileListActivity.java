package com.jgh.androidssh.overall;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.DragEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnDragListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.SftpProgressMonitor;
import com.jgh.androidssh.R;
import com.jgh.androidssh.adapters.LocaleFileListAdapter;
import com.jgh.androidssh.adapters.RemoteFileListAdapter;
import com.jgh.androidssh.sshutils.SessionController;
import com.jgh.androidssh.sshutils.TaskCallbackHandler;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Vector;


/**
 * Activity to list files. Uploads chosen files to server (SFTP) using an
 * AsyncTask.
 *
 * @author Jonathan Hough
 * @since 7 Dec 2012
 */
public class FileListActivity extends Activity implements OnItemClickListener, OnClickListener {

    private static final String TAG = "FileListActivity";
    private ArrayList<File> mFilenames = new ArrayList<>();
    private GridView mLocalGridView;
    private GridView mRemoteGridView;
    private LocaleFileListAdapter mLocaleFileListAdapter;
    private RemoteFileListAdapter mRemoteFileListAdapter;
    private File mRootFile;
    private SessionController mSessionController;
    private boolean mIsProcessing = false;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        setContentView(R.layout.activity_filelistactivity);
        mLocalGridView = findViewById(R.id.listview);
        mRemoteGridView = findViewById(R.id.remotelistview);
        // Get external storage
        mRootFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        mFilenames.addAll(Arrays.asList(mRootFile.listFiles()));

        mLocaleFileListAdapter = new LocaleFileListAdapter(this, mFilenames);

        mLocalGridView.setAdapter(mLocaleFileListAdapter);
        mLocalGridView.setOnItemClickListener(this);
        //----------------- buttons ---------------//
        Button mUpButton = findViewById(R.id.upbutton);
        mUpButton.setOnClickListener(this);
        Button mConnectButton = findViewById(R.id.connectbutton);
        mConnectButton.setOnClickListener(this);

        TextView mStateView = findViewById(R.id.statetextview);

        mSessionController = SessionController.getSessionController();
        mSessionController.connect();

        RemoteClickListener mRemoteClickListener = new RemoteClickListener();
        mRemoteGridView.setOnItemClickListener(mRemoteClickListener);


        if (mSessionController.getSession().isConnected()) {
            mStateView.setText("Connected");
            showRemoteFiles();

        } else {
            mStateView.setText("Disconnected");
        }

    }

    private void setAdapter(ArrayList<File> files) {
        mLocaleFileListAdapter = new LocaleFileListAdapter(this, files);
    }

    public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
        // change the list
        if (mFilenames.get(position).isDirectory()) {
            mRootFile = mFilenames.get(position);
            Log.d(TAG, "ROOT FILE POSIITON IS " + mRootFile);
            mFilenames.clear();

            if (mRootFile.listFiles() == null) {
                return;
            }
            mFilenames.addAll(Arrays.asList(mRootFile.listFiles()));
//            setAdapter(mFilenames);
//            mLocalGridView.setAdapter(mLocaleFileListAdapter);
            mLocaleFileListAdapter.notifyDataSetChanged();

        } else {
            // sftp the file
            SftpProgressDialog progressDialog = new SftpProgressDialog(this, 0);
            progressDialog.setIndeterminate(false);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);

            File[] arr = {mFilenames.get(position)};
            mSessionController.uploadFiles(arr, progressDialog);
        }

    }

    public void onClick(View v) {
        if (v.getId() == R.id.upbutton) {
            boolean hasParent = mRootFile.getParentFile() != null;
            if (hasParent && mRootFile.getParentFile().canRead()) {
                mRootFile = mRootFile.getParentFile();
                mFilenames.clear();
                Collections.addAll(mFilenames, mRootFile.listFiles());
                setAdapter(mFilenames);
                mLocalGridView.setAdapter(mLocaleFileListAdapter);
            }
        }
    }

    private void showRemoteFiles() {
        final ProgressDialog progressDialog = new ProgressDialog(FileListActivity.this, 0);
        progressDialog.setIndeterminate(true);
        progressDialog.setTitle(R.string.retrieve_remote_files);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        try {
            mSessionController.listRemoteFiles(new TaskCallbackHandler() {
                @Override
                public void OnBegin() {
                    progressDialog.show();
                }

                @Override
                public void onFail() {
                    Log.e(TAG, "Fail listing remote files");
                    progressDialog.dismiss();
                }

                @Override
                public void onTaskFinished(Vector<ChannelSftp.LsEntry> lsEntries) {
                    mRemoteFileListAdapter = new RemoteFileListAdapter(FileListActivity.this, lsEntries);
                    mRemoteGridView.setAdapter(mRemoteFileListAdapter);
                    progressDialog.dismiss();
                }
            }, "");
        } catch (JSchException j) {
            Log.e(TAG, "ShowRemoteFiles exception " + j.getMessage());
            progressDialog.dismiss();
        } catch (SftpException s) {
            Log.e(TAG, "ShowRemoteFiles exception " + s.getMessage());
            progressDialog.dismiss();
        }
    }


    private class SftpProgressDialog extends ProgressDialog implements SftpProgressMonitor {

        private long mSize = 0;

        private long mCount = 0;

        SftpProgressDialog(Context context, int theme) {
            super(context, theme);
            // TODO Auto-generated constructor stub
        }

        public boolean count(long arg0) {
            mCount += arg0;
            this.setProgress((int) ((float) (mCount) / (float) (mSize) * (float) getMax()));
            return true;
        }

        public void end() {
            this.setProgress(this.getMax());
            this.dismiss();

        }

        public void init(int arg0, String arg1, String arg2, long arg3) {
            mSize = arg3;

        }


    }

    private class RemoteClickListener implements OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
            if (mIsProcessing) {
                return;
            }
            if (mRemoteFileListAdapter == null) {
                return;
            }
            //Is directory?   TODO make depend on data only
            if (mRemoteFileListAdapter.getRemoteFiles().get(position).getAttrs().isDir()
                    || "..".equals(mRemoteFileListAdapter.getRemoteFiles().get(position).getFilename().trim())) {

                final ProgressDialog progressDialog = new ProgressDialog(FileListActivity.this, 0);
                progressDialog.setIndeterminate(true);
                progressDialog.setTitle(R.string.retrieve_remote_files);
                progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);

                try {
                    mIsProcessing = true;
                    mSessionController.listRemoteFiles(new TaskCallbackHandler() {

                        @Override
                        public void OnBegin() {
                            progressDialog.show();
                        }

                        @Override
                        public void onFail() {
                            mIsProcessing = false;
                            progressDialog.dismiss();
                        }

                        @Override
                        public void onTaskFinished(Vector<ChannelSftp.LsEntry> lsEntries) {
                            mRemoteFileListAdapter = new RemoteFileListAdapter(FileListActivity.this, lsEntries);
                            mRemoteGridView.setAdapter(mRemoteFileListAdapter);
                            mRemoteFileListAdapter.notifyDataSetChanged();
                            mIsProcessing = false;
                            progressDialog.dismiss();

                        }
                    }, mRemoteFileListAdapter.getRemoteFiles().get(position).getFilename());
                } catch (JSchException j) {
                    Log.e(TAG, "Error on remote file click " + j.getMessage());
                    progressDialog.dismiss();
                } catch (SftpException s) {
                    Log.e(TAG, "Error on remote file click " + s.getMessage());
                    progressDialog.dismiss();
                }

            } else {

                SftpProgressDialog progressDialog = new SftpProgressDialog(FileListActivity.this, 0);
                progressDialog.setIndeterminate(false);
                progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                progressDialog.show();

                try {
                    String name = mRemoteFileListAdapter.getRemoteFiles().get(position).getFilename();
                    String out = mRootFile.getAbsolutePath() + "/" + name;

                    mSessionController.downloadFile(mRemoteFileListAdapter.getRemoteFiles().get(position).getFilename(), out, progressDialog);
                } catch (JSchException je) {
                    Log.d(TAG, "JschException " + je.getMessage());
                } catch (SftpException se) {
                    Log.d(TAG, "SftpException " + se.getMessage());
                }
            }


        }
    }

    private class DragShadow extends View.DragShadowBuilder {

        ColorDrawable mBox;

        public DragShadow(View view) {
            super(view);
            mBox = new ColorDrawable(Color.GRAY);
        }

        @Override
        public void onDrawShadow(Canvas canvas) {
            mBox.draw(canvas);
        }

        @Override
        public void onProvideShadowMetrics(Point shadowSize,
                                           Point shadowTouchPoint) {

            View v = getView();
            int height = v.getHeight();
            int width = v.getWidth();

            mBox.setBounds(0, 0, width, height);
            shadowSize.set(width, height);
            shadowTouchPoint.set(width / 2, height / 2);

        }
    }

    private class FileDragListener implements OnDragListener {

        @Override
        public boolean onDrag(View view, DragEvent dragEvent) {
            switch (dragEvent.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED:
                    break; //TODO

                case DragEvent.ACTION_DRAG_ENTERED:
                    break; //TODO

                case DragEvent.ACTION_DRAG_EXITED:
                    break; //TODO

                case DragEvent.ACTION_DROP:
                    if (view.getId() == R.id.listview) {
                        Log.d(TAG, "DROPPED");
                    }
                    break; //TODO

                case DragEvent.ACTION_DRAG_ENDED:
                    break; //TODO

                default:
                    break;
            }
            return false;
        }
    }
}
