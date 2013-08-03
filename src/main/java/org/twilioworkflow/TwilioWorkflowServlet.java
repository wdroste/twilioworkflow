package org.twilioworkflow;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Iterables;
import com.google.common.io.Closer;
import com.twilio.sdk.verbs.TwiMLResponse;
import org.python.core.PyDictionary;
import org.python.core.PyGenerator;
import org.python.core.PyObject;
import org.python.google.common.io.CharStreams;
import org.python.util.PythonInterpreter;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * This servlet is responsible for tracking Twilio sessions using Python generators for workflow execution.
 */
public class TwilioWorkflowServlet extends HttpServlet {

    ScriptEngineManager scriptEngineManager;
    Cache<String, Invocable> workflowScriptCache;
    Cache<String, PyGenerator> workflowSessionCache;

    @Override
    public void init(ServletConfig config) throws ServletException {

        super.init(config);

        Properties props = new Properties();
        PythonInterpreter.initialize(System.getProperties(), props, new String[]{""});

        // TODO: Use servlet configuration
        this.workflowScriptCache = CacheBuilder
                .newBuilder()
                .maximumSize(100)
                .build();

        // TODO: Use servlet configuration
        this.workflowSessionCache = CacheBuilder
                .newBuilder()
                .maximumSize(10000)
                .expireAfterWrite(1, TimeUnit.HOURS)
                .build();

        // build out the script compiler
        scriptEngineManager = new ScriptEngineManager();
    }

    /**
     * Determine based on Cookies if this request is new or is already in process.
     */
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            // attempt to find the existing session..
            TwiMLResponse twiMLResponse;
            String id = getWorkflowSessionId(req);
            PyObject twilioRequest = buildRequestTuple(req);
            PyGenerator pygen = workflowSessionCache.getIfPresent(id);
            if (null == pygen) {
                // load the workflow provided
                Invocable f = loadWorkflow(getWorkflowName(req));
                // create the python generator
                pygen = (PyGenerator) f.invokeFunction("execute", twilioRequest);
                twiMLResponse = toTwiMLResponse(pygen.next());
            } else {
                // execute the next request..
                twiMLResponse = toTwiMLResponse(pygen.send(twilioRequest));
            }

            // determine if this is the last response..
            if (twiMLResponse instanceof LastTwilioResponse) {
                workflowSessionCache.invalidate(id);
            } else {
                workflowSessionCache.put(id, pygen);
            }

            // send back the Twilio response..
            resp.addCookie(new Cookie("uuid", id));
            resp.getWriter().write(twiMLResponse.toXML());
        } catch (Exception ex) {
            throw new ServletException(ex);
        }
    }

    TwiMLResponse toTwiMLResponse(PyObject pyObject) {
        return (TwiMLResponse) pyObject.__tojava__(TwiMLResponse.class);
    }

    Invocable loadWorkflow(final String workflowName) throws ExecutionException, IOException {
        return this.workflowScriptCache.get(workflowName, new Callable<Invocable>() {
            @Override
            public Invocable call() throws Exception {
                ScriptEngine engine = newScriptEngine();
                String script = loadWorkflowScript(workflowName);
                engine.eval(script);
                return (Invocable) engine;
            }
        });
    }

    String loadWorkflowScript(String workflowName) throws IOException {
        Closer closer = Closer.create();
        try {
            ServletContext ctx = getServletContext();
            String scriptName = workflowName + ".py";
            InputStream ins = ctx.getResourceAsStream(scriptName);
            Reader rdr = closer.register(new InputStreamReader(ins, "UTF-8"));
            return CharStreams.toString(rdr);
        } catch (IOException ioe) {
            throw closer.rethrow(ioe);
        } finally {
            closer.close();
        }
    }

    String getWorkflowName(HttpServletRequest req) {
        String uri = req.getRequestURI();
        return uri.substring(1);
    }

    String getWorkflowSessionId(HttpServletRequest req) {
        Cookie uuidCookie = null;
        List<Cookie> cookies = getCookies(req);
        if (!cookies.isEmpty()) {
            uuidCookie = Iterables.find(cookies, new Predicate<Cookie>() {
                @Override
                public boolean apply(Cookie cookie) {
                    return "uuid".equals(cookie.getName());
                }
            });
        }
        return uuidCookie == null ? UUID.randomUUID().toString() : uuidCookie.getValue();
    }

    List<Cookie> getCookies(HttpServletRequest req) {
        Cookie[] cookies = req.getCookies();
        return cookies == null ? Collections.<Cookie>emptyList() : Arrays.asList(cookies);
    }

    ScriptEngine newScriptEngine() {
        ScriptEngine engine = scriptEngineManager.getEngineByName("python");
        Preconditions.checkNotNull("Unable to create python script engine.");
        return engine;
    }

//    AccountSid:AC6296f88308b5d30b17d9a2a88ac16606
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


    PyDictionary buildRequestTuple(HttpServletRequest req) {
        PyDictionary ret = new PyDictionary();
        ret.putAll(req.getParameterMap());
        return ret;
    }

}