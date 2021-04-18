ALTER TABLE failure_report
    MODIFY error_name VARCHAR(1000) NOT NULL;
ALTER TABLE failure_report
    MODIFY error_detail TEXT NOT NULL;
