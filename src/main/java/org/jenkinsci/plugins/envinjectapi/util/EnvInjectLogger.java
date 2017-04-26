package org.jenkinsci.plugins.envinjectapi.util;

import hudson.model.TaskListener;

import java.io.Serializable;

/**
 * Utility class for logging EnvInject messages to the run.
 * @author Gregory Boissinot
 */
public class EnvInjectLogger implements Serializable {

    private static final long serialVersionUID = 1L;

    private final TaskListener listener;

    public EnvInjectLogger(TaskListener listener) {
        this.listener = listener;
    }

    public TaskListener getListener() {
        return listener;
    }

    public void info(String message) {
        listener.getLogger().println("[EnvInject] - " + message);
    }

    public void error(String message) {
        listener.getLogger().println("[EnvInject] - [ERROR] - " + message);
    }
}

