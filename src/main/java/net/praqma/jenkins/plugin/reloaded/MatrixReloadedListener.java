package net.praqma.jenkins.plugin.reloaded;

import java.util.List;

import net.praqma.jenkins.plugin.reloaded.MatrixReloadedState.BuildState;

import hudson.Extension;
import hudson.matrix.Combination;
import hudson.matrix.MatrixRun;
import hudson.matrix.MatrixBuild;
import hudson.model.AbstractBuild;
import hudson.model.ParameterValue;
import hudson.model.TaskListener;
import hudson.model.ParametersAction;
import hudson.model.Run;
import hudson.model.StringParameterValue;
import hudson.model.listeners.RunListener;

/**
 * This registers the {@link Action}s to the side panel of the matrix project
 * and sets the Run.RedoRun object if it's actually a redo.
 * @author wolfgang
 *
 */
@Extension
public class MatrixReloadedListener extends RunListener<Run>
{
	public MatrixReloadedListener()
	{
		super( Run.class );
	}
	
	@Override
	public void onStarted( Run run, TaskListener listener )
	{
        /* First try to find the correct parameter for the project */
		/* Test for MatrixRun */
		if( run instanceof MatrixRun )
		{
			List<ParametersAction> actionList = run.getActions( ParametersAction.class );
			/* This is not a Matrix Reloaded instance */
			if( actionList.size() == 0 )
			{
				return;
			}
			
			List<ParameterValue> pvs = run.getActions( ParametersAction.class ).get(0).getParameters();
			StringParameterValue uuid = (StringParameterValue)getParameterValue( pvs, "uuid" );
            
            MatrixRun mr = (MatrixRun)run;
            
            /* This run is not related to a matrix reloaded build */
            if( uuid == null )
            {
            	return;
            }
            
            /* Retrieve the build state and set the RedoRun object */
            BuildState bs = MatrixReloadedState.getInstance().getBuildState( uuid.value );

            int mnumber = bs.rebuildNumber;
            Combination combination = mr.getParent().getCombination();
            run.setRedoRun( mnumber, bs.getConfiguration( combination ) );
            bs.removeConfiguration( combination );
		}
	}
	
	/**
	 * Convenience method for retrieving {@link ParameterValue}s.
	 * @param pvs A list of {@link ParameterValue}s.
	 * @param key The key of the {@link ParameterValue}.
	 * @return The parameter or null
	 */
	private ParameterValue getParameterValue( List<ParameterValue> pvs, String key )
	{
    	for( ParameterValue pv : pvs )
    	{
    		if( pv.getName().equals( key ) )
    		{
    			return pv;
    		}
    	}
    	
    	return null;
	}
	
	/**
	 * Add the Matrix Reloaded link to the build context
	 */
	@Override
	public void onCompleted( Run run, TaskListener listener )
	{
		/* Test for MatrixBuild and add to context */
		if( run instanceof MatrixBuild )
		{
			AbstractBuild<?, ?> build = (AbstractBuild<?, ?>)run;

			MatrixReloadedAction action = new MatrixReloadedAction();
			build.getActions().add( action );
		}
		
		/* Test for MatrixRun and add to context */
		if( run instanceof MatrixRun )
		{
			AbstractBuild<?, ?> build = (AbstractBuild<?, ?>)run;

			/* Only add the action if there isn't one already => if its not a reuse. */
			if( run.getRedoRun() == null || run.getRedoRun().rebuild )
			{
				MatrixReloadedAction action = new MatrixReloadedAction( ( (MatrixRun)run ).getParent().getCombination().toString() );
				build.getActions().add( action );
			}
		}
	}

}