# tracking_user_by_session_servlet #

### Sofware used ###

* IntelliJ
* TomCat 8.0

### Library used ###

* servlet-api 3.1.0
* Jsoup

### Objective ###

Implement a Servlet to query and present data from http://eavesdrop.openstack.org

### Description ###

The eavesdrop site is a site that provides public access to the IRC chat logs and meeting logs for various OPENStack projects.

Servlet will handle two groups of query parameters:

* username and session

Two query parameters used to start or end session depending on the value of session parameter client request

* username * keeps anyvalue except space

* session * keeps "start" to start a new session or "end" to end the current session

* type, project, meeting

This group used for clients access a particular URL address.

For example:

the input given: "type=meetings&project=barbican&year=2013"

the output: Display the file inside the page: http://eavesdrop.openstack.org/meetings/barbican/2013

this group of query parameters will also be recorded if there is an active user on that browser.

* type * keeps two values "irlogs" or "meetings"

* project * any project inside irlogs or meeting will be valid

* year * keeps value from 2010 to 2015

### Example Pictures ###

When there is no active user on a browser

![Alt text](noUser.PNG?raw=true "No user active")

When there is a active user. Visited URL will keep the URL address user visited before the current one.

![Alt text](userActive.PNG?raw=true "Active user")


