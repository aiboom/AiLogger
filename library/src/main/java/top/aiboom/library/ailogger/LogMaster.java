package top.aiboom.library.ailogger;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class LogMaster {

    public static final int LOG_LEVEL_NO_SET = 0;

    public static final int LOG_BUFFER_MAIN = 1;
    public static final int LOG_BUFFER_SYSTEM = 1 << 1;
    public static final int LOG_BUFFER_RADIO = 1 << 2;
    public static final int LOG_BUFFER_EVENTS = 1 << 3;
    public static final int LOG_BUFFER_KERNEL = 1 << 4; // not be supported by now

    public static final int LOG_BUFFER_DEFAULT = LOG_BUFFER_MAIN | LOG_BUFFER_SYSTEM;

    public static final int INVALID_PID = -1;

    public String mFileSuffix;
    public String mFolderPath;
    public int mFileSizeLimitation;
    public int mLevel;
    public List<String> mFilterTags = new ArrayList<>();
    public int mPID = INVALID_PID;

    public boolean mUseLogcatFileOut = false;

    private LogDumper mLogDumper = null;

    public static final int EVENT_RESTART_LOG = 1001;

    private RestartHandler mHandler;

    public LogMaster() {
        mHandler = new RestartHandler(this);
    }

    public void start() {
        // make sure the out folder exist
        // TODO support multi-phase path
        File file = new File(mFolderPath);
        if (!file.exists()) {
            file.mkdirs();
        }

        String cmdStr = collectLogcatCommand();

        if (mLogDumper != null) {
            mLogDumper.stopDumping();
            mLogDumper = null;
        }

        mLogDumper = new LogDumper(this, mFolderPath, mFileSuffix, mFileSizeLimitation, cmdStr, mHandler);
        mLogDumper.start();
    }

    public void stop() {
        // TODO maybe should clean the log buffer first?
        if (mLogDumper != null) {
            mLogDumper.stopDumping();
            mLogDumper = null;
        }
    }

    private String collectLogcatCommand() {
        StringBuilder stringBuilder = new StringBuilder();
        final String SPACE = " ";
        stringBuilder.append("logcat");

        // TODO select ring buffer, -b

        // TODO set out format
        stringBuilder.append(SPACE);
        stringBuilder.append("-v time");

        // append tag filters
        String levelStr = getLevelStr();

        if (!mFilterTags.isEmpty()) {
            stringBuilder.append(SPACE);
            stringBuilder.append("-s");
            for (int i = 0; i < mFilterTags.size(); i++) {
                String tag = mFilterTags.get(i) + ":" + levelStr;
                stringBuilder.append(SPACE);
                stringBuilder.append(tag);
            }
        } else {
            if (!TextUtils.isEmpty(levelStr)) {
                stringBuilder.append(SPACE);
                stringBuilder.append("*:" + levelStr);
            }
        }

        // logcat -f , but the rotated count default is 4?
        // can`t be sure to use that feature
        if (mPID != INVALID_PID) {
            mUseLogcatFileOut = false;
            String pidStr = adjustPIDStr();
            if (!TextUtils.isEmpty(pidStr)) {
                stringBuilder.append(SPACE);
                stringBuilder.append("|");
                stringBuilder.append(SPACE);
                stringBuilder.append("grep (" + pidStr + ")");
            }
        }

        return stringBuilder.toString();
    }

    private String getLevelStr() {
        switch (mLevel) {
            case 2:
                return "V";
            case 3:
                return "D";
            case 4:
                return "I";
            case 5:
                return "W";
            case 6:
                return "E";
            case 7:
                return "F";
        }

        return "V";
    }

    /**
     * Android`s user app pid is bigger than 1000.
     *
     * @return
     */
    private String adjustPIDStr() {
        if (mPID == INVALID_PID) {
            return null;
        }

        String pidStr = String.valueOf(mPID);
        int length = pidStr.length();
        if (length < 4) {
            pidStr = " 0" + pidStr;
        }

        if (length == 4) {
            pidStr = " " + pidStr;
        }

        return pidStr;
    }


    /**
     * Builder pattern.
     */
    public static class Builder {

        /**
         * context object
         */
        private Context mContext;

        /**
         * the folder name that we save log files to,
         * just folder name, not the whole path,
         * if set this, will save log files to /sdcard/$mLogFolderName folder,
         * use /sdcard/$ApplicationName as default.
         */
        private String mLogFolderName;

        /**
         * the whole folder path that we save log files to,
         * this setting`s priority is bigger than folder name.
         */
        private String mLogFolderPath;

        /**
         * the log file suffix,
         * if this is sot, it will be appended to log file name automatically
         */
        private String mLogFileNameSuffix = "";

        /**
         * single log file size limitation,
         * in k-bytes, ex. set to 16, is 16KB limitation.
         */
        private int mLogFileSizeLimitation = 0;

        /**
         * log level, see android.util.Log, 2 - 7,
         * if not be set, will use verbose as default
         */
        private int mLogLevel = LogMaster.LOG_LEVEL_NO_SET;

        /**
         * can set several filter tags
         * logcat -s ActivityManager:V SystemUI:V
         */
        private List<String> mLogFilterTags = new ArrayList<>();

        /**
         * filter through pid, by setting this with your APP PID,
         * the log recorder will just record the APP`s own log,
         * use one call: android.os.Process.myPid().
         */
        private int mPID = LogMaster.INVALID_PID;

        /**
         * which log buffer to catch...
         * <p/>
         * Request alternate ring buffer, 'main', 'system', 'radio'
         * or 'events'. Multiple -b parameters are allowed and the
         * results are interleaved.
         * <p/>
         * The default is -b main -b system.
         */
        private int mLogBuffersSelected = LogMaster.LOG_BUFFER_DEFAULT;

        /**
         * log output format, don`t support config yet, use $time format as default.
         * <p/>
         * Log messages contain a number of metadata fields, in addition to the tag and priority.
         * You can modify the output format for messages so that they display a specific metadata
         * field. To do so, you use the -v option and specify one of the supported output formats
         * listed below.
         * <p/>
         * brief      — Display priority/tag and PID of the process issuing the message.
         * process    — Display PID only.
         * tag        — Display the priority/tag only.
         * thread     - Display the priority, tag, and the PID(process ID) and TID(thread ID)
         * of the thread issuing the message.
         * raw        — Display the raw log message, with no other metadata fields.
         * time       — Display the date, invocation time, priority/tag, and PID of
         * the process issuing the message.
         * threadtime — Display the date, invocation time, priority, tag, and the PID(process ID)
         * and TID(thread ID) of the thread issuing the message.
         * long       — Display all metadata fields and separate messages with blank lines.
         */
        private int mLogOutFormat;

        /**
         * set log out folder name
         *
         * @param logFolderName folder name
         * @return The same Builder.
         */
        public Builder setLogFolderName(String logFolderName) {
            this.mLogFolderName = logFolderName;
            return this;
        }

        /**
         * set log out folder path
         *
         * @param logFolderPath out folder absolute path
         * @return the same Builder
         */
        public Builder setLogFolderPath(String logFolderPath) {
            this.mLogFolderPath = logFolderPath;
            return this;
        }

        /**
         * set log file name suffix
         *
         * @param logFileNameSuffix auto appened suffix
         * @return the same Builder
         */
        public Builder setLogFileNameSuffix(String logFileNameSuffix) {
            this.mLogFileNameSuffix = logFileNameSuffix;
            return this;
        }

        /**
         * set the file size limitation
         *
         * @param fileSizeLimitation file size limitation in KB
         * @return the same Builder
         */
        public Builder setLogFileSizeLimitation(int fileSizeLimitation) {
            this.mLogFileSizeLimitation = fileSizeLimitation;
            return this;
        }

        /**
         * set the log level
         *
         * @param logLevel log level, 2-7
         * @return the same Builder
         */
        public Builder setLogLevel(int logLevel) {
            this.mLogLevel = logLevel;
            return this;
        }

        /**
         * add log filterspec tag name, can add multiple ones,
         * they use the same log level set by setLogLevel()
         *
         * @param tag tag name
         * @return the same Builder
         */
        public Builder addLogFilterTag(String tag) {
            mLogFilterTags.add(tag);
            return this;
        }

        /**
         * which process`s log
         *
         * @param mPID process id
         * @return the same Builder
         */
        public Builder setPID(int mPID) {
            this.mPID = mPID;
            return this;
        }

        /**
         * -b radio, -b main, -b system, -b events
         * -b main -b system as default
         *
         * @param logBuffersSelected one of
         *                           LOG_BUFFER_MAIN = 1 << 0;
         *                           LOG_BUFFER_SYSTEM = 1 << 1;
         *                           LOG_BUFFER_RADIO = 1 << 2;
         *                           LOG_BUFFER_EVENTS = 1 << 3;
         *                           LOG_BUFFER_KERNEL = 1 << 4;
         * @return the same Builder
         */
        public Builder setLogBufferSelected(int logBuffersSelected) {
            this.mLogBuffersSelected = logBuffersSelected;
            return this;
        }

        /**
         * sets log out format, -v parameter
         *
         * @param logOutFormat out format, like -v time
         * @return the same Builder
         */
        public Builder setLogOutFormat(int logOutFormat) {
            this.mLogOutFormat = mLogOutFormat;
            return this;
        }

        public Builder(Context context) {
            mContext = context;
        }

        /**
         * call this only if mLogFolderName and mLogFolderPath not
         * be set both.
         *
         * @return
         */
        private void applyAppNameAsOutfolderName() {
            try {
                String appName = mContext.getPackageName();
                String versionName = mContext.getPackageManager().getPackageInfo(
                        appName, 0).versionName;
                int versionCode = mContext.getPackageManager()
                        .getPackageInfo(appName, 0).versionCode;
                mLogFolderName = appName + "-" + versionName + "-" + versionCode;
                mLogFolderPath = applyOutfolderPath();
            } catch (Exception e) {
            }
        }

        private String applyOutfolderPath() {
            String outPath = "";
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                outPath = Environment.getExternalStorageDirectory()
                        .getAbsolutePath() + File.separator + mLogFolderName;
            }

            return outPath;
        }

        /**
         * Combine all of the options that have been set and return
         * a new {@link LogMaster} object.
         */
        public LogMaster build() {
            LogMaster logRecorder = new LogMaster();

            // no folder name & folder path be set
            if (TextUtils.isEmpty(mLogFolderName)
                    && TextUtils.isEmpty(mLogFolderPath)) {
                applyAppNameAsOutfolderName();
            }

            // make sure out path be set
            if (TextUtils.isEmpty(mLogFolderPath)) {
                mLogFolderPath = applyOutfolderPath();
            }

            logRecorder.mFolderPath = mLogFolderPath;
            logRecorder.mFileSuffix = mLogFileNameSuffix;
            logRecorder.mFileSizeLimitation = mLogFileSizeLimitation;
            logRecorder.mLevel = mLogLevel;
            if (!mLogFilterTags.isEmpty()) {
                for (int i = 0; i < mLogFilterTags.size(); i++) {
                    logRecorder.mFilterTags.add(mLogFilterTags.get(i));
                }
            }
            logRecorder.mPID = mPID;

            return logRecorder;
        }
    }

}
