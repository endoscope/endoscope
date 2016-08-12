H2 schema
---------

    CREATE TABLE IF NOT EXISTS endoscopeGroup(
        id VARCHAR(36) PRIMARY KEY, 
        startDate TIMESTAMP, 
        endDate TIMESTAMP, 
        statsLeft INT, 
        lost INT, 
        fatalError VARCHAR(255),
        appGroup VARCHAR(100),
        appType VARCHAR(100)
    );
                    
    CREATE TABLE IF NOT EXISTS endoscopeStat(
        id VARCHAR(36) PRIMARY KEY, 
        groupId VARCHAR(36), 
        parentId VARCHAR(36), 
        rootId VARCHAR(36), 
        name VARCHAR(255), 
        hits INT, 
        max INT, 
        min INT, 
        avg INT, 
        ah10 INT, 
        hasChildren INT 
    );
    
Oracle schema
-------------
     
     CREATE TABLE public.endoscopeGroup(
         id VARCHAR(36) PRIMARY KEY, 
         startDate TIMESTAMP, 
         endDate TIMESTAMP, 
         statsLeft NUMBER, 
         lost NUMBER, 
         fatalError VARCHAR(255),
         appGroup VARCHAR(100),
         appType VARCHAR(100)
     );
     CREATE INDEX endo_gr_startDate ON public.endoscopeGroup(startDate);
     CREATE INDEX endo_gr_endDate ON public.endoscopeGroup(endDate);
     CREATE INDEX endo_gr_appGroup ON public.endoscopeGroup(appGroup);
     CREATE INDEX endo_gr_appType ON public.endoscopeGroup(appType);
                     
     CREATE TABLE public.endoscopeStat(
         id VARCHAR(36) PRIMARY KEY, 
         groupId VARCHAR(36), 
         parentId VARCHAR(36), 
         rootId VARCHAR(36), 
         name VARCHAR(255), 
         hits NUMBER, 
         max NUMBER, 
         min NUMBER, 
         avg NUMBER, 
         ah10 NUMBER, 
         hasChildren NUMBER 
     );
     CREATE INDEX endo_st_parentId ON public.endoscopeStat(parentId);
     CREATE INDEX endo_st_groupId ON public.endoscopeStat(groupId);
     CREATE INDEX endo_st_name ON public.endoscopeStat(name);
     CREATE INDEX endo_st_rootId ON public.endoscopeStat(rootId);

PostgreSQL schema
-----------------
     CREATE TABLE public.endoscopeGroup(
         id VARCHAR(36) PRIMARY KEY, 
         startDate TIMESTAMP, 
         endDate TIMESTAMP, 
         statsLeft numeric, 
         lost numeric, 
         fatalError VARCHAR(255),
         appGroup VARCHAR(100),
         appType VARCHAR(100)
     );
     CREATE INDEX endo_gr_startDate ON public.endoscopeGroup(startDate);
     CREATE INDEX endo_gr_endDate ON public.endoscopeGroup(endDate);
     CREATE INDEX endo_gr_appGroup ON public.endoscopeGroup(appGroup);
     CREATE INDEX endo_gr_appType ON public.endoscopeGroup(appType);
                     
     CREATE TABLE public.endoscopeStat(
         id VARCHAR(36) PRIMARY KEY, 
         groupId VARCHAR(36), 
         parentId VARCHAR(36), 
         rootId VARCHAR(36), 
         name VARCHAR(255), 
         hits numeric, 
         max numeric, 
         min numeric, 
         avg numeric, 
         ah10 numeric, 
         hasChildren numeric 
     );
     CREATE INDEX endo_st_parentId ON public.endoscopeStat(parentId);
     CREATE INDEX endo_st_groupId ON public.endoscopeStat(groupId);
     CREATE INDEX endo_st_name ON public.endoscopeStat(name);
     CREATE INDEX endo_st_rootId ON public.endoscopeStat(rootId);