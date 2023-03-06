CREATE TABLE documents
(
    id           SERIAL    NOT NULL PRIMARY KEY,
    name         TEXT      NOT NULL,
    status       TEXT               DEFAULT 'ACTIVE',
    owner_id     TEXT      NOT NULL,
    content_type TEXT               DEFAULT '*/*',
    size         INTEGER   NOT NULL DEFAULT 0,
    version      INTEGER   NOT NULL DEFAULT 0,
    timestamp    TIMESTAMP NOT NULL,
    storage_type TEXT               DEFAULT 'FS',
    content_uri  TEXT               DEFAULT '')