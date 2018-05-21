package top.aiboom.library.ailogger;

import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;

class LogDumper extends Thread {
    private LogMaster logUtils;
    final String logPath;
    final String logFileSuffix;
    final int logFileLimitation;
    final String logCmd;

    final RestartHandler restartHandler;

    private Process logcatProc;
    private BufferedReader mReader = null;
    private FileOutputStream out = null;

    private boolean mRunning = true;
    final private Object mRunningLock = new Object();

    private long currentFileSize;

    public LogDumper(LogMaster logUtils, String folderPath, String suffix,
                     int fileSizeLimitation, String command,
                     RestartHandler handler) {
        this.logUtils = logUtils;
        logPath = folderPath;
        logFileSuffix = suffix;
        logFileLimitation = fileSizeLimitation;
        logCmd = command;
        restartHandler = handler;

        String date = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss")
                .format(new Date(System.currentTimeMillis()));
        String fileName = (TextUtils.isEmpty(logFileSuffix)) ? date : (logFileSuffix + "-"+ date);
        try {
            out = new FileOutputStream(new File(logPath, fileName + ".log"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void stopDumping() {
        synchronized (mRunningLock) {
            mRunning = false;
        }
    }

    @Override
    public void run() {
        try {
            logcatProc = Runtime.getRuntime().exec(logCmd);
            mReader = new BufferedReader(new InputStreamReader(
                    logcatProc.getInputStream()), 1024);
            String line = null;
            while (mRunning && (line = mReader.readLine()) != null) {
                if (!mRunning) {
                    break;
                }
                if (line.length() == 0) {
                    continue;
                }
                if (out != null && !line.isEmpty()) {
                    byte[] data = (line + "\n").getBytes();
                    out.write(data);
                    if (logFileLimitation != 0) {
                        currentFileSize += data.length;
                        if (currentFileSize > logFileLimitation*1024) {
                            restartHandler.sendEmptyMessage(LogMaster.EVENT_RESTART_LOG);
                            break;
                        }
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (logcatProc != null) {
                logcatProc.destroy();
                logcatProc = null;
            }
            if (mReader != null) {
                try {
                    mReader.close();
                    mReader = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                out = null;
            }
        }
    }
}
