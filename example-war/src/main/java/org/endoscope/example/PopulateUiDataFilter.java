package org.endoscope.example;

import javax.inject.Inject;
import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * It's demo app so run some processing before entering UI - otherwise it's empty and doesn't look good.
 */
@WebFilter(filterName = "PopulateUiDataFilter",urlPatterns = {
        "/rest/endoscope/current/ui/*",
        "/rest/endoscope/storage/ui/*"
})
public class PopulateUiDataFilter implements Filter {
    @Inject
    TheRestController controller;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        String url = ((HttpServletRequest)servletRequest).getRequestURL().toString();
        if( url.endsWith("/ui") ){//main page only
            controller.process();
        }
        filterChain.doFilter(servletRequest, servletResponse);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void destroy() {

    }
}
