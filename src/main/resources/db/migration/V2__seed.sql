-- =====================================================================
-- AniTrack - V2__seed.sql
-- Demo / development data (PostgreSQL)
-- NOTE: dates and episode counts are approximate demo data.
-- =====================================================================

-- pgcrypto provides crypt() and gen_salt(); its 'bf' option produces
-- BCrypt hashes that Spring Security's BCryptPasswordEncoder accepts.
CREATE EXTENSION IF NOT EXISTS pgcrypto;


-- ---------------------------------------------------------------------
-- USERS  (development accounts only!)
--   admin  / Admin123!
--   tester / Test1234!
--   alice  / Test1234!
--   bob    / Test1234!
-- ---------------------------------------------------------------------
INSERT INTO users (username, email, password_hash, role) VALUES
    ('admin',  'admin@anitrack.local',  crypt('Admin123!', gen_salt('bf', 10)), 'ADMIN'),
    ('tester', 'tester@anitrack.local', crypt('Test1234!', gen_salt('bf', 10)), 'USER'),
    ('alice',  'alice@anitrack.local',  crypt('Test1234!', gen_salt('bf', 10)), 'USER'),
    ('bob',    'bob@anitrack.local',    crypt('Test1234!', gen_salt('bf', 10)), 'USER');

INSERT INTO user_profiles (user_id, bio)
SELECT id,
       CASE WHEN role = 'ADMIN' THEN 'Administrator account.' ELSE 'Demo account.' END
FROM users;


-- ---------------------------------------------------------------------
-- GENRES and TAGS
-- ---------------------------------------------------------------------
INSERT INTO genres (name) VALUES
    ('Action'), ('Adventure'), ('Comedy'), ('Drama'), ('Fantasy'), ('Horror'),
    ('Mystery'), ('Romance'), ('Sci-Fi'), ('Slice of Life'), ('Thriller');

INSERT INTO tags (name) VALUES
    ('Time Travel'), ('School'), ('Samurai'), ('Psychological'),
    ('Isekai'), ('Supernatural'), ('Military');


-- ---------------------------------------------------------------------
-- MEDIA - anime
-- ---------------------------------------------------------------------
INSERT INTO media
    (title, description, media_type, format, start_date, end_date, status,
     total_episodes, duration_minutes)
VALUES
    ('Fullmetal Alchemist: Brotherhood',
     'Two brothers search for the Philosopher''s Stone to restore their bodies after a failed alchemical ritual.',
     'ANIME', 'TV', '2009-04-05', '2010-07-04', 'FINISHED', 64, 24),
    ('Steins;Gate',
     'A self-proclaimed mad scientist discovers that text messages can be sent to the past, with dangerous consequences.',
     'ANIME', 'TV', '2011-04-06', '2011-09-14', 'FINISHED', 24, 24),
    ('Cowboy Bebop',
     'A crew of bounty hunters drifts through the solar system, chasing targets and running from their pasts.',
     'ANIME', 'TV', '1998-04-03', '1999-04-24', 'FINISHED', 26, 24),
    ('Death Note',
     'A student finds a notebook that kills anyone whose name is written in it, and a duel of wits begins.',
     'ANIME', 'TV', '2006-10-04', '2007-06-27', 'FINISHED', 37, 23),
    ('Your Name',
     'Two teenagers in different parts of Japan mysteriously begin swapping bodies.',
     'ANIME', 'MOVIE', '2016-08-26', '2016-08-26', 'FINISHED', 1, 106),
    ('Spirited Away',
     'A girl wanders into a world of spirits and must work in a bathhouse to free her parents.',
     'ANIME', 'MOVIE', '2001-07-20', '2001-07-20', 'FINISHED', 1, 125),
    ('Demon Slayer: Kimetsu no Yaiba',
     'A boy becomes a demon slayer after his family is attacked and his sister is turned into a demon.',
     'ANIME', 'TV', '2019-04-06', '2019-09-28', 'FINISHED', 26, 23),
    ('Frieren: Beyond Journey''s End',
     'An elf mage reflects on the passing of time after the hero of her old adventuring party dies.',
     'ANIME', 'TV', '2023-09-29', '2024-03-22', 'FINISHED', 28, 24),
    ('Demo Airing Anime',
     'Sample entry for testing currently airing titles.',
     'ANIME', 'TV', '2026-10-04', NULL, 'AIRING', NULL, 24),
    ('Demo Upcoming Anime',
     'Sample entry for testing upcoming titles.',
     'ANIME', 'TV', '2027-04-04', NULL, 'UPCOMING', NULL, 24);


