package org.jenkinsci.plugins.envinjectapi.util;

import hudson.Plugin;
import hudson.model.Action;
import hudson.model.Run;
import edu.umd.cs.findbugs.annotations.NonNull;
import jenkins.model.Jenkins;
import org.jenkinsci.lib.envinject.EnvInjectAction;

/**
 * API Methods for checking installation status of the EnvInject plugin.
 * @author Gregory Boissinot
 * @author Oleg Nenashev
 */
public class EnvInjectPluginHelper {
       
    private EnvInjectPluginHelper() {
        // Cannot be instantinated
    }
    
    /**
     * Check if EnvInject is activated for the Run.
     * It means that the {@link EnvInjectAction} is defined, actually this action may be contributed by other plugins.
     * @param run Run
     * @return {@code true} if the run has {@link EnvInjectAction} 
     */
    public static boolean isEnvInjectActivated(@NonNull Run<?, ?> run) {
        Action envInjectAction = EnvInjectActionRetriever.getEnvInjectAction(run);
        return envInjectAction != null;
    }

    /**
     * Check if the EnvInject plugin is installed
     * @return {@code true} If the plugin is installed. It may be not activated.
     */
    public static boolean isEnvInjectPluginInstalled() {
        final Jenkins jenkins = Jenkins.getInstanceOrNull();
        if (jenkins == null) {
            return false;
        }
        Plugin envInjectPlugin = jenkins.getPlugin("envinject");
        return envInjectPlugin != null;
    }
}
