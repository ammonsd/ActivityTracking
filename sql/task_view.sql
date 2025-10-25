CREATE VIEW public.taskview
AS
SELECT taskdate,
    client,
    project,
    phase,
    SUBSTRING(details, 1, (POSITION(' ' IN details || ' ') - 1)) AS taskid,
    LTRIM(SUBSTRING(details, (POSITION('- ' IN details) + 1), 250)) AS details,
    taskhours
FROM public.taskactivity
WHERE POSITION('-' IN SUBSTRING(details, 1, (POSITION(' ' IN details || ' ') - 1))) > 0
