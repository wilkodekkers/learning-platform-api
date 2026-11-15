CREATE TABLE practice_sessions
(
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT      NOT NULL,
    chapter_id BIGINT      NOT NULL,
    status     VARCHAR(20) NOT NULL,
    started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ended_at   TIMESTAMPTZ,

    CONSTRAINT fk_practice_sessions_user
        FOREIGN KEY (user_id)
            REFERENCES users (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_practice_sessions_chapter
        FOREIGN KEY (chapter_id)
            REFERENCES chapters (id)
            ON DELETE CASCADE,

    CONSTRAINT chk_practice_sessions_status
        CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'ABANDONED'))
);

CREATE UNIQUE INDEX uq_practice_sessions_one_open_per_user
    ON practice_sessions (user_id)
    WHERE status = 'IN_PROGRESS';

CREATE INDEX idx_practice_sessions_chapter_id
    ON practice_sessions (chapter_id);

CREATE TABLE practice_session_items
(
    id                  BIGSERIAL PRIMARY KEY,
    session_id          BIGINT       NOT NULL,
    vocabulary_item_id  BIGINT,
    position            INT          NOT NULL,
    term_snapshot       VARCHAR(255) NOT NULL,
    definition_snapshot TEXT         NOT NULL,
    answers_snapshot    TEXT         NOT NULL,
    submitted_answer    TEXT,
    result              VARCHAR(20),
    answered_at         TIMESTAMPTZ,

    CONSTRAINT fk_practice_session_items_session
        FOREIGN KEY (session_id)
            REFERENCES practice_sessions (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_practice_session_items_vocabulary_item
        FOREIGN KEY (vocabulary_item_id)
            REFERENCES vocabulary_items (id)
            ON DELETE SET NULL,

    CONSTRAINT uq_practice_session_items_position
        UNIQUE (session_id, position),

    CONSTRAINT chk_practice_session_items_result
        CHECK (result IN ('CORRECT', 'INCORRECT', 'SKIPPED'))
);

CREATE INDEX idx_practice_session_items_vocabulary_item_id
    ON practice_session_items (vocabulary_item_id);
