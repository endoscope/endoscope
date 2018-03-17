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
        err INT, 
        max INT, 
        min INT, 
        avg INT, 
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
     CREATE INDEX endo_gr_appGroup ON public.endoscopeGroup(appGroup);
     --instead of following consider complex indexes like in PostgreSQL schema
     CREATE INDEX endo_gr_startDate ON public.endoscopeGroup(startDate);
     CREATE INDEX endo_gr_endDate ON public.endoscopeGroup(endDate);
     CREATE INDEX endo_gr_appType ON public.endoscopeGroup(appType);
                     
     CREATE TABLE public.endoscopeStat(
         id VARCHAR(36) PRIMARY KEY, 
         groupId VARCHAR(36), 
         parentId VARCHAR(36), 
         rootId VARCHAR(36), 
         name VARCHAR(255), 
         hits NUMBER, 
         err NUMBER, 
         max NUMBER, 
         min NUMBER, 
         avg NUMBER, 
         hasChildren NUMBER 
     );
     
     CREATE INDEX endo_st_rootId ON public.endoscopeStat(rootId);
     --instead of following consider complex indexes like in PostgreSQL schema
     CREATE INDEX endo_st_parentId ON public.endoscopeStat(parentId);
     CREATE INDEX endo_st_groupId ON public.endoscopeStat(groupId);
     CREATE INDEX endo_st_name ON public.endoscopeStat(name);
     
     --see also aggregated tables schema for Postgresql
     
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
     CREATE INDEX endo_gr_appGroup ON public.endoscopeGroup(appGroup);
     CREATE INDEX endo_gr_e_s_t ON public.endoscopeGroup(endDate, startDate, appType);
                     
     CREATE TABLE public.endoscopeStat(
         id VARCHAR(36) PRIMARY KEY, 
         groupId VARCHAR(36), 
         parentId VARCHAR(36), 
         rootId VARCHAR(36), 
         name VARCHAR(255), 
         hits numeric, 
         err numeric, 
         max numeric, 
         min numeric, 
         avg numeric, 
         hasChildren numeric 
     );
     CREATE INDEX endo_st_g_p_n ON public.endoscopeStat(groupId, parentId, name);
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
     CREATE INDEX endo_gr_day_appGroup ON public.day_endoscopeGroup(appGroup);
     CREATE INDEX endo_gr_day_e_s_t ON public.day_endoscopeGroup(endDate, startDate, appType);
                     
     CREATE TABLE public.day_endoscopeStat(
         id VARCHAR(36) PRIMARY KEY, 
         groupId VARCHAR(36), 
         parentId VARCHAR(36), 
         rootId VARCHAR(36), 
         name VARCHAR(255), 
         hits numeric, 
         err numeric, 
         max numeric, 
         min numeric, 
         avg numeric, 
         hasChildren numeric 
     );
     CREATE INDEX endo_st_day_g_p_n ON public.day_endoscopeStat(groupId, parentId, name);
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
     CREATE INDEX endo_gr_week_appGroup ON public.week_endoscopeGroup(appGroup);
     CREATE INDEX endo_gr_week_e_s_t ON public.week_endoscopeGroup(endDate, startDate, appType);
                     
     CREATE TABLE public.week_endoscopeStat(
         id VARCHAR(36) PRIMARY KEY, 
         groupId VARCHAR(36), 
         parentId VARCHAR(36), 
         rootId VARCHAR(36), 
         name VARCHAR(255), 
         hits numeric, 
         err numeric, 
         max numeric, 
         min numeric, 
         avg numeric, 
         hasChildren numeric 
     );
     CREATE INDEX endo_st_week_g_p_n ON public.week_endoscopeStat(groupId, parentId, name);
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
     CREATE INDEX endo_gr_month_appGroup ON public.month_endoscopeGroup(appGroup);
     CREATE INDEX endo_gr_month_e_s_t ON public.month_endoscopeGroup(endDate, startDate, appType);
                     
     CREATE TABLE public.month_endoscopeStat(
         id VARCHAR(36) PRIMARY KEY, 
         groupId VARCHAR(36), 
         parentId VARCHAR(36), 
         rootId VARCHAR(36), 
         name VARCHAR(255), 
         hits numeric, 
         err numeric, 
         max numeric, 
         min numeric, 
         avg numeric, 
         hasChildren numeric 
     );
     CREATE INDEX endo_st_month_g_p_n ON public.month_endoscopeStat(groupId, parentId, name);
     CREATE INDEX endo_st_month_rootId ON public.month_endoscopeStat(rootId);