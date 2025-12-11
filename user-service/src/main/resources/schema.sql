CREATE SCHEMA IF NOT EXISTS intelbotdb;

-- Enable pgvector extension (requires appropriate DB privileges)
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS intelbotdb.users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Conversations: logical grouping of messages per user
CREATE TABLE IF NOT EXISTS intelbotdb.conversations (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES intelbotdb.users (id),
    title VARCHAR(255),
    -- immutable external identifier for client-side chat references
    external_id VARCHAR(64) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Individual messages (user or AI)
CREATE TABLE IF NOT EXISTS intelbotdb.messages (
    id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT NOT NULL REFERENCES intelbotdb.conversations (id),
    user_id BIGINT NOT NULL REFERENCES intelbotdb.users (id),
    role VARCHAR(20) NOT NULL, -- 'USER' or 'AI'
    content TEXT NOT NULL,
    content_type VARCHAR(20) NOT NULL DEFAULT 'TEXT', -- 'TEXT' or 'HTML'
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Embeddings for semantic search
CREATE TABLE IF NOT EXISTS intelbotdb.message_embeddings (
    id BIGSERIAL PRIMARY KEY,
    message_id BIGINT NOT NULL REFERENCES intelbotdb.messages (id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES intelbotdb.users (id),
    embedding VECTOR(1536) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Helpful indexes
CREATE INDEX IF NOT EXISTS idx_conversations_user_created_at
    ON intelbotdb.conversations (user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_messages_user_created_at
    ON intelbotdb.messages (user_id, created_at DESC);

-- Vector index for fast similarity search (cosine distance)
CREATE INDEX IF NOT EXISTS idx_message_embeddings_embedding
    ON intelbotdb.message_embeddings
    USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);

