package org.jenkinsci.plugins.envinjectapi.util;

import hudson.model.Action;
import hudson.model.Run;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import org.jenkinsci.lib.envinject.EnvInjectAction;

/**
 * @author Gregory Boissinot
 * @author Oleg Nenashev
 */
public class EnvInjectActionRetriever {

    private static final Logger LOGGER = Logger.getLogger(EnvInjectActionRetriever.class.getName());
        
    private EnvInjectActionRetriever() {
        // Cannot be instantinated
    }
    
    // TODO: Return statement likely mentions something obsolete (Binary conflict due to multiple envinject-lib deps?). Maybe makes sense to remove
    /**
     * Retrieve {@link EnvInjectAction}.
     * @param run Run
     * @return Abstract class Action due to a class loading issue
     *         Subclasses cannot be casted from all point of Jenkins (classes are not loaded in some point)
     */
    @CheckForNull
    public static Action getEnvInjectAction(@NonNull Run<?, ?> run) {

        try {
            Class<?> matrixClass = Class.forName("hudson.matrix.MatrixRun");
            if (matrixClass.isInstance(run)) {
                Method method = matrixClass.getMethod("getParentBuild");
                Object object = method.invoke(run);
                if (object instanceof Run<?, ?>) {
                    run = (Run<?, ?>) object;
                }
            }
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.FINEST, "hudson.matrix.MatrixRun is not installed", e);
        } catch (NoSuchMethodException e) {
            LOGGER.log(Level.WARNING, "The method getParentBuild does not exist for hudson.matrix.MatrixRun", e);
        } catch (IllegalAccessException | InvocationTargetException e) {
            LOGGER.log(Level.WARNING, "There was a problem in the invocation of getParentBuild in hudson.matrix.MatrixRun", e);
        }

        List<? extends Action> actions = run.getAllActions();
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


}
