package org.ecocean.servlet;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpSession;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.authc.UsernamePasswordToken;
import java.io.IOException;
import java.io.PrintWriter;
import javax.jdo.Query;
import java.util.List;
import java.util.ArrayList;
import org.ecocean.Shepherd;
import org.ecocean.ShepherdRO;
import org.ecocean.Util;
import org.ecocean.User;
import org.ecocean.configuration.*;
import org.json.JSONObject;
import org.json.JSONArray;
import java.util.Iterator;
import java.util.Collection;
import java.lang.reflect.Method;


public class RestServletV2 extends HttpServlet {
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }

    public void doOptions(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ServletUtilities.doOptions(request, response);
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        handleRequest(request, response, _parseUrl(request, Util.stringToJSONObject(request.getParameter("content"))));
    }
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        JSONObject payload = new JSONObject();
        try {
            payload = ServletUtilities.jsonFromHttpServletRequest(request);
        } catch (Exception ex) {
            _log("failed to parse json payload from request: " + ex.toString());
        }
        handleRequest(request, response, _parseUrl(request, payload));
    }

    //this will get /class/id from the url and massage it into json (which will take overwrite values from inJson if they exist)
    private JSONObject _parseUrl(final HttpServletRequest request, JSONObject inJson) {
        if (request.getPathInfo() == null) return inJson;
        if (inJson == null) inJson = new JSONObject();
        String[] parts = request.getPathInfo().split("/");  //dont forget has leading / like:  "/class/id"
        if (parts.length > 1) inJson.put("class", parts[1]);
        if (parts.length > 2) inJson.put("id", parts[2]);
        return inJson;
    }

    private void handleRequest(HttpServletRequest request, HttpServletResponse response, JSONObject payload) throws ServletException, IOException {
        response.setHeader("Access-Control-Allow-Origin", "*");  //allow us stuff from localhost
        String context = ServletUtilities.getContext(request);
        String instanceId = Util.generateUUID();
        String httpMethod = request.getMethod();

        if (payload == null) payload = new JSONObject();
        payload.put("_queryString", request.getQueryString());
        boolean debug = (payload.optBoolean("_debug", false) || ((request.getQueryString() != null) && request.getQueryString().matches(".*_debug.*")));

        if (debug) _log(instanceId, "payload: " + payload.toString());

        //first handle special cases (where arg is NOT a classname)
        if (payload.optString("class", "__FAIL__").equals("login")) {
            handleLogin(request, response, payload, instanceId, context);
            return;
        }
        if (payload.optString("class", "__FAIL__").equals("logout")) {
            handleLogout(request, response, payload, instanceId, context);
            return;
        }
        if (payload.optString("id", "__FAIL__").equals("list")) {
            handleList(request, response, payload, instanceId, context);
            return;
        }
        if (payload.optString("class", "__FAIL__").equals("configuration")) {
            handleConfiguration(request, response, payload, instanceId, context);
            return;
        }

        JSONObject rtn = new JSONObject();
        rtn.put("success", false);
/*
        Shepherd myShepherd = new Shepherd(context);
        myShepherd.setAction("RestServletV2.handleContent");
        myShepherd.commitDBTransaction();
        myShepherd.closeDBTransaction();
*/


        rtn.put("transactionId", instanceId);
        rtn.put("message", _rtnMessage("error"));
        if (debug) {
            _log(instanceId, "rtn: " + rtn.toString());
            JSONObject jbug = new JSONObject();
            jbug.put("payload", payload);
            jbug.put("timestamp", System.currentTimeMillis());
            jbug.put("remoteHost", ServletUtilities.getRemoteHost(request));
            jbug.put("method", httpMethod);
            jbug.put("queryString", request.getQueryString());
            jbug.put("pathInfo", request.getPathInfo());
            rtn.put("_debug", jbug);
        }
        response.setContentType("application/javascript");
        PrintWriter out = response.getWriter();
        out.println(rtn.toString());
        out.close();
    }
    private void handleLogin(HttpServletRequest request, HttpServletResponse response, JSONObject payload, String instanceId, String context) throws ServletException, IOException {
        if ((payload == null) || (context == null)) throw new IOException("invalid paramters");
        JSONObject rtn = new JSONObject();
        Shepherd myShepherd = new Shepherd(context);
        myShepherd.setAction("RestServletV2.handleLogin");
        response.setContentType("application/javascript");
        PrintWriter out = response.getWriter();

        rtn.put("success", false);
        rtn.put("transactionId", instanceId);
        User user = myShepherd.getUserByWhatever(payload.optString("login", null));
        if (user == null) {
            _log(instanceId, "invalid login with payload=" + payload);
            rtn.put("message", _rtnMessage("access_denied"));
            response.setStatus(401);
            myShepherd.rollbackDBTransaction();
            myShepherd.closeDBTransaction();
            out.println(rtn.toString());
            out.close();
            return;
        }
        //potentially could do something like 429 for too many tries, 4XX for acct disabled, etc.

        //do the actual login...
        try {
            UsernamePasswordToken token = new UsernamePasswordToken(user.getUsername(),
                ServletUtilities.hashAndSaltPassword(payload.optString("password", Util.generateUUID()), user.getSalt()) );
            Subject subject = SecurityUtils.getSubject();			
            subject.login(token);
        } catch (Exception ex) {
            _log(instanceId, "invalid login with payload=" + payload + "; threw " + ex.toString());
            rtn.put("message", _rtnMessage("access_denied"));
            response.setStatus(401);
            myShepherd.rollbackDBTransaction();
            myShepherd.closeDBTransaction();
            out.println(rtn.toString());
            out.close();
            return;
        }

/*   FIXME
		  	if((CommonConfiguration.getProperty("showUserAgreement",context)!=null)&&(CommonConfiguration.getProperty("userAgreementURL",context)!=null)&&(CommonConfiguration.getProperty("showUserAgreement",context).equals("true"))&&(!user.getAcceptedUserAgreement())){
*/
        rtn.put("needsUserAgreement", false);
        rtn.put("previousLogin", user.getLastLogin());
        rtn.put("success", true);
        rtn.put("message", _rtnMessage("success"));
        _log(instanceId, "successful login user=" + user);
        user.setLastLogin(System.currentTimeMillis());
        myShepherd.commitDBTransaction();
        myShepherd.closeDBTransaction();
        response.setContentType("application/javascript");
        out.println(rtn.toString());
        out.close();
    }
    private void handleLogout(HttpServletRequest request, HttpServletResponse response, JSONObject payload, String instanceId, String context) throws ServletException, IOException {
        JSONObject rtn = new JSONObject();
            //see:  http://jsecurity.org/api/index.html?org/jsecurity/web/DefaultWebSecurityManager.html
        Subject subject = SecurityUtils.getSubject();
        if (subject != null) subject.logout();
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        response.setContentType("application/javascript");
        PrintWriter out = response.getWriter();
        rtn.put("success", true);
        rtn.put("transactionId", instanceId);
        rtn.put("message", _rtnMessage("success"));
        out.println(rtn.toString());
        out.close();
    }

