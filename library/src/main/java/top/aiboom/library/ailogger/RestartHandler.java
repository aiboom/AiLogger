package top.aiboom.library.ailogger;

import android.os.Handler;
import android.os.Message;

class RestartHandler extends Handler {
    final LogMaster logRecorder;
    public RestartHandler(LogMaster logRecorder) {
        this.logRecorder = logRecorder;
    }

    @Override
    public void handleMessage(Message msg) {
        if (msg.what == LogMaster.EVENT_RESTART_LOG) {
            logRecorder.stop();
            logRecorder.start();
        }
    }
}
