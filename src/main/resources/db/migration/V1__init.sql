-- =====================================================================
-- AniTrack - V1__init.sql
-- Initial schema (PostgreSQL)
-- =====================================================================


-- ---------------------------------------------------------------------
-- 1. USERS  (user accounts)
-- ---------------------------------------------------------------------
CREATE TABLE users (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username      VARCHAR(30)  NOT NULL UNIQUE,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,            -- BCrypt hash, never plaintext
    role          VARCHAR(10)  NOT NULL DEFAULT 'USER',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_users_role CHECK (role IN ('USER', 'ADMIN'))
);


-- ---------------------------------------------------------------------
-- 2. USER_PROFILES  (1:1 with users, user_id is both PK and FK)
-- ---------------------------------------------------------------------
CREATE TABLE user_profiles (
    user_id          BIGINT PRIMARY KEY,
    avatar_url       VARCHAR(500),
    banner_url       VARCHAR(500),
    bio              VARCHAR(500),
    theme_preference VARCHAR(10) NOT NULL DEFAULT 'SYSTEM',
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_profiles_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_profiles_theme
        CHECK (theme_preference IN ('LIGHT', 'DARK', 'SYSTEM'))
);


-- ---------------------------------------------------------------------
-- 3. MEDIA  (shared anime / manga catalog)
-- ---------------------------------------------------------------------
CREATE TABLE media (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title            VARCHAR(255) NOT NULL,
    description      TEXT,
    media_type       VARCHAR(10)  NOT NULL,
    format           VARCHAR(20)  NOT NULL,
    start_date       DATE,
    end_date         DATE,
    status           VARCHAR(20)  NOT NULL,
    total_episodes   INTEGER,
    total_chapters   INTEGER,
    total_volumes    INTEGER,
    duration_minutes INTEGER,                       -- minutes per episode (anime)
    cover_image_url  VARCHAR(500),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT chk_media_type
        CHECK (media_type IN ('ANIME', 'MANGA')),

    -- the format must match the media type
    CONSTRAINT chk_media_format CHECK (
        (media_type = 'ANIME' AND format IN ('TV', 'MOVIE', 'OVA', 'ONA', 'SPECIAL'))
        OR
        (media_type = 'MANGA' AND format IN ('MANGA', 'ONE_SHOT', 'NOVEL'))
    ),

    -- the status must match the media type (AIRING for anime, RELEASING for manga)
    CONSTRAINT chk_media_status CHECK (
        (media_type = 'ANIME' AND status IN ('UPCOMING', 'AIRING', 'FINISHED', 'HIATUS'))
        OR
        (media_type = 'MANGA' AND status IN ('UPCOMING', 'RELEASING', 'FINISHED', 'HIATUS'))
    ),

    -- anime has no chapters/volumes, manga has no episodes/duration
    CONSTRAINT chk_media_type_fields CHECK (
        (media_type = 'ANIME' AND total_chapters IS NULL AND total_volumes IS NULL)
        OR
        (media_type = 'MANGA' AND total_episodes IS NULL AND duration_minutes IS NULL)
    ),

    CONSTRAINT chk_media_numbers CHECK (
        (total_episodes   IS NULL OR total_episodes   > 0) AND
        (total_chapters   IS NULL OR total_chapters   > 0) AND
        (total_volumes    IS NULL OR total_volumes    > 0) AND
        (duration_minutes IS NULL OR duration_minutes > 0)
    ),

    CONSTRAINT chk_media_dates
        CHECK (end_date IS NULL OR start_date IS NULL OR end_date >= start_date)
);

CREATE INDEX idx_media_type_status ON media (media_type, status);