-- ---------------------------------------------------------------------
-- MEDIA - manga
-- ---------------------------------------------------------------------
INSERT INTO media
    (title, description, media_type, format, start_date, end_date, status,
     total_chapters, total_volumes)
VALUES
    ('Fullmetal Alchemist',
     'The original manga about two brothers and the price of alchemy.',
     'MANGA', 'MANGA', '2001-07-12', '2010-06-11', 'FINISHED', 108, 27),
    ('Death Note',
     'The original manga about a notebook that decides who lives and who dies.',
     'MANGA', 'MANGA', '2003-12-01', '2006-05-15', 'FINISHED', 108, 12),
    ('Monster',
     'A brilliant surgeon hunts the patient he once saved, who has become a killer.',
     'MANGA', 'MANGA', '1994-12-05', '2001-12-20', 'FINISHED', 162, 18),
    ('Vagabond',
     'A fictional retelling of the life of a wandering swordsman in feudal Japan.',
     'MANGA', 'MANGA', '1998-09-03', NULL, 'HIATUS', 327, 37),
    ('Chainsaw Man',
     'A young man merges with a devil and becomes a devil hunter.',
     'MANGA', 'MANGA', '2018-12-03', NULL, 'RELEASING', NULL, NULL),
    ('Demo Upcoming Manga',
     'Sample entry for testing upcoming titles.',
     'MANGA', 'MANGA', '2027-01-15', NULL, 'UPCOMING', NULL, NULL);


-- ---------------------------------------------------------------------
-- MEDIA_GENRES  (N:M, filled via JOINs on names)
-- media_type is part of the join because anime and manga share titles.
-- ---------------------------------------------------------------------
INSERT INTO media_genres (media_id, genre_id)
SELECT m.id, g.id
FROM (VALUES
    ('Fullmetal Alchemist: Brotherhood', 'ANIME', 'Action'),
    ('Fullmetal Alchemist: Brotherhood', 'ANIME', 'Adventure'),
    ('Fullmetal Alchemist: Brotherhood', 'ANIME', 'Drama'),
    ('Fullmetal Alchemist: Brotherhood', 'ANIME', 'Fantasy'),
    ('Steins;Gate', 'ANIME', 'Sci-Fi'),
    ('Steins;Gate', 'ANIME', 'Drama'),
    ('Steins;Gate', 'ANIME', 'Thriller'),
    ('Cowboy Bebop', 'ANIME', 'Action'),
    ('Cowboy Bebop', 'ANIME', 'Adventure'),
    ('Cowboy Bebop', 'ANIME', 'Sci-Fi'),
    ('Death Note', 'ANIME', 'Mystery'),
    ('Death Note', 'ANIME', 'Thriller'),
    ('Your Name', 'ANIME', 'Romance'),
    ('Your Name', 'ANIME', 'Drama'),
    ('Your Name', 'ANIME', 'Fantasy'),
    ('Spirited Away', 'ANIME', 'Adventure'),
    ('Spirited Away', 'ANIME', 'Fantasy'),
    ('Demon Slayer: Kimetsu no Yaiba', 'ANIME', 'Action'),
    ('Demon Slayer: Kimetsu no Yaiba', 'ANIME', 'Fantasy'),
    ('Frieren: Beyond Journey''s End', 'ANIME', 'Adventure'),
    ('Frieren: Beyond Journey''s End', 'ANIME', 'Fantasy'),
    ('Frieren: Beyond Journey''s End', 'ANIME', 'Drama'),
    ('Demo Airing Anime', 'ANIME', 'Action'),
    ('Demo Upcoming Anime', 'ANIME', 'Action'),
    ('Fullmetal Alchemist', 'MANGA', 'Action'),
    ('Fullmetal Alchemist', 'MANGA', 'Adventure'),
    ('Fullmetal Alchemist', 'MANGA', 'Drama'),
    ('Fullmetal Alchemist', 'MANGA', 'Fantasy'),
    ('Death Note', 'MANGA', 'Mystery'),
    ('Death Note', 'MANGA', 'Thriller'),
    ('Monster', 'MANGA', 'Mystery'),
    ('Monster', 'MANGA', 'Thriller'),
    ('Monster', 'MANGA', 'Drama'),
    ('Vagabond', 'MANGA', 'Action'),
    ('Vagabond', 'MANGA', 'Drama'),
    ('Vagabond', 'MANGA', 'Adventure'),
    ('Chainsaw Man', 'MANGA', 'Action'),
    ('Chainsaw Man', 'MANGA', 'Horror'),
    ('Chainsaw Man', 'MANGA', 'Fantasy'),
    ('Demo Upcoming Manga', 'MANGA', 'Action')
) AS v(title, media_type, genre)
JOIN media  m ON m.title = v.title AND m.media_type = v.media_type
JOIN genres g ON g.name = v.genre;


-- ---------------------------------------------------------------------
-- MEDIA_TAGS  (N:M)
-- ---------------------------------------------------------------------
INSERT INTO media_tags (media_id, tag_id)
SELECT m.id, t.id
FROM (VALUES
    ('Steins;Gate', 'ANIME', 'Time Travel'),
    ('Death Note', 'ANIME', 'Psychological'),
    ('Death Note', 'ANIME', 'Supernatural'),
    ('Death Note', 'MANGA', 'Psychological'),
    ('Death Note', 'MANGA', 'Supernatural'),
    ('Monster', 'MANGA', 'Psychological'),
    ('Your Name', 'ANIME', 'School'),
    ('Your Name', 'ANIME', 'Supernatural'),
    ('Spirited Away', 'ANIME', 'Supernatural'),
    ('Demon Slayer: Kimetsu no Yaiba', 'ANIME', 'Supernatural'),
    ('Vagabond', 'MANGA', 'Samurai'),
    ('Fullmetal Alchemist: Brotherhood', 'ANIME', 'Military'),
    ('Fullmetal Alchemist', 'MANGA', 'Military')
) AS v(title, media_type, tag)
JOIN media m ON m.title = v.title AND m.media_type = v.media_type
JOIN tags  t ON t.name = v.tag;


-- ---------------------------------------------------------------------
-- USER_MEDIA_LIST
-- ---------------------------------------------------------------------
INSERT INTO user_media_list (user_id, media_id, status, progress)
SELECT u.id, m.id, v.status, v.progress
FROM (VALUES
    ('tester', 'Fullmetal Alchemist: Brotherhood', 'ANIME', 'COMPLETED', 64),
    ('tester', 'Steins;Gate', 'ANIME', 'CURRENT', 10),
    ('tester', 'Death Note', 'ANIME', 'PLANNING', 0),
    ('tester', 'Death Note', 'MANGA', 'CURRENT', 40),
    ('tester', 'Frieren: Beyond Journey''s End', 'ANIME', 'COMPLETED', 28),
    ('tester', 'Chainsaw Man', 'MANGA', 'CURRENT', 50),
    ('alice', 'Fullmetal Alchemist: Brotherhood', 'ANIME', 'COMPLETED', 64),
    ('alice', 'Death Note', 'ANIME', 'COMPLETED', 37),
    ('alice', 'Your Name', 'ANIME', 'COMPLETED', 1),
    ('alice', 'Spirited Away', 'ANIME', 'PLANNING', 0),
    ('alice', 'Vagabond', 'MANGA', 'CURRENT', 100),
    ('bob', 'Steins;Gate', 'ANIME', 'COMPLETED', 24),
    ('bob', 'Cowboy Bebop', 'ANIME', 'COMPLETED', 26),
    ('bob', 'Death Note', 'ANIME', 'DROPPED', 12),
    ('bob', 'Demon Slayer: Kimetsu no Yaiba', 'ANIME', 'CURRENT', 8),
    ('bob', 'Fullmetal Alchemist', 'MANGA', 'COMPLETED', 108),
    ('admin', 'Fullmetal Alchemist: Brotherhood', 'ANIME', 'COMPLETED', 64)
) AS v(username, title, media_type, status, progress)
JOIN users u ON u.username = v.username
JOIN media m ON m.title = v.title AND m.media_type = v.media_type;


