-- Use only on the isolated staging database after a k6 run.
-- Replace the quoted numeric run ID with the "Cleanup run ID" printed by k6.
SET @k6_run_id = 'REPLACE_WITH_RUN_ID';
SET @k6_email_pattern = CONCAT('k6-', @k6_run_id, '-%@load-test.invalid');

START TRANSACTION;

DELETE attributes
FROM SPRING_SESSION_ATTRIBUTES AS attributes
JOIN SPRING_SESSION AS sessions
  ON sessions.PRIMARY_ID = attributes.SESSION_PRIMARY_ID
WHERE @k6_run_id REGEXP '^[0-9]+$'
  AND sessions.PRINCIPAL_NAME LIKE @k6_email_pattern;

DELETE FROM SPRING_SESSION
WHERE @k6_run_id REGEXP '^[0-9]+$'
  AND PRINCIPAL_NAME LIKE @k6_email_pattern;

DELETE FROM user_accounts
WHERE @k6_run_id REGEXP '^[0-9]+$'
  AND email_normalized LIKE @k6_email_pattern;

COMMIT;
