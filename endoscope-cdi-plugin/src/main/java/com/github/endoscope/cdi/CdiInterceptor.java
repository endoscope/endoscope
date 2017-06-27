package com.github.endoscope.cdi;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import com.github.endoscope.Endoscope;

@Priority(Interceptor.Priority.APPLICATION)
@Interceptor
@WithEndoscope
public class CdiInterceptor {
    @AroundInvoke
    public Object monitorOperation(InvocationContext ctx) throws Exception {
        return Endoscope.monitorEx(getCallNameFromContext(ctx), () -> ctx.proceed() );
    }

    protected String getCallNameFromContext(InvocationContext ctx) {
        return ctx.getMethod().getDeclaringClass().getSimpleName() + "." + ctx.getMethod().getName();
    }
}
