package top.aiboom.library.ailogger;

import java.util.ArrayList;
import java.util.List;

/**
 * 对log进行配置
 */
public class LogConfig {

    public enum Level {
        V,D,I,W,E
    }

    private String path;
    private String suffix;
    private Level level;
    private int pid;
    private int size;
    private List<String> tags = new ArrayList<>();
    private String tagRegex;
    private String message;
    private String msgRegex;

    private static class Builder {

        private String path;
        private String suffix;
        private Level level;
        private int pid;
        private int size;
        private List<String> tags = new ArrayList<>();
        private String tagRegex;
        private String message;
        private String msgRegex;

        public Builder logFolder(String path){
            this.path = path;
            return this;
        }

        public Builder logFileSuffix(String suffix) {
            this.suffix = suffix;
            return this;
        }

        public Builder logLevel(Level level) {
            this.level = level;
            return this;
        }

        public Builder PID(int pid) {
            this.pid = pid;
            return this;
        }

        public Builder fileSize(int size) {
            this.size = size;
            return this;
        }

        public Builder tagFilter(List<String> tags) {
            this.tags.addAll(tags);
            return this;
        }

        public Builder tagFilter(String regex) {
            this.tagRegex = regex;
            return this;
        }

        public Builder msgFilter(String message) {
            this.message = message;
            return this;
        }

        public Builder msgRegexFilter(String msgRegex) {
            this.msgRegex = msgRegex;
            return this;
        }

        public LogConfig build() {
            LogConfig config = new LogConfig();
            config.path = path;
            config.level = level;
            config.message = message;
            config.msgRegex = msgRegex;
            config.pid = pid;
            config.size = size;
            config.suffix = suffix;
            config.tags = tags;
            config.tagRegex = tagRegex;
            return config;
        }
    }


}
