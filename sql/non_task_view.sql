
CREATE VIEW public.nontaskview
AS
SELECT taskdate,
    client,
    project,
    phase,
    details,
    taskhours
FROM public.taskactivity
WHERE POSITION('-' IN SUBSTRING(details, 1, (POSITION(' ' IN details || ' ') - 1))) = 0
