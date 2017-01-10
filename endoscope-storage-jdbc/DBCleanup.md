--we don't need detailed stats older than month

delete from endoscopestat 
where groupid in(
  select id 
  from endoscopegroup
  where enddate < now() - interval '35' day
);