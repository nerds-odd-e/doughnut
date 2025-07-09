-- Delete failure_report records older than 2025-01-01
DELETE FROM failure_report WHERE created_datetime < '2025-01-01 00:00:00';
