alter table quiz_answer add column correct boolean not null default false;

update quiz_answer set correct = true;
