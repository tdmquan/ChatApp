CREATE TABLE users (
    id             BIGSERIAL PRIMARY KEY,
    email          VARCHAR(255) NOT NULL UNIQUE,
    password_hash  VARCHAR(100) NOT NULL,
    first_name     VARCHAR(100),
    last_name      VARCHAR(100),
    avatar_key     VARCHAR(500),
    color          SMALLINT,
    profile_setup  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE conversations (
    id               BIGSERIAL PRIMARY KEY,
    type             VARCHAR(10)  NOT NULL CHECK (type IN ('DIRECT', 'GROUP')),
    name             VARCHAR(100),
    -- DIRECT: "<minUserId>_<maxUserId>" để đảm bảo mỗi cặp user chỉ có 1 conversation
    direct_key       VARCHAR(50) UNIQUE,
    last_message_at  TIMESTAMPTZ,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CHECK ((type = 'DIRECT' AND direct_key IS NOT NULL)
        OR (type = 'GROUP' AND name IS NOT NULL))
);

CREATE TABLE conversation_members (
    conversation_id       BIGINT      NOT NULL REFERENCES conversations (id) ON DELETE CASCADE,
    user_id               BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role                  VARCHAR(10) NOT NULL DEFAULT 'MEMBER' CHECK (role IN ('ADMIN', 'MEMBER')),
    joined_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_read_message_id  BIGINT,
    PRIMARY KEY (conversation_id, user_id)
);
CREATE INDEX idx_conversation_members_user ON conversation_members (user_id);

CREATE TABLE messages (
    id               BIGSERIAL PRIMARY KEY,
    conversation_id  BIGINT       NOT NULL REFERENCES conversations (id) ON DELETE CASCADE,
    sender_id        BIGINT       NOT NULL REFERENCES users (id),
    client_msg_id    UUID         NOT NULL,
    type             VARCHAR(10)  NOT NULL CHECK (type IN ('TEXT', 'FILE')),
    content          TEXT,
    file_key         VARCHAR(500),
    file_name        VARCHAR(255),
    file_size        BIGINT,
    content_type     VARCHAR(100),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_messages_sender_client_msg UNIQUE (sender_id, client_msg_id),
    CHECK ((type = 'TEXT' AND content IS NOT NULL)
        OR (type = 'FILE' AND file_key IS NOT NULL))
);
CREATE INDEX idx_messages_conversation_id ON messages (conversation_id, id DESC);
