-- After a trusted user has registered, replace the email below with that user's
-- exact normalized address and run this once as a database administrator.
-- Review the affected-row count; it should be exactly one.
UPDATE users
SET user_role = 'ADMIN'
WHERE email = 'replace-with-admin-email@example.com';
