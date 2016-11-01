--Print size of Endoscope tables
SELECT *, pg_size_pretty(total_bytes) AS total
    , pg_size_pretty(index_bytes) AS INDEX
    , pg_size_pretty(toast_bytes) AS toast
    , pg_size_pretty(table_bytes) AS TABLE
  FROM (
  SELECT *, total_bytes-index_bytes-COALESCE(toast_bytes,0) AS table_bytes FROM (
      SELECT c.oid,nspname AS table_schema, relname AS TABLE_NAME
              , c.reltuples AS row_estimate
              , pg_total_relation_size(c.oid) AS total_bytes
              , pg_indexes_size(c.oid) AS index_bytes
              , pg_total_relation_size(reltoastrelid) AS toast_bytes
          FROM pg_class c
          LEFT JOIN pg_namespace n ON n.oid = c.relnamespace
          WHERE relkind = 'r'
          AND relname like '%endoscope%'
  ) a
) a;

SELECT count(*) from endoscopegroup;
SELECT count(*) from day_endoscopegroup;
SELECT count(*) from week_endoscopegroup;
SELECT count(*) from month_endoscopegroup;

SELECT id, startdate, enddate from month_endoscopegroup where 1=1
and apptype = 'type'
and startdate >= to_timestamp('2016-09-01 00:00:00', 'YYYY-MM-DD HH24:MI:SS')
and startdate  < to_timestamp('2016-10-20 00:00:00', 'YYYY-MM-DD HH24:MI:SS')
order by startdate;