-- ---------------------------------------------------------------------
-- 4. GENRES and MEDIA_GENRES  (N:M)
-- ---------------------------------------------------------------------
CREATE TABLE genres (
    id   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE media_genres (
    media_id BIGINT NOT NULL,
    genre_id BIGINT NOT NULL,
    PRIMARY KEY (media_id, genre_id),               -- composite PK prevents duplicates
    CONSTRAINT fk_media_genres_media
        FOREIGN KEY (media_id) REFERENCES media (id)  ON DELETE CASCADE,
    CONSTRAINT fk_media_genres_genre
        FOREIGN KEY (genre_id) REFERENCES genres (id) ON DELETE CASCADE
);

CREATE INDEX idx_media_genres_genre ON media_genres (genre_id);


-- ---------------------------------------------------------------------
-- 5. TAGS and MEDIA_TAGS  (N:M)
-- ---------------------------------------------------------------------
CREATE TABLE tags (
    id   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE media_tags (
    media_id BIGINT NOT NULL,
    tag_id   BIGINT NOT NULL,
    PRIMARY KEY (media_id, tag_id),
    CONSTRAINT fk_media_tags_media
        FOREIGN KEY (media_id) REFERENCES media (id) ON DELETE CASCADE,
    CONSTRAINT fk_media_tags_tag
        FOREIGN KEY (tag_id)   REFERENCES tags (id)  ON DELETE CASCADE
);

CREATE INDEX idx_media_tags_tag ON media_tags (tag_id);


-- ---------------------------------------------------------------------
-- 6. USER_MEDIA_LIST  (a user's personal list; users N:M media)
-- ---------------------------------------------------------------------
CREATE TABLE user_media_list (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id      BIGINT      NOT NULL,
    media_id     BIGINT      NOT NULL,
    status       VARCHAR(20) NOT NULL,
    progress     INTEGER     NOT NULL DEFAULT 0,    -- episodes (anime) or chapters (manga)
    started_at   DATE,
    completed_at DATE,
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_uml_user
        FOREIGN KEY (user_id)  REFERENCES users (id)  ON DELETE CASCADE,
    CONSTRAINT fk_uml_media
        FOREIGN KEY (media_id) REFERENCES media (id)  ON DELETE CASCADE,
    CONSTRAINT uq_uml_user_media UNIQUE (user_id, media_id),
    CONSTRAINT chk_uml_status
        CHECK (status IN ('PLANNING', 'CURRENT', 'COMPLETED', 'PAUSED', 'DROPPED')),
    CONSTRAINT chk_uml_progress
        CHECK (progress >= 0),
    CONSTRAINT chk_uml_dates
        CHECK (completed_at IS NULL OR started_at IS NULL OR completed_at >= started_at)
);

CREATE INDEX idx_uml_media ON user_media_list (media_id);


-- ---------------------------------------------------------------------
-- 7. RATINGS  (one reaction per user per media)
-- ---------------------------------------------------------------------
CREATE TABLE ratings (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id    BIGINT      NOT NULL,
    media_id   BIGINT      NOT NULL,
    reaction   VARCHAR(10) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_ratings_user
        FOREIGN KEY (user_id)  REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_ratings_media
        FOREIGN KEY (media_id) REFERENCES media (id) ON DELETE CASCADE,
    CONSTRAINT uq_ratings_user_media UNIQUE (user_id, media_id),
    CONSTRAINT chk_ratings_reaction
        CHECK (reaction IN ('NEGATIVE', 'NEUTRAL', 'POSITIVE'))
);

CREATE INDEX idx_ratings_media ON ratings (media_id);


-- ---------------------------------------------------------------------
-- 8. FAVORITES  (max 6 per user, enforced by the database)
-- ---------------------------------------------------------------------
CREATE TABLE favorites (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id    BIGINT      NOT NULL,
    media_id   BIGINT      NOT NULL,
    position   SMALLINT    NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_favorites_user
        FOREIGN KEY (user_id)  REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_favorites_media
        FOREIGN KEY (media_id) REFERENCES media (id) ON DELETE CASCADE,
    CONSTRAINT uq_favorites_user_media UNIQUE (user_id, media_id),
    -- DEFERRABLE: positions can be swapped within one transaction
    CONSTRAINT uq_favorites_user_position
        UNIQUE (user_id, position) DEFERRABLE INITIALLY DEFERRED,
    CONSTRAINT chk_favorites_position
        CHECK (position BETWEEN 1 AND 6)
);


-- ---------------------------------------------------------------------
-- 9. ACTIVITY_LOG  (events, not just the current state)
-- ---------------------------------------------------------------------
CREATE TABLE activity_log (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id       BIGINT      NOT NULL,
    media_id      BIGINT,
    activity_type VARCHAR(20) NOT NULL,
    quantity      INTEGER     NOT NULL DEFAULT 1,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_activity_user
        FOREIGN KEY (user_id)  REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_activity_media
        FOREIGN KEY (media_id) REFERENCES media (id) ON DELETE CASCADE,
    CONSTRAINT chk_activity_type CHECK (activity_type IN (
        'MEDIA_ADDED', 'STATUS_CHANGED', 'EPISODE_WATCHED', 'CHAPTER_READ',
        'MEDIA_COMPLETED', 'RATING_ADDED', 'FAVORITE_ADDED'
    )),
    CONSTRAINT chk_activity_quantity
        CHECK (quantity > 0)
);

-- used by the heatmap query (per user, grouped by date)
CREATE INDEX idx_activity_user_date ON activity_log (user_id, created_at);


-- ---------------------------------------------------------------------
-- 10. FRIENDSHIPS  (planned extension)
-- ---------------------------------------------------------------------
CREATE TABLE friendships (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    requester_id BIGINT      NOT NULL,
    addressee_id BIGINT      NOT NULL,
    status       VARCHAR(10) NOT NULL DEFAULT 'PENDING',
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_friendships_requester
        FOREIGN KEY (requester_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_friendships_addressee
        FOREIGN KEY (addressee_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_friendships_not_self
        CHECK (requester_id <> addressee_id),
    CONSTRAINT chk_friendships_status
        CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED'))
);

-- one relationship per pair of users, regardless of direction:
-- (1,2) and (2,1) both become (1,2), so the second one is rejected
CREATE UNIQUE INDEX uq_friendships_pair
    ON friendships (LEAST(requester_id, addressee_id),
                    GREATEST(requester_id, addressee_id));

CREATE INDEX idx_friendships_addressee ON friendships (addressee_id);
