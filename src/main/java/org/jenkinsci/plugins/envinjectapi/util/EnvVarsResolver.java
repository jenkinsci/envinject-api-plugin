package org.jenkinsci.plugins.envinjectapi.util;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
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
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jenkins.model.Jenkins;
import jenkins.security.MasterToSlaveCallable;
import org.jenkinsci.lib.envinject.EnvInjectException;

/**
 * Provides utility methods for resolving environment variables.
 * @author Gregory Boissinot
 * @author Oleg Nenashev
 */
public class EnvVarsResolver {

    private EnvVarsResolver() {
        // Cannot be instantinated
    }
    
    @Nonnull
    public static Map<String, String> getPollingEnvVars(@Nonnull Job<?, ?> job, @CheckForNull Node node) throws EnvInjectException {

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

    @Nonnull
    public static Map<String, String> getEnVars(@Nonnull Run<?, ?> run) throws EnvInjectException {
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
        Node builtOn = (run instanceof AbstractBuild) ? ((AbstractBuild)run).getBuiltOn() : null;
        
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

    @Nonnull
    private static Map<String, String> getFallBackMasterNode(@Nonnull Job<?, ?> job) throws EnvInjectException {
        final Node masterNode = getMasterNode();
        if (masterNode == null) {
            return gatherEnvVarsMaster(job);
        }
        return getDefaultEnvVarsJob(job, masterNode);
    }

    @CheckForNull
    private static Node getMasterNode() {
        final Jenkins jenkins  = Jenkins.getInstance();
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
    public static String resolveEnvVars(@Nonnull Run<?, ?> run, @CheckForNull String value) throws EnvInjectException {
        if (value == null) {
            return null;
        }

        return Util.replaceMacro(value, getEnVars(run));
    }

    @Nonnull
    private static Map<String, String> getDefaultEnvVarsJob(@Nonnull Job<?, ?> job, @Nonnull Node node) throws EnvInjectException {
        // TODO: wat
        assert node.getRootPath() != null;
        //--- Same code for master or a slave node
        Map<String, String> result = gatherEnvVarsMaster(job);
        result.putAll(gatherEnvVarsNode(job, node));
        result.putAll(gatherEnvVarsNodeProperties(node));
        return result;
    }

    @Nonnull
    private static Map<String, String> gatherEnvVarsMaster(@Nonnull Job<?, ?> job) throws EnvInjectException {
        Jenkins jenkins = JenkinsHelper.getInstance();
        
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
    @Nonnull
    private static Map<String, String> gatherEnvVarsNodeProperties(@CheckForNull Node node) throws EnvInjectException {

        EnvVars env = new EnvVars();

        Jenkins jenkins = Jenkins.getInstance();
        if (jenkins != null) {
            DescribableList<NodeProperty<?>, NodePropertyDescriptor> globalNodeProperties = jenkins.getGlobalNodeProperties();
            if (globalNodeProperties != null) {
                for (NodeProperty nodeProperty : globalNodeProperties) {
                    if (nodeProperty != null && nodeProperty instanceof EnvironmentVariablesNodeProperty) {
                        env.putAll(((EnvironmentVariablesNodeProperty) nodeProperty).getEnvVars());
                    }
                }
            }
        }

        if (node != null) {
            DescribableList<NodeProperty<?>, NodePropertyDescriptor> nodeProperties = node.getNodeProperties();
            for (NodeProperty nodeProperty : nodeProperties) {
                if (nodeProperty != null && nodeProperty instanceof EnvironmentVariablesNodeProperty) {
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

    @Nonnull
    private static Map<String, String> gatherEnvVarsNode(@Nonnull Job<?, ?> job, @Nonnull Node node) throws EnvInjectException {
        
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
                FilePath wFilePath = ((AbstractProject)job).getSomeWorkspace();
                if (wFilePath != null) {
                    envVars.put("WORKSPACE", wFilePath.getRemote());
                }
            }

            return envVars;

        } catch (IOException ioe) {
            throw new EnvInjectException(ioe);
        } catch (InterruptedException ie) {
            throw new EnvInjectException(ie);
        }
    }

}

