package org.twilioworkflow;

import com.google.common.collect.Maps;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

/**
 * Test all the workflow options.
 */
public class TwilioWorkflowServletUnitTest {

    IMocksControl control;
    TwilioWorkflowServlet servlet;

    ServletConfig servletConfig;
    ServletContext servletContext;
    HttpServletRequest httpServletRequest;
    HttpServletResponse httpServletResponse;

    Map<String, String[]> parameterMap;

    @Before
    public void setup() throws Exception {
        this.servlet = new TwilioWorkflowServlet();
        this.control = EasyMock.createStrictControl();
        this.servletConfig = this.control.createMock(ServletConfig.class);
        this.servletContext = this.control.createMock(ServletContext.class);
        this.httpServletRequest = this.control.createMock(HttpServletRequest.class);
        this.httpServletResponse = this.control.createMock(HttpServletResponse.class);

        this.parameterMap = Maps.newHashMap();
        this.parameterMap.put("AccountSid", new String[]{"AC6296f88308b5d30b17d9a2a88ac16606"});
        this.parameterMap.put("Body", new String[]{"some stuff"});
        this.parameterMap.put("ToZip", new String[]{"78702"});

    }

    //    AccountSid:
    //    Body:more stuff
    //    ToZip:78702
    //    FromState:TX
    //    ToCity:AUSTIN
    //    SmsSid:SM3c964e54860fe90d08b44638d00561ef
    //    ToState:TX
    //    To:+15125498629
    //    ToCountry:US
    //    FromCountry:US
    //    SmsMessageSid:SM3c964e54860fe90d08b44638d00561ef
    //    ApiVersion:2010-04-01
    //    FromCity:AUSTIN
    //    SmsStatus:received
    //    From:+15125246948
    //    FromZip:78703

    @Test
    public void testTwilioRequest() throws Exception {
        // build the request parameters..
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        InputStream ins = TwilioWorkflowServletUnitTest.class.getResourceAsStream("/myworkflow.py");

        EasyMock.expect(this.httpServletRequest.getCookies()).andReturn(null);
        EasyMock.expect(this.httpServletRequest.getParameterMap()).andReturn(this.parameterMap);
        EasyMock.expect(this.httpServletRequest.getRequestURI()).andReturn("/myworkflow");
        EasyMock.expect(this.servletConfig.getServletContext()).andReturn(this.servletContext);
        EasyMock.expect(this.servletContext.getResourceAsStream(EasyMock.eq("myworkflow.py"))).andReturn(ins);
        this.httpServletResponse.addCookie(EasyMock.isA(Cookie.class));
        EasyMock.expect(this.httpServletResponse.getWriter()).andReturn(printWriter);

        // execute a typical request
        this.control.replay();
        this.servlet.init(this.servletConfig);
        this.servlet.service(this.httpServletRequest, this.httpServletResponse);
        this.control.verify();

        printWriter.flush();
        System.out.println("Response: " + stringWriter.getBuffer());
    }
}
