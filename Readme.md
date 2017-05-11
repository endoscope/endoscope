About
=====
Endoscope is simple monitoring utility for Java applications. 
It was created to run all the time with your application and collect execution time statistics from many points 
of the application. With Endoscope you will be able to see counts and average times for call trees in you application. 

[TODO: put screenshot of UI with expanded call tree here]

If you decide to periodically persist statistics you can review them when needed and either see what wen wrong 
yesterday or just see progress or regression in performance in some period of time. 

[TODO: put screenshot of daily chart here]

There are many tool's that can do similar tasks. Some can connected to your JVM as an agent. 
Other can use cloud based services to collect application metrics of your choice. 
You may also use instrumentation to transparently add such monitoring to you application.
 
In case of Endoscope all you need to do is to add few dependencies to your project and configure Endoscope with 
system properties or even at runtime from your own code.

Sometimes it's the easiest way to get insight of what happens inside your web application. 

How it works
============
Storing stats
-------------
You need to feed Endoscope with data by telling when method/call/query starts and when it ends.

Use Endoscope.push(nameOfYourOperation) to tell that call has started and Endoscope.pop() to tell when it ends.

If you use following order:
- Endoscope.push("parent")
- Endoscope.push("child")
- Endoscope.pop()
- Endoscope.pop()

You will put "child" call in context of a "parent" call. 

Runtime overhead
----------------
Endoscope.push creates small object that stores method name and start timestamp.
Endoscope.pop calculates execution time and puts it in last object. 
With last call to Endoscope.pop Endoscope stores structure describing execution tree in a queue for further processing
in separate thread.

That is all overhead you add to execution of your methods.

Heavy operations and RAM usage
------------------------------
Separate thread will handle all heavier operations like aggregation of results and persistence if such was configured.

Stats queue, call tree structures and aggregated results have hard limits on number of items in order to make sure 
you will not run out of RAM. By default Endoscope will take up to 50MB of your heap and will simply start to drop 
data if for any reason you exceed limits.

Design
======
Default
-------
You may call Endoscope push and pop explicitly but it's not the most handy way.
Most applications use frameworks and libraries that support interceptors.
In such case typical Endoscope modules work in following flows:

    traffic -> Interceptors -> Core -> Storage -> DB
    
    user -> UI -> Core -> Storage -> DB

You can use in memory statistics only:

    traffic -> Interceptors -> Core
    
    user -> UI -> Core
    
Modules
-------
Except Core module all other are optional and you may call explicitly and collect statistics in memory only if you wish.

Interceptor modules are here to let you easily plugin Endoscope to your framework and libraries.

Storage modules let's you persist and read aggregated statistics in storage of your choice.

UI modules present data in a user friendly way so you can see it.

Distributed Apps
----------------
If you run multiple instances of your application you can aggregate data from all of them in one place if you use shared
storage. Make sure each app has following modules:

    Interceptors -> Core -> Storage
    
You can deploy UI module in separate application in order to see stats from any application:

    UI -> Core -> Storage

Setup
=====
Dependencies
------------
Usually you need to add:
- interceptor module dependency
- storage module dependency

Then set system properties for you application:
- endoscope.enabled=true
- endoscope.storage.class=com.github.endoscope.storage.gzip.GzipStorage
- endoscope.storage.class.init.param=/tmp/endoscope/storage
            
If you need UI then add:
- UI module dependency

Configuration
-------------
For complete list of available properties please refer to:

    com.github.endoscope.properties.Properties
     
Enabling in CDI 1.0
-------------------
In JBoss 6 (CDI 1.0) you may need to add beans interceptor manually in WEB-INF/beans.xml

    <beans xmlns="http://java.sun.com/xml/ns/javaee"
           xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
           xsi:schemaLocation="
          http://java.sun.com/xml/ns/javaee
          http://java.sun.com/xml/ns/javaee/beans_1_0.xsd" >
        <interceptors>
            <class>com.github.endoscope.cdi.CdiInterceptor</class>
        </interceptors>
    </beans>

If you need password protected UI add web filter for UI plugin win WEB-INF/web.xml

    <filter>
        <filter-name>endoscopeSecurityFilter</filter-name>
        <filter-class>com.github.endoscope.cdiui.SecurityFilter</filter-class>
    </filter>
    <filter-mapping>
        <filter-name>endoscopeSecurityFilter</filter-name>
        <url-pattern>/endoscope/*</url-pattern>
    </filter-mapping>
  
and configure credentials in properties

Development
===========
Where do I start?
-----------------
Take a look at following places for start:
- com.github.endoscope.cdi.CdiInterceptor
- com.github.endoscope.core.Engine
- com.github.endoscope.properties.Properties
- com.github.endoscope.core.CurrentStatsAsyncTasks.triggerAsyncTask()
- com.github.endoscope.core.Stats.store(com.github.endoscope.core.Context)
- com.github.endoscope.storage.gzip.GzipStorage

Be careful - we are working with threads andy try to use the least synchronization possible.
By design synchronization may happen only in Endoscope thread and NOT in runtime operations around monitored code.

Example application
-------------------
Deploy example WAR to JBoss (we suggest Wildfy) and open stats UI page:

    http://localhost:8080/endoscope-example-war/rest/endoscope/

You may run some additional processing in order to change statistics by entering:
             
    http://localhost:8080/endoscope-example-war/rest/controller/process

Notice that storage directory changes every time you restart application. 
In example application configuration properties are hardcoded in:
    
    com.github.endoscope.CustomPropertyProvider

UI Development
--------------
You may serve static files from disk instead from JAR resources by settings following property:
 
    endoscope.dev.res.dir=/path_to_sources/endoscope/cdi-simple-ui/src/main/resources/res
    
UI code is not the masterpiece... yet. I've got some plans to clean it up... in the future.

License
=======
Copyright 2017 Endoscope Team

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.