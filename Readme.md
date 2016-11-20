This software isn't finished and hasn't been tested.
You use it on your own responsibility.  

Stats UI
--------
Deploy example WAR to Wildfy and open stats UI page:

    http://localhost:8080/endoscope-example-war/rest/endoscope/

You may run some additional processing in order to change statistics by entering:
             
    http://localhost:8080/endoscope-example-war/rest/controller/process

Storage directory changes every time to restart application. 
In example application those values are hardcoded in:
    
    com.github.endoscope.CustomPropertyProvider

UI Development
--------------
You may serve static files from disk insterad from JAR resource by settings following property:
 
    endoscope.dev.res.dir=/path_to_sources/endoscope/cdi-simple-ui/src/main/resources/res
    
Configuration
-------------
For complete list of available properties please refer to:

    com.github.endoscope.properties.Properties
     