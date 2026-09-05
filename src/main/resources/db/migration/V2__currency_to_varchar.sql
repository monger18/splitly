-- V1 declared expense_groups.currency as char(3), which Postgres reports as bpchar.
-- The entity maps it to a String with @Column(length = 3), which Hibernate expects to
-- be varchar(3), so ddl-auto: validate refused to start the application.
--
-- varchar is the better type here anyway: char pads to its full width and compares with
-- trailing spaces ignored, which quietly turns 'IN ' and 'IN' into the same value.
--
-- V1 has already been applied, so it is not edited. Migrations are append-only: editing
-- an applied file changes its checksum and Flyway then refuses to start against any
-- database that already ran the old version.
alter table expense_groups
    alter column currency type varchar(3);

alter table expense_groups
    alter column currency set default 'INR';
