package org.twiliowf;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Iterables;
import org.python.core.PyGenerator;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * This servlet is responsible for tracking Twilio sessions using Python generators for workflow execution.
 */
public class TwilioWorkflowServlet extends HttpServlet {

    Compilable compiler;
    Cache<String, PyGenerator> workflowSessionCache;
    LoadingCache<String, CompiledScript> workflowScriptCache;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        // TODO: Use servlet configuration
        this.workflowScriptCache = CacheBuilder.newBuilder().maximumSize(100).build(new CacheLoader<String, CompiledScript>() {
            @Override
            public CompiledScript load(String key) throws Exception {
                return loadPythonWorkflow(key);
            }
        });
        // TODO: Use servlet configuration
        this.workflowSessionCache = CacheBuilder.newBuilder().maximumSize(10000).expireAfterWrite(1, TimeUnit.HOURS).build();


        ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
        final String PATH = "pydictbuilder.py";
        try {
            ScriptEngine engine;


            // create the static script engine..
        engine = getScriptEngine(scriptEngineManager);
        for (String imprt : getImports()) {
            engine.eval(imprt);
        }
        this.compiler = (Compilable) engine;

    }

    /**
     * Determine based on Cookies if this request is new or is already in process.
     */
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String workflowSession = getWorkflowSession(req);
        if (workflowSession == null) {
            // create a new session based on the request information..

        } else {
            try {
                // attempt to find the existing session..
                final String workflowName = req.getRequestURI();
                workflowSession = UUID.randomUUID().toString();
                workflowSessionCache.get(workflowSession, new Callable<PyGenerator>() {
                    @Override
                    public PyGenerator call() throws Exception {
                        // build the compiler code..
                        CompiledScript c = workflowScriptCache.get(workflowName);

                        return null;
                    }
                });
            } catch (Exception ex) {

            }

        }
    }

    CompiledScript loadPythonWorkflow(String workflowName) {

        return null;
    }


    String getWorkflowSession(HttpServletRequest req) {
        List<Cookie> cookies = getCookies(req);
        Cookie uuidCookie = Iterables.find(cookies, new Predicate<Cookie>() {
            @Override
            public boolean apply(Cookie cookie) {
                return "uuid".equals(cookie.getName());
            }
        });
        return uuidCookie == null ? null : uuidCookie.getValue();
    }

    List<Cookie> getCookies(HttpServletRequest req) {
        Cookie[] cookies = req.getCookies();
        return cookies == null ? Collections.<Cookie>emptyList() : Arrays.asList(cookies);
    }

    ScriptEngine getScriptEngine(ScriptEngineManager scriptEngineManager) {
        ScriptEngine engine = scriptEngineManager.getEngineByName("python");
        Preconditions.checkNotNull("Unable to retrieve python script engine.");
        return engine;
    }


}
