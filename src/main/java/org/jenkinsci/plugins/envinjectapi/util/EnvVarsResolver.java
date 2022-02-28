package org.jenkinsci.plugins.envinjectapi.util;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Cause;
import hudson.model.CauseAction;
import hudson.model.Computer;
import hudson.model.Job;
import hudson.model.Node;
import hudson.model.Run;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.slaves.NodeProperty;
import hudson.slaves.NodePropertyDescriptor;
import hudson.util.DescribableList;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import jenkins.model.Jenkins;
import jenkins.security.MasterToSlaveCallable;
import org.jenkinsci.lib.envinject.EnvInjectException;
import static org.jenkinsci.plugins.envinjectapi.util.CauseHelper.ENV_CAUSE;
import static org.jenkinsci.plugins.envinjectapi.util.CauseHelper.ENV_ROOT_CAUSE;

/**
 * Provides utility methods for resolving environment variables.
 * @author Gregory Boissinot
 * @author Oleg Nenashev
 */
public class EnvVarsResolver {

    private EnvVarsResolver() {
        // Cannot be instantinated
    }
    
    @NonNull
    public static Map<String, String> getPollingEnvVars(@NonNull Job<?, ?> job, @CheckForNull Node node) throws EnvInjectException {

        final Run<?, ?> lastBuild = job.getLastBuild();
        if (lastBuild != null) {
            if (EnvInjectPluginHelper.isEnvInjectPluginInstalled()) {
                return getEnVars(lastBuild);
            }
        }

        if (node == null) {
            return getFallBackMasterNode(job);
        }
        if (node.getRootPath() == null) {
            return getFallBackMasterNode(job);
        }

        return getDefaultEnvVarsJob(job, node);
    }

    @SuppressWarnings("unchecked")
    @NonNull
    public static Map<String, String> getEnVars(@NonNull Run<?, ?> run) throws EnvInjectException {
        Action envInjectAction = EnvInjectActionRetriever.getEnvInjectAction(run);
        if (envInjectAction != null) {
            try {
                Method method = envInjectAction.getClass().getMethod("getEnvMap");
                return (Map<String, String>) method.invoke(envInjectAction);
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException | ClassCastException e) {
                throw new EnvInjectException(e);
            }
        }

        // Retrieve node used for this build
        Node builtOn = (run instanceof AbstractBuild) ? ((AbstractBuild<?,?>)run).getBuiltOn() : null;
        
        // Check if node is always on. Otherwise, gather master env vars
        if (builtOn == null) {
            return getFallBackMasterNode(run.getParent());
        }
        if (builtOn.getRootPath() == null) {
            return getFallBackMasterNode(run.getParent());
        }

        // Get envVars from the node of the last build
        return getDefaultEnvVarsJob(run.getParent(), builtOn);
    }

    @NonNull
    private static Map<String, String> getFallBackMasterNode(@NonNull Job<?, ?> job) throws EnvInjectException {
        final Node masterNode = getMasterNode();
        if (masterNode == null) {
            return gatherEnvVarsMaster(job);
        }
        return getDefaultEnvVarsJob(job, masterNode);
    }

    @CheckForNull
    private static Node getMasterNode() {
        final Jenkins jenkins  = Jenkins.getInstanceOrNull();
        if (jenkins == null) {
            return null;
        }
        Computer computer = jenkins.toComputer();
        if (computer == null) {
            return null; //Master can have no executors
        }
        return computer.getNode();
    }

    @CheckForNull
    public static String resolveEnvVars(@NonNull Run<?, ?> run, @CheckForNull String value) throws EnvInjectException {
        if (value == null) {
            return null;
        }

        return Util.replaceMacro(value, getEnVars(run));
    }

    @NonNull
    private static Map<String, String> getDefaultEnvVarsJob(@NonNull Job<?, ?> job, @NonNull Node node) throws EnvInjectException {
        // TODO: wat
        assert node.getRootPath() != null;
        //--- Same code for master or a slave node
        Map<String, String> result = gatherEnvVarsMaster(job);
        result.putAll(gatherEnvVarsNode(job, node));
        result.putAll(gatherEnvVarsNodeProperties(node));
        return result;
    }

    @NonNull
    private static Map<String, String> gatherEnvVarsMaster(@NonNull Job<?, ?> job) throws EnvInjectException {
        final Jenkins jenkins;
        try {
            jenkins = Jenkins.get();
        } catch(IllegalStateException ex) {
            throw new EnvInjectException(ex);
        }
        
        EnvVars env = new EnvVars();
        env.put("JENKINS_SERVER_COOKIE", Util.getDigestOf("ServerID:" + jenkins.getSecretKey()));
        env.put("HUDSON_SERVER_COOKIE", Util.getDigestOf("ServerID:" + jenkins.getSecretKey())); // Legacy compatibility
        env.put("JOB_NAME", job.getFullName());
        
        env.put("JENKINS_HOME", jenkins.getRootDir().getPath());
        env.put("HUDSON_HOME", jenkins.getRootDir().getPath());   // legacy compatibility

        String rootUrl = jenkins.getRootUrl();
        if (rootUrl != null) {
            env.put("JENKINS_URL", rootUrl);
            env.put("HUDSON_URL", rootUrl); // Legacy compatibility
            env.put("JOB_URL", rootUrl + job.getUrl());
        }

        return env;
    }

    //Strong limitation: Restrict here to EnvironmentVariablesNodeProperty subclasses
    //in order to avoid the propagation of a Launcher object and a BuildListener object
    @NonNull
    private static Map<String, String> gatherEnvVarsNodeProperties(@CheckForNull Node node) throws EnvInjectException {

        EnvVars env = new EnvVars();

        Jenkins jenkins = Jenkins.getInstanceOrNull();
        if (jenkins != null) {
            DescribableList<NodeProperty<?>, NodePropertyDescriptor> globalNodeProperties = jenkins.getGlobalNodeProperties();
            if (globalNodeProperties != null) {
                for (NodeProperty<?> nodeProperty : globalNodeProperties) {
                    if (nodeProperty instanceof EnvironmentVariablesNodeProperty) {
                        env.putAll(((EnvironmentVariablesNodeProperty) nodeProperty).getEnvVars());
                    }
                }
            }
        }

        if (node != null) {
            DescribableList<NodeProperty<?>, NodePropertyDescriptor> nodeProperties = node.getNodeProperties();
            for (NodeProperty<?> nodeProperty : nodeProperties) {
                if (nodeProperty instanceof EnvironmentVariablesNodeProperty) {
                    EnvVars envVars = ((EnvironmentVariablesNodeProperty) nodeProperty).getEnvVars();
                    if (envVars != null) {
                        for (Map.Entry<String, String> entry : envVars.entrySet()) {
                            String key = entry.getKey();
                            String value = entry.getValue();
                            if (key != null && value != null) {
                                env.put(key, value);
                            }
                        }
                    }
                }
            }
        }

        return env;
    }

    @NonNull
    private static Map<String, String> gatherEnvVarsNode(@NonNull Job<?, ?> job, @NonNull Node node) throws EnvInjectException {
        
        final FilePath rootPath = node.getRootPath();
        if (rootPath == null) {
            //TODO: better than the original NPE. But maybe it's preferable to have more intelligent handling
            throw new EnvInjectException("Cannot retrieve Environment variables from the offline node");
        }
        
        try {
            Map<String, String> envVars = new EnvVars(rootPath.act(new MasterToSlaveCallable<Map<String, String>, EnvInjectException>() {
                private static final long serialVersionUID = 1L;
                   
                @Override
                public Map<String, String> call() throws EnvInjectException {
                    return EnvVars.masterEnvVars;
                }
            }));

            envVars.put("NODE_NAME", node.getNodeName());
            envVars.put("NODE_LABELS", Util.join(node.getAssignedLabels(), " "));
            
            if (job instanceof AbstractProject) {
                FilePath wFilePath = ((AbstractProject<?,?>)job).getSomeWorkspace();
                if (wFilePath != null) {
                    envVars.put("WORKSPACE", wFilePath.getRemote());
                }
            }

            return envVars;

        } catch (IOException | InterruptedException ioe) {
            throw new EnvInjectException(ioe);
        }
    }
    
    // Moved from EnvInject
    
    /**
     * Retrieves variables describing the Run cause. 
     * @param run Run
     * @return Set of environment variables, which depends on the cause type. 
     */
    @NonNull
    public static Map<String, String> getCauseEnvVars(@NonNull Run<?, ?> run) {
        CauseAction causeAction = run.getAction(CauseAction.class);
        Map<String, String> env = new HashMap<>();
        List<String> directCauseNames = new ArrayList<>();
        Set<String> rootCauseNames = new LinkedHashSet<>();

        if (causeAction != null) {
            List<Cause> buildCauses = causeAction.getCauses();
            for (Cause cause : buildCauses) {
                directCauseNames.add(CauseHelper.getTriggerName(cause));
                CauseHelper.insertRootCauseNames(rootCauseNames, cause, 0);
            }
        } else {
            directCauseNames.add("UNKNOWN");
            rootCauseNames.add("UNKNOWN");
        }
        env.putAll(CauseHelper.buildCauseEnvironmentVariables(ENV_CAUSE, directCauseNames));
        env.putAll(CauseHelper.buildCauseEnvironmentVariables(ENV_ROOT_CAUSE, rootCauseNames));
        return env;
    }
}