-- ---------------------------------------------------------------------
-- RATINGS
-- ---------------------------------------------------------------------
INSERT INTO ratings (user_id, media_id, reaction)
SELECT u.id, m.id, v.reaction
FROM (VALUES
    ('tester', 'Fullmetal Alchemist: Brotherhood', 'ANIME', 'POSITIVE'),
    ('tester', 'Steins;Gate', 'ANIME', 'POSITIVE'),
    ('tester', 'Death Note', 'ANIME', 'NEUTRAL'),
    ('tester', 'Frieren: Beyond Journey''s End', 'ANIME', 'POSITIVE'),
    ('alice', 'Fullmetal Alchemist: Brotherhood', 'ANIME', 'POSITIVE'),
    ('alice', 'Death Note', 'ANIME', 'POSITIVE'),
    ('alice', 'Your Name', 'ANIME', 'POSITIVE'),
    ('bob', 'Steins;Gate', 'ANIME', 'POSITIVE'),
    ('bob', 'Cowboy Bebop', 'ANIME', 'POSITIVE'),
    ('bob', 'Death Note', 'ANIME', 'NEGATIVE'),
    ('bob', 'Fullmetal Alchemist', 'MANGA', 'POSITIVE')
) AS v(username, title, media_type, reaction)
JOIN users u ON u.username = v.username
JOIN media m ON m.title = v.title AND m.media_type = v.media_type;


-- ---------------------------------------------------------------------
-- FAVORITES
-- ---------------------------------------------------------------------
INSERT INTO favorites (user_id, media_id, position)
SELECT u.id, m.id, v.position
FROM (VALUES
    ('tester', 'Fullmetal Alchemist: Brotherhood', 'ANIME', 1),
    ('tester', 'Steins;Gate', 'ANIME', 2),
    ('tester', 'Frieren: Beyond Journey''s End', 'ANIME', 3),
    ('alice', 'Your Name', 'ANIME', 1),
    ('alice', 'Spirited Away', 'ANIME', 2)
) AS v(username, title, media_type, position)
JOIN users u ON u.username = v.username
JOIN media m ON m.title = v.title AND m.media_type = v.media_type;


-- ---------------------------------------------------------------------
-- ACTIVITY_LOG  (spread over the last days; days_ago -> created_at)
-- ---------------------------------------------------------------------
INSERT INTO activity_log (user_id, media_id, activity_type, quantity, created_at)
SELECT u.id, m.id, v.activity_type, v.quantity, now() - make_interval(days => v.days_ago)
FROM (VALUES
    ('tester', 'Steins;Gate', 'ANIME', 'MEDIA_ADDED', 1, 12),
    ('tester', 'Steins;Gate', 'ANIME', 'EPISODE_WATCHED', 4, 9),
    ('tester', 'Steins;Gate', 'ANIME', 'EPISODE_WATCHED', 6, 2),
    ('tester', 'Death Note', 'MANGA', 'MEDIA_ADDED', 1, 6),
    ('tester', 'Death Note', 'MANGA', 'CHAPTER_READ', 40, 5),
    ('tester', 'Frieren: Beyond Journey''s End', 'ANIME', 'MEDIA_COMPLETED', 1, 3),
    ('tester', 'Frieren: Beyond Journey''s End', 'ANIME', 'RATING_ADDED', 1, 3),
    ('tester', 'Fullmetal Alchemist: Brotherhood', 'ANIME', 'FAVORITE_ADDED', 1, 1),
    ('alice', 'Your Name', 'ANIME', 'MEDIA_COMPLETED', 1, 4),
    ('alice', 'Vagabond', 'MANGA', 'CHAPTER_READ', 100, 2),
    ('bob', 'Demon Slayer: Kimetsu no Yaiba', 'ANIME', 'EPISODE_WATCHED', 8, 1),
    ('bob', 'Cowboy Bebop', 'ANIME', 'MEDIA_COMPLETED', 1, 7)
) AS v(username, title, media_type, activity_type, quantity, days_ago)
JOIN users u ON u.username = v.username
JOIN media m ON m.title = v.title AND m.media_type = v.media_type;