/*
    if payload.id exists, this is considered a GET of that value.  otherwise, payload *keys* will be considered ids, with values
    representing what to SET on those ids.
*/
    private void handleConfiguration(HttpServletRequest request, HttpServletResponse response, JSONObject payload, String instanceId, String context) throws ServletException, IOException {
        if ((payload == null) || (context == null)) throw new IOException("invalid paramters");
        payload.remove("class");
        payload.remove("_queryString");
        boolean isAdmin = request.isUserInRole("admin");
        JSONObject rtn = new JSONObject();
        rtn.put("success", false);
        rtn.put("transactionId", instanceId);
        Shepherd myShepherd = new Shepherd(context);
        myShepherd.setAction("RestServletV2.handleConfiguration");
        myShepherd.beginDBTransaction();
        response.setContentType("application/javascript");
        PrintWriter out = response.getWriter();

        String id = payload.optString("id", null);
        if (id != null) {  //get value
            Configuration conf = ConfigurationUtil.getConfiguration(myShepherd, id);
            JSONObject meta = conf.getMeta();
            if (!conf.isValid(meta)) {
                JSONObject jerr = new JSONObject();
                jerr.put("id", id);
                rtn.put("message", _rtnMessage("invalid_configuration_id", jerr));
            } else if (conf.isPrivate(meta) && !isAdmin) {
                JSONObject jerr = new JSONObject();
                jerr.put("id", id);
                rtn.put("message", _rtnMessage("access_denied_configuration", jerr));
                response.setStatus(401);
            } else {
                rtn.put("success", true);
                if (conf.isPrivate(meta)) rtn.put("private", true);
                if (conf.hasValue()) {
                    rtn.put("value", conf.getContent().get(ConfigurationUtil.VALUE_KEY));
                } else if (meta.has("defaultValue")) {
                    rtn.put("valueNotSet", true);
                    rtn.put("usingDefault", true);
                    rtn.put("value", meta.get("defaultValue"));
                } else {
                    rtn.put("valueNotSet", true);
                    rtn.put("message", _rtnMessage("configuration_no_value"));
                }
            }
            myShepherd.rollbackDBTransaction();
            myShepherd.closeDBTransaction();
            out.println(rtn.toString());
            out.close();
            return;
        }

        if (!isAdmin) {
            _log(instanceId, "invalid config set access with payload=" + payload);
            rtn.put("message", _rtnMessage("access_denied"));
            response.setStatus(401);
            myShepherd.rollbackDBTransaction();
            myShepherd.closeDBTransaction();
            out.println(rtn.toString());
            out.close();
            return;
        }

        List<String> updated = new ArrayList<String>();
        List<Configuration> updatedConfs = new ArrayList<Configuration>();
rtn.put("_payload", payload);

        try {
            for (Object k : payload.keySet()) {
                String key = (String)k;
                if (key.equals("foo")) throw new org.ecocean.DataDefinitionException("fake foo blah");
                Configuration conf = ConfigurationUtil.setConfigurationValue(myShepherd, key, payload.get(key));
                updatedConfs.add(conf);
                _log(instanceId, ">>>> SET key=" + key + " <= " + payload.get(key) + " => " + conf);
                rtn.put("success", true);
                updated.add(key);
            }
        } catch (Exception ex) {
            myShepherd.rollbackDBTransaction();
            myShepherd.closeDBTransaction();
            rtn.put("message", _rtnMessage("configuration_set_error", null, ex.toString()));
            _log(instanceId, "ERROR - rolling back db transaction due to exception on SET operation: " + ex.toString());
            out.println(rtn.toString());
            out.close();
            return;
        }

        myShepherd.commitDBTransaction();
        myShepherd.closeDBTransaction();
        //easiest way to update ROOT caches (let them reload when needed) now that we know we are persisted
        for (Configuration conf : updatedConfs) {
            conf.resetRootCache();
        }
        rtn.put("updated", new JSONArray(updated));
        rtn.put("message", _rtnMessage("success"));
        out.println(rtn.toString());
        out.close();
    }

    private void handleList(HttpServletRequest request, HttpServletResponse response, JSONObject payload, String instanceId, String context) throws ServletException, IOException {
        String className = payload.optString("class", null);
        if (className == null) throw new ServletException("empty class name");
        JSONArray rtn = new JSONArray();
        ShepherdRO myShepherd = new ShepherdRO(context);
        myShepherd.setAction("RestServletV2.handleList");
        String jdo = "SELECT FROM " + className;
///TODO set fetchDepth = 0 or whatever to make fast
        Query query = myShepherd.getPM().newQuery("JDOQL", jdo);
        Collection c = (Collection) (query.execute());
        Iterator it = c.iterator();
        while (it.hasNext()) {
            Object obj = it.next();
            String id = null;
            Long version = null;
            try {
                Method m = obj.getClass().getMethod("getId", new Class[0]);
                id = (String)m.invoke(obj);
                m = obj.getClass().getMethod("getVersion", new Class[0]);
                version = (Long)m.invoke(obj);
            } catch (Exception ex) {
                System.out.println("handleList threw " + ex.toString());
            }
            if (id == null) break;  //we dont try others cuz if this one failed, they all likely will!
            JSONObject j = new JSONObject();
            j.put("id", id);
            j.put("version", version);
            rtn.put(j);
        }
        query.closeAll();
        myShepherd.rollbackDBTransaction();
        myShepherd.closeDBTransaction();
        response.setContentType("application/javascript");
        PrintWriter out = response.getWriter();
        out.println(rtn.toString());
        out.close();
    }

    private JSONObject _rtnMessage(String key, JSONObject args, String details) {
        if (key == null) return null;
        JSONObject m = new JSONObject();
        m.put("key", key);
        if (args != null) m.put("args", args);
        if (details != null) m.put("details", details);
        return m;
    }
    private JSONObject _rtnMessage(String key, JSONObject args) {
        return _rtnMessage(key, args, null);
    }
    private JSONObject _rtnMessage(String key) {
        return _rtnMessage(key, null, null);
    }

    private void _log(String msg) {
        _log("-", msg);
    }
    private void _log(String id, String msg) {
        System.out.println("[RestServletV2:" + id + "] " + msg);
    }

}
  
  
