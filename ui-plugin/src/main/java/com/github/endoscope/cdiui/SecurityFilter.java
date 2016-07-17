package com.github.endoscope.cdiui;

import com.github.endoscope.properties.Properties;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Serializable;
import java.util.Base64;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 By default filters /endoscope/* so if you need different path configure it in web.xml:

 <filter>
   <filter-name>endoscopeSecurityFilter</filter-name>
   <filter-class>com.github.endoscope.cdiui.SecurityFilter</filter-class>
 </filter>
 <filter-mapping>
   <filter-name>endoscopeSecurityFilter</filter-name>
   <url-pattern>/rest/endoscope/*</url-pattern>
 </filter-mapping>

 */
@WebFilter("/endoscope/*")
public class SecurityFilter implements Filter, Serializable {
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        String userPass = Properties.getAuthCredentials();
        if( isBlank(userPass) ){
            //allow all
            chain.doFilter(request, response);
            return;
        }

        //credentials are defined - verify request
        HttpServletRequest httpReq = (HttpServletRequest)request;
        String base64 = httpReq.getHeader("Authorization");
        if( isNotBlank(base64) && base64.startsWith("Basic ") ){
            base64 = base64.replace("Basic ", "");
            String reqUserPass = new String(Base64.getDecoder().decode(base64));
            if( userPass.equals(reqUserPass) ){
                chain.doFilter(request, response);
                //credentials confirmed
                return;
            }
        }

        //no credentials or incorrect
        HttpServletResponse httpResponse = (HttpServletResponse)response;
        httpResponse.setHeader("WWW-Authenticate", "Basic");
        httpResponse.setStatus(401);
    }

    @Override
    public void destroy() {
    }
}
