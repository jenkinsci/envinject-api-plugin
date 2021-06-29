package org.jenkinsci.plugins.envinjectapi.util;

import hudson.model.Cause;
import hudson.triggers.SCMTrigger;
import hudson.triggers.TimerTrigger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.Locale;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import static org.apache.commons.lang.StringUtils.isNotBlank;

// Moved from EnvInject plugin
/**
 * Contains helper methods for working with run {@link Cause}s.
 * @author Gregory Boissinot
 * @author Oleg Nenashev
 */
/*package*/ class CauseHelper {

    /**
     * Maximum depth of transitive upstream causes we want to record.
     */
    private static final int MAX_UPSTREAM_DEPTH = 10;
    public static final String ENV_CAUSE = "BUILD_CAUSE";
    public static final String ENV_ROOT_CAUSE = "ROOT_BUILD_CAUSE";
    
    /**
     * Inserts root cause names to the specified target container.
     * @param causeNamesTarget Target set. May receive null items
     * @param cause Cause to be added. For {@code Cause.UstreamCause} there will be in-depth search
     * @param depth Current search depth. {@link #MAX_UPSTREAM_DEPTH} is a limit
     */
    static void insertRootCauseNames(@Nonnull Set<String> causeNamesTarget, @CheckForNull Cause cause, int depth) {
        if (cause instanceof Cause.UpstreamCause) {
            if (depth == MAX_UPSTREAM_DEPTH) {
                causeNamesTarget.add("DEEPLYNESTEDCAUSES");
            } else {
                Cause.UpstreamCause c = (Cause.UpstreamCause) cause;
                List<Cause> upstreamCauses = c.getUpstreamCauses();
                for (Cause upstreamCause : upstreamCauses)
                    insertRootCauseNames(causeNamesTarget, upstreamCause, depth + 1);
            }
        } else {
            //TODO: Accordig to the current design this list may receive null for unknown trigger. Bug?
            // Should actually return UNKNOWN
            causeNamesTarget.add(getTriggerName(cause));
        }
    }

    @Nonnull
    static Map<String, String> buildCauseEnvironmentVariables(@Nonnull String envBase, @Nonnull Collection<String> causeNames) {
        Map<String, String> triggerVars = new HashMap<>();
        List<String> nonEmptyNames = new ArrayList<>();
        for (String name : causeNames) {
            if (isNotBlank(name)) {
                triggerVars.put(String.join("_", envBase, name), "true");
                nonEmptyNames.add(name);
            }
        }
        // add variable containing all the trigger names
        triggerVars.put(envBase, String.join(",", nonEmptyNames));
        return triggerVars;
    }

    @CheckForNull
    @SuppressWarnings(value = "deprecation")
    static String getTriggerName(Cause cause) {
        if (SCMTrigger.SCMTriggerCause.class.isInstance(cause)) {
            return "SCMTRIGGER";
        } else if (TimerTrigger.TimerTriggerCause.class.isInstance(cause)) {
            return "TIMERTRIGGER";
        } else if (Cause.UserIdCause.class.isInstance(cause)) {
            return "MANUALTRIGGER";
        } else if (Cause.UserCause.class.isInstance(cause)) {
            return "MANUALTRIGGER";
        } else if (Cause.UpstreamCause.class.isInstance(cause)) {
            return "UPSTREAMTRIGGER";
        } else if (cause != null) {
            return cause.getClass().getSimpleName().toUpperCase(Locale.ENGLISH);
        }

        return null;
    }

}
