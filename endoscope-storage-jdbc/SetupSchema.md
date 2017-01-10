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


    --additional tabels for aggregated JDBC storage

    CREATE TABLE public.day_endoscopeGroup(
         id VARCHAR(36) PRIMARY KEY, 
         startDate TIMESTAMP, 
         endDate TIMESTAMP, 
         statsLeft numeric, 
         lost numeric, 
         fatalError VARCHAR(255),
         appGroup VARCHAR(100),
         appType VARCHAR(100)
     );
     CREATE INDEX endo_gr_day_startDate ON public.day_endoscopeGroup(startDate);
     CREATE INDEX endo_gr_day_endDate ON public.day_endoscopeGroup(endDate);
     CREATE INDEX endo_gr_day_appGroup ON public.day_endoscopeGroup(appGroup);
     CREATE INDEX endo_gr_day_appType ON public.day_endoscopeGroup(appType);
                     
     CREATE TABLE public.day_endoscopeStat(
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
     CREATE INDEX endo_st_day_parentId ON public.day_endoscopeStat(parentId);
     CREATE INDEX endo_st_day_groupId ON public.day_endoscopeStat(groupId);
     CREATE INDEX endo_st_day_name ON public.day_endoscopeStat(name);
     CREATE INDEX endo_st_day_rootId ON public.day_endoscopeStat(rootId);
     
     CREATE TABLE public.week_endoscopeGroup(
         id VARCHAR(36) PRIMARY KEY, 
         startDate TIMESTAMP, 
         endDate TIMESTAMP, 
         statsLeft numeric, 
         lost numeric, 
         fatalError VARCHAR(255),
         appGroup VARCHAR(100),
         appType VARCHAR(100)
     );
     CREATE INDEX endo_gr_week_startDate ON public.week_endoscopeGroup(startDate);
     CREATE INDEX endo_gr_week_endDate ON public.week_endoscopeGroup(endDate);
     CREATE INDEX endo_gr_week_appGroup ON public.week_endoscopeGroup(appGroup);
     CREATE INDEX endo_gr_week_appType ON public.week_endoscopeGroup(appType);
                     
     CREATE TABLE public.week_endoscopeStat(
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
     CREATE INDEX endo_st_week_parentId ON public.week_endoscopeStat(parentId);
     CREATE INDEX endo_st_week_groupId ON public.week_endoscopeStat(groupId);
     CREATE INDEX endo_st_week_name ON public.week_endoscopeStat(name);
     CREATE INDEX endo_st_week_rootId ON public.week_endoscopeStat(rootId);
     
     CREATE TABLE public.month_endoscopeGroup(
         id VARCHAR(36) PRIMARY KEY, 
         startDate TIMESTAMP, 
         endDate TIMESTAMP, 
         statsLeft numeric, 
         lost numeric, 
         fatalError VARCHAR(255),
         appGroup VARCHAR(100),
         appType VARCHAR(100)
     );
     CREATE INDEX endo_gr_month_startDate ON public.month_endoscopeGroup(startDate);
     CREATE INDEX endo_gr_month_endDate ON public.month_endoscopeGroup(endDate);
     CREATE INDEX endo_gr_month_appGroup ON public.month_endoscopeGroup(appGroup);
     CREATE INDEX endo_gr_month_appType ON public.month_endoscopeGroup(appType);
                     
     CREATE TABLE public.month_endoscopeStat(
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
     CREATE INDEX endo_st_month_parentId ON public.month_endoscopeStat(parentId);
     CREATE INDEX endo_st_month_groupId ON public.month_endoscopeStat(groupId);
     CREATE INDEX endo_st_month_name ON public.month_endoscopeStat(name);
     CREATE INDEX endo_st_month_rootId ON public.month_endoscopeStat(rootId);