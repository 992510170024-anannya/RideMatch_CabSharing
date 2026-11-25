-- Run once against a database created by the earlier RideMatch persistence phase.
-- Hibernate ddl-auto=update will add the other new columns and replacement_searches table.
UPDATE ride_memberships
SET membership_status = 'CONFIRMED'
WHERE membership_status = 'ACTIVE';

ALTER TABLE ride_memberships
    MODIFY COLUMN joined_at DATETIME NULL;

ALTER TABLE cancellation_records
    ADD COLUMN policy_decision VARCHAR(30) NOT NULL DEFAULT 'NO_POTENTIAL_LOSS',
    ADD COLUMN policy_explanation VARCHAR(500) NULL;
