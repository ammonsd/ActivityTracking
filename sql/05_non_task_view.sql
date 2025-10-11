
CREATE VIEW dbo."NonTaskView"
AS
SELECT "taskDate",
    client,
    project,
    phase,
    details,
    hours
FROM dbo."TaskActivity"
WHERE POSITION('-' IN SUBSTRING(details, 1, (POSITION(' ' IN details || ' ') - 1))) = 0
