ALTER TABLE vocabulary_items
    ADD COLUMN answers TEXT;

UPDATE vocabulary_items
SET answers = definition;

ALTER TABLE vocabulary_items
    ALTER COLUMN answers SET NOT NULL;
