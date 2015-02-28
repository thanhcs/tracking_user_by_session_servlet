import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Thanh on 2/17/2015.
 * Name: Thanh Nguyen
 * EID: ttn2365
 */
public class MyEavesdropServlet extends HttpServlet {

    //Map used to store data about user
    //Key: user's cookie
    //Value: list of visited URLs
    HashMap<String, List<String>> data = new HashMap<String, List<String>>();

    int COOKIE_SESSION_LIFE = -1;  // value to pass to decide the cookie will live to the end of session
    int COOKIE_END_IMMEDIATE = 0;  // delete the cookie immediately
    @Override
    protected void doGet (HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        // get info from request
        String userName = request.getParameter("username");
        String sessionState = request.getParameter("session");
        if (sessionState == null)
            sessionState = "";
        Cookie[] cookies = request.getCookies();

        if (userName != null) {
            if (!userName.contains(" ") && userName.length() != 0) {                  // check if username contains space or empty
                Cookie validCookie = checkCookie(cookies);  // get the user's cookie from the request
                if (validCookie != null) {
                    switch (sessionState) {
                        case "end":
                            if (validCookie.getValue() != null && validCookie.getValue().equals(userName)) {
                                String cookieName = validCookie.getName();
                                //remove user's data from map
                                data.remove(cookieName);
                                // delete cookie on browser
                                Cookie cookieKiller = createCookie(cookieName, cookieName, request.getServletPath(), COOKIE_END_IMMEDIATE);
                                response.addCookie(cookieKiller);
                                response.getWriter().println(userName + "'s session is end");
                            }
                            // add else here to indicate that there is no session on user's browser
                            else {
                                response.getWriter().println("There is no " + userName + "'s session to be ended");
                            }
                            break;
                        case "start":
                            if (validCookie.getValue() != null && !validCookie.getValue().equals(userName))
                                response.getWriter().println("Error: Another session already active");
                            else
                                response.getWriter().println("Error: Session already active");
                            break;
                        default:
                            response.getWriter().println("Disallowed value specified for parameter session");
                    }

                } else switch (sessionState) {
                    case "end":
                        response.getWriter().println("There is no session to be ended");
                        break;
                    case "start":
                        // There may be an user have a same name in map but no cookie from browser
                        // -> user logs in from another browser


                        String cookieName = userName;
                        boolean flag = data.containsKey(cookieName);

                        // add N to username if there exists a cookie having same name as username
                        // to create a distinguish cookie's name
                        while (flag) {
                            cookieName = userName + "N";
                            if (!data.containsKey(cookieName))
                                flag = false;
                        }
                        // add cookie to response
                        Cookie cookie = createCookie(cookieName, userName, request.getServletPath(), COOKIE_SESSION_LIFE);
                        response.addCookie(cookie);
                        response.getWriter().println("Initiate a session for user " + userName);
                        // create a key in map
                        data.put(cookieName, new ArrayList<String>());
                        break;
                    default:
                        response.getWriter().println("Disallowed value specified for parameter session");
                }
            } else
                response.getWriter().println("Disallowed value specified for parameter username");
        } else {
            boolean historyControl = true;

            // false if there is no session
            //but the browser request a link.
            if (cookies == null || checkCookie(cookies) == null)
                historyControl = false;

            // URL data
            // get information about url user wants to get data from.
            String type = request.getParameter("type");
            String project = request.getParameter("project");
            String year = request.getParameter("year");

            // check if they are valid parameters
            String notValid = checkValidParams(type, project, year);


            if (notValid == null) {
                //tomcat will replace %23 by #, reverse this behavior
                project = project.replace("#", "%23");
                // build url
                String url = "http://eavesdrop.openstack.org/";
                if (type.equals("irclogs"))
                    url = url + type + "/" + project + "/";
                else
                    url = url + type + "/" + project + "/" + year + "/";

                // if there is a user session
                if (historyControl) {

                    // get session's info
                    String currentUserCookie = "";
                    for (Cookie ck : cookies) { // check if null?
                        if (data.containsKey(ck.getName()))
                            currentUserCookie = ck.getName();
                    }

                    //Visited URLs
                    List<String> urls = data.get(currentUserCookie);
                    response.getWriter().println("Visited URLs:");
                    for (String _url : urls) {
                        response.getWriter().println(_url);
                    }
                    response.getWriter().println();

                    //save url to Map
                    data.get(currentUserCookie).add(url);
                }

                // display data url
                response.getWriter().println("URL Data");

                // user JSoup to filter all file links
                Document doc = Jsoup.connect(url).get();
                Elements links = doc.getElementsByTag("a");
                for (Element link : links) {
                    String linkHref = link.attr("href");
                    // if a link is a link file, it will have "." in its name
                    // bad assumption?
                    if (linkHref.contains("."))
                        response.getWriter().println(url + linkHref);
                }

            } else
                response.getWriter().println("Disallowed value specified for parameter " + notValid);

        }

    }


    /*
    checkValidParams method used to check if parameters passed by user are valid or not
    Parameters:
        type: user's provided type parameter in {meetings, irclogs}
        project: user's provided project parameter.
                Valid if url: http://eavesdrop.openstack.org/<type>/<project> is accessible
        year: user's provided year parameter in {2010, 2011, 2012, 2013, 2014, 2015}
    return:
        "type", "project", "year" if type, project, or year is not valid
        null otherwise
     */
    private String checkValidParams(String type, String project, String year) {
        // check if no type parameter
        if (type == null)
            return "type";
        // check type
        if (!(type.equals("meetings") ||  type.equals("irclogs")))
            return "type";
        // don't have project parameter
        if (project == null || project.length() == 0)
            return "project";
        // check if project parameter exists
        project = project.replace("#", "%23");
        String url = "http://eavesdrop.openstack.org/" + type + "/" + project + "/";
        try {
            // source: http://stackoverflow.com/questions/4177864/checking-a-url-exist-or-not
//            HttpURLConnection.setFollowRedirects(false);
            // note : you may also need
//                    HttpURLConnection.setInstanceFollowRedirects(false)
            HttpURLConnection connection =
                    (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("HEAD");
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
                return "project";
        } catch (IOException e) {
            return "project";
        }

        // if type=meetings -> check year
        if (type.equals("meetings")) {
            if (year == null)
                return "year";
            //check year
            switch (year) {
                case "2010":
                case "2011":
                case "2012":
                case "2013":
                case "2014":
                case "2015":
                    break;
                default:
                    return "year";
            }
        }
        return null;
    }

    /*
    createCookie method used to create a cookie
    parameters:
        cookieName: cookie's name
        userName: cookie's value
        servletPath: cookie's path
        cookieLife: cookie's life span
    return:
        Cookie object
     */
    private Cookie createCookie(String cookieName, String userName, String servletPath, int cookieLife) {
        Cookie cookie = new Cookie(cookieName, userName);
        cookie.setDomain("localhost");
        cookie.setPath("/assignment2" + servletPath);
        cookie.setMaxAge(cookieLife);
        return cookie;
    }

    /*
    checkCookie method used to check if a cookie in request cookies live in data map
    parameters:
        cookies: array of request cookies
    return:
        Cookie object if there is a one
        null otherwise
     */
    private Cookie checkCookie(Cookie[] cookies) {
        // check if the user's browser has other user's active session
        if (cookies == null)
            return null;
        for (Cookie ck : cookies) {
            if (data.containsKey(ck.getName()))
                return ck;
        }
        return null;
    }
}
