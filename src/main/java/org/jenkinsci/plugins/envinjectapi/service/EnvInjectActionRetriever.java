package org.jenkinsci.plugins.envinjectapi.service;

import hudson.model.Action;
import hudson.model.Run;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import org.jenkinsci.lib.envinject.EnvInjectAction;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectActionRetriever {

    //Returns the abstract class Action due to a class loading issue
    //with EnvInjectAction subclasses. Subclasses cannot be casted from
    //all point of Jenkins (classes are not loaded in some points)
    public Action getEnvInjectAction(@Nonnull Run<?, ?> build) {
        List<Action> actions;
        if (build == null) {
            throw new NullPointerException("A Run object must be set.");
        }
        try {
            Class<?> matrixClass = Class.forName("hudson.matrix.MatrixRun");
            if (matrixClass.isInstance(build)) {
                Method method = matrixClass.getMethod("getParentBuild", null);
                Object object = method.invoke(build);
                if (object instanceof Run<?, ?>) {
                    build = (Run<?, ?>) object;
                }
            }
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.FINEST, String.format("hudson.matrix.MatrixRun is not installed", e));
        } catch (NoSuchMethodException e) {
            LOGGER.log(Level.WARNING, String.format("The method getParentBuild does not exist for hudson.matrix.MatrixRun", e));
        } catch (IllegalAccessException e) {
            LOGGER.log(Level.WARNING, String.format("There was a problem in the invocation of getParentBuild in hudson.matrix.MatrixRun", e));
        } catch (InvocationTargetException e) {
            LOGGER.log(Level.WARNING, String.format("There was a problem in the invocation of getParentBuild in hudson.matrix.MatrixRun", e));
        }
        actions = build.getActions();
        for (Action action : actions) {
            if (action == null) {
                continue;
            }

            if (EnvInjectAction.URL_NAME.equals(action.getUrlName())) {
                return action;
            }
        }
        return null;
    }

    private static final Logger LOGGER = Logger.getLogger(EnvInjectActionRetriever.class.getName());
}
