H2 schema
---------

    CREATE TABLE IF NOT EXISTS clob_endoscopeGroup(
        id VARCHAR(36) PRIMARY KEY, 
        startDate TIMESTAMP, 
        endDate TIMESTAMP, 
        statsLeft INT, 
        lost INT, 
        fatalError VARCHAR(255),
        appGroup VARCHAR(100),
        appType VARCHAR(100)
    );
                    
    CREATE TABLE IF NOT EXISTS clob_endoscopeStat(
        id VARCHAR(36) PRIMARY KEY, 
        groupId VARCHAR(36), 
        rootId VARCHAR(36), 
        name VARCHAR(255), 
        hits INT, 
        err INT, 
        max INT, 
        min INT, 
        avg INT, 
        ah10 INT, 
        hasChildren INT,
        children TEXT
    );
     
PostgreSQL schema
-----------------
     CREATE TABLE public.clob_endoscopeGroup(
         id VARCHAR(36) PRIMARY KEY, 
         startDate TIMESTAMP, 
         endDate TIMESTAMP, 
         statsLeft numeric, 
         lost numeric, 
         fatalError VARCHAR(255),
         appGroup VARCHAR(100),
         appType VARCHAR(100)
     );    
     CREATE INDEX clob_endo_gr_appGroup ON public.clob_endoscopeGroup(appGroup);
     CREATE INDEX clob_endo_gr_e_s_t ON public.clob_endoscopeGroup(endDate, startDate, appType);
                     
     CREATE TABLE public.clob_endoscopeStat(
         id VARCHAR(36) PRIMARY KEY, 
         groupId VARCHAR(36), 
         rootId VARCHAR(36), 
         name VARCHAR(255), 
         hits numeric, 
         err numeric, 
         max numeric, 
         min numeric, 
         avg numeric, 
         ah10 numeric, 
         hasChildren numeric,
         children TEXT
     );
     CREATE INDEX clob_endo_st_g_p_n ON public.clob_endoscopeStat(groupId, name);
     CREATE INDEX clob_endo_st_rootId ON public.clob_endoscopeStat(rootId);


    --additional tabels for aggregated JDBC storage

    CREATE TABLE public.clob_day_endoscopeGroup(
         id VARCHAR(36) PRIMARY KEY, 
         startDate TIMESTAMP, 
         endDate TIMESTAMP, 
         statsLeft numeric, 
         lost numeric, 
         fatalError VARCHAR(255),
         appGroup VARCHAR(100),
         appType VARCHAR(100)
     );
     CREATE INDEX clob_endo_gr_day_appGroup ON public.clob_day_endoscopeGroup(appGroup);
     CREATE INDEX clob_endo_gr_day_e_s_t ON public.clob_day_endoscopeGroup(endDate, startDate, appType);
                     
     CREATE TABLE public.clob_day_endoscopeStat(
         id VARCHAR(36) PRIMARY KEY, 
         groupId VARCHAR(36), 
         rootId VARCHAR(36), 
         name VARCHAR(255), 
         hits numeric, 
         err numeric, 
         max numeric, 
         min numeric, 
         avg numeric, 
         ah10 numeric, 
         hasChildren numeric,
         children TEXT
     );
     CREATE INDEX clob_endo_st_day_g_p_n ON public.clob_day_endoscopeStat(groupId, name);
     CREATE INDEX clob_endo_st_day_rootId ON public.clob_day_endoscopeStat(rootId);
     
     CREATE TABLE public.clob_week_endoscopeGroup(
         id VARCHAR(36) PRIMARY KEY, 
         startDate TIMESTAMP, 
         endDate TIMESTAMP, 
         statsLeft numeric, 
         lost numeric, 
         fatalError VARCHAR(255),
         appGroup VARCHAR(100),
         appType VARCHAR(100)
     );
     CREATE INDEX clob_endo_gr_week_appGroup ON public.clob_week_endoscopeGroup(appGroup);
     CREATE INDEX clob_endo_gr_week_e_s_t ON public.clob_week_endoscopeGroup(endDate, startDate, appType);
                     
     CREATE TABLE public.clob_week_endoscopeStat(
         id VARCHAR(36) PRIMARY KEY, 
         groupId VARCHAR(36), 
         rootId VARCHAR(36), 
         name VARCHAR(255), 
         hits numeric, 
         err numeric, 
         max numeric, 
         min numeric, 
         avg numeric, 
         ah10 numeric, 
         hasChildren numeric,
         children TEXT
     );
     CREATE INDEX clob_endo_st_week_g_p_n ON public.clob_week_endoscopeStat(groupId, name);
     CREATE INDEX clob_endo_st_week_rootId ON public.clob_week_endoscopeStat(rootId);
     
     CREATE TABLE public.clob_month_endoscopeGroup(
         id VARCHAR(36) PRIMARY KEY, 
         startDate TIMESTAMP, 
         endDate TIMESTAMP, 
         statsLeft numeric, 
         lost numeric, 
         fatalError VARCHAR(255),
         appGroup VARCHAR(100),
         appType VARCHAR(100)
     );
     CREATE INDEX clob_endo_gr_month_appGroup ON public.clob_month_endoscopeGroup(appGroup);
     CREATE INDEX clob_endo_gr_month_e_s_t ON public.clob_month_endoscopeGroup(endDate, startDate, appType);
                     
     CREATE TABLE public.clob_month_endoscopeStat(
         id VARCHAR(36) PRIMARY KEY, 
         groupId VARCHAR(36), 
         rootId VARCHAR(36), 
         name VARCHAR(255), 
         hits numeric, 
         err numeric, 
         max numeric, 
         min numeric, 
         avg numeric, 
         ah10 numeric, 
         hasChildren numeric,
         children TEXT
     );
     CREATE INDEX clob_endo_st_month_g_p_n ON public.clob_month_endoscopeStat(groupId, name);
     CREATE INDEX clob_endo_st_month_rootId ON public.clob_month_endoscopeStat(rootId);
