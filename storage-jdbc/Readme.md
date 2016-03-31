H2 schema
---------

    CREATE TABLE IF NOT EXISTS endoscopeGroup(
        id VARCHAR(36) PRIMARY KEY, 
        startDate TIMESTAMP, 
        endDate TIMESTAMP, 
        statsLeft INT, 
        lost INT, 
        fatalError VARCHAR(255)
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
     
     CREATE TABLE endoscopeGroup(
         id VARCHAR(36) PRIMARY KEY, 
         startDate TIMESTAMP, 
         endDate TIMESTAMP, 
         statsLeft NUMBER, 
         lost NUMBER, 
         fatalError VARCHAR(255)
     );
     CREATE INDEX test_endo_startDate ON SMARTRDS.endoscopeGroup(startDate);
     CREATE INDEX test_endo_endDate ON SMARTRDS.endoscopeGroup(endDate);     
                     
     CREATE TABLE endoscopeStat(
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
     CREATE INDEX test_endo_parentId ON SMARTRDS.endoscopeStat(parentId);
     CREATE INDEX test_endo_groupId ON SMARTRDS.endoscopeStat(groupId);
     CREATE INDEX test_endo_name ON SMARTRDS.endoscopeStat(name);
     CREATE INDEX test_endo_rootId ON SMARTRDS.endoscopeStat(rootId);
