package org.hobbit.core.service.api;

import com.google.common.util.concurrent.AbstractExecutionThreadService;

public class ExecutionThreadServiceDelegate2<T>
    extends AbstractExecutionThreadService
    implements ServiceDelegateEntity<T>
{
	protected T entity;
    protected Runnable startUp;
    protected Runnable run;
    protected Runnable shutDown;

    public ExecutionThreadServiceDelegate2(T entity, Runnable startUp, Runnable run, Runnable shutDown) {
        super();
        this.entity = entity;
        this.startUp = startUp;
        this.run = run;
        this.shutDown = shutDown;
    }
    
    @Override
    public T getEntity() {
    	return entity;
    }

    @Override
    protected void startUp() throws Exception {
        super.startUp();
        if(startUp != null) {
            startUp.run();
        }
    }

    @Override
    protected void run() throws Exception {
        if(run != null) {
            run.run();
        }
    }

    @Override
    protected void triggerShutdown() {
        if(shutDown != null) {
            shutDown.run();
        }

        super.triggerShutdown();
    }
}
