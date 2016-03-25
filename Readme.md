All right reserved.
You cannot use any part of this software unless you get my permission.
This software isn't finished and hasn't been tested.


Current Stats
-------------
Deploy WAR to Wildfy and open UI page:

	http://localhost:8080/example-war/rest/endoscope/current/ui

You may run some additional processing in order to change statistics by entering:
     
    http://localhost:8080/example-war/rest/controller/process


Stored Stats
------------
Deploy example WAR to Wildfy and open stats UI page:

    http://localhost:8080/example-war/rest/endoscope/storage/ui
        
Storage directory changes every time to restart application. 
In example application those values are hardcoded in:
    
    org.endoscope.CustomPropertyProvider

Development
--------------
You may serve static files from disk insterad from JAR resource by settings following property:
 
    org.endoscope.dev.res.dir=/path_to_sources/endoscope/cdi-simple-ui/src/main/resources/res
    
Configuration
-------------
For complete list of available properties please refer to:

    org.endoscope.properties.Properties
     