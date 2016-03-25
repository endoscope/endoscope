package org.endoscope.cdi;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.endoscope.Endoscope;

@Priority(Interceptor.Priority.APPLICATION)
@Interceptor
@WithEndoscope
public class CdiInterceptor {
    @AroundInvoke
    public Object monitorOperation(InvocationContext ctx) throws Exception {

        if (!Endoscope.isEnabled()) {
            return ctx.proceed();
        }

        boolean first = false;
        try {
            first = Endoscope.push(getCallNameFromContext(ctx));
            return ctx.proceed();
        } catch (final Error e) {
            throw e;
        } finally {
            if( first ){
                Endoscope.popAll();
            }else {
                Endoscope.pop();
            }
        }
    }

    protected String getCallNameFromContext(InvocationContext ctx) {
        return ctx.getMethod().getDeclaringClass().getSimpleName() + "." + ctx.getMethod().getName();
    }
}
