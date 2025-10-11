CREATE VIEW dbo."TaskView"
AS
SELECT "taskDate",
    client,
    project,
    phase,
    SUBSTRING(details, 1, (POSITION(' ' IN details || ' ') - 1)) AS taskid,
    LTRIM(SUBSTRING(details, (POSITION('- ' IN details) + 1), 250)) AS details,
    hours
FROM dbo."TaskActivity"
WHERE POSITION('-' IN SUBSTRING(details, 1, (POSITION(' ' IN details || ' ') - 1))) > 0
