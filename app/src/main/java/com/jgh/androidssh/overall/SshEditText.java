package com.jgh.androidssh.overall;

import android.content.Context;
import android.text.InputType;
import android.text.Layout;
import android.text.Selection;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;

public class SshEditText extends android.support.v7.widget.AppCompatEditText {

    private String mlastInput;

    private String mPrompt = "";

    public SshEditText(Context context) {
        super(context);
        setup();
    }

    public SshEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup();
    }

    public SshEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setup();
    }

    public void setup() {
        this.setRawInputType(InputType.TYPE_CLASS_TEXT);
//        this.setImeOptions(EditorInfo.IME_ACTION_GO);
//        this.setTextSize(12f);
    }

    @Override
    protected void onSelectionChanged(int s, int e) {
        //force selection to end
        setSelection(this.length());
    }

    public String getLastInput() {
        synchronized (this) {
            String rez = mlastInput;
            mlastInput = null;
            return rez;
        }
    }

    public String peekLastInput() {
        synchronized (this) {
            return mlastInput;
        }
    }

    public void AddLastInput(String s) {
        synchronized (this) {
            if (mlastInput == null) {
                mlastInput = "";
            }
            mlastInput = s;
        }
    }

    public int getCurrentCursorLine() {
        int selectionStart = Selection.getSelectionStart(this.getText());
        Layout layout = this.getLayout();

        if (!(selectionStart == -1)) {
            return layout.getLineForOffset(selectionStart);
        }

        return -1;
    }

    public boolean isNewLine() {

        int i = this.getText().toString().toCharArray().length;
        if (i == 0)
            return true;

        char s = this.getText().toString().toCharArray()[i - 1];
        if (s == '\n' || s == '\r') return true;

        return false;
    }

    public synchronized void setPrompt(String prompt) {
        mPrompt = prompt;
    }

    public synchronized String getPrompt() {
        return mPrompt;
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        return new SshConnectionWrapper(super.onCreateInputConnection(outAttrs),
                true);
    }

    private class SshConnectionWrapper extends InputConnectionWrapper {


        SshConnectionWrapper(InputConnection target, boolean mutable) {
            super(target, mutable);
        }

        @Override
        public boolean sendKeyEvent(KeyEvent event) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_DEL) {
                if (isNewLine()) {
                    return false;

                } else if (getCurrentCursorLine() < getLineCount() - 1) {
                    return false;
                }
            }
            return super.sendKeyEvent(event);
        }

        @Override
        public boolean deleteSurroundingText(int beforeLength, int afterLength) {

            if (isNewLine()) {
                return false;
            } else if (getCurrentCursorLine() < getLineCount() - 1) {
                return false;
            } else {
                return super.deleteSurroundingText(beforeLength, afterLength);
            }
        }
    }
}
