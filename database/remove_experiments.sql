-- Script for manually removing experiments
-- and all the data related to them

BEGIN TRANSACTION;
CREATE TEMP TABLE expids as 
SELECT unnest(ARRAY[
-- \/ ids list separated by commas \/
--     for example: 1,2,3,4,5,6

1,2,3,4,5,6

-------------------------------------
]) as exp_id;

CREATE TEMP TABLE sids as (
	SELECT session_id 
	FROM sessions
	WHERE exp_id IN (SELECT exp_id FROM expids)
);

DELETE FROM sessionevents
WHERE session_id IN (SELECT session_id FROM sids);

DELETE FROM sessionsfeedback
WHERE session_id IN (SELECT session_id FROM sids);

DELETE FROM sessions
WHERE session_id IN (SELECT session_id FROM sids);

DELETE FROM experimentsfeedback
WHERE exp_id IN (SELECT exp_id FROM expids);

DELETE FROM experiments
WHERE exp_id IN (SELECT exp_id FROM expids);

-- return the end result to be able to review before committing
SELECT * FROM experiments
ORDER BY exp_id DESC;
