
CREATE TABLE meme_type (
    id INT PRIMARY KEY,
    name VARCHAR(10) UNIQUE
);

INSERT INTO meme_type (id, name) VALUES (1, 'file'), (2, 'link'), (3, 'note');

CREATE TABLE IF NOT EXISTS memes (
  id            LONG NOT NULL AUTO_INCREMENT,

  -- type_id defines which fields have values
  -- when file: filename, description and extension will have values
  -- when link: filename will contain the URL, and description will have value
  -- when note: filename will contain the note title, and description will have value
  type_id       INT NOT NULL,

  filename      VARCHAR(512) NULL,
  description   VARCHAR(4096) NULL,
  extension     VARCHAR(32) NULL,

  -- These two should always be the last columns for making the trigger MemeUpdatedTrigger
  -- work even after changes in the table definition
  created_at TIMESTAMP(2) WITH TIME ZONE DEFAULT (CURRENT_TIMESTAMP(2)),
  updated_at TIMESTAMP(2) WITH TIME ZONE,

  PRIMARY KEY ( id ),
  FOREIGN KEY ( type_id ) REFERENCES meme_type( id )
);

-- The tag format is scope/value
CREATE TABLE IF NOT EXISTS tags (
  id    LONG NOT NULL AUTO_INCREMENT,
  scope VARCHAR(255) NOT NULL,
  name  VARCHAR(255) NULL,

  PRIMARY KEY ( id )
);

-- Relation table between tags and memes. This is a NxN relationship
CREATE TABLE IF NOT EXISTS tags_to_memes (
  id long not null auto_increment,
  meme_id LONG NOT NULL,
  tag_id LONG NOT NULL,
  FOREIGN KEY ( meme_id ) REFERENCES memes ( id ),
  FOREIGN KEY ( tag_id ) REFERENCES tags( id )
);

-- Alias for fuzzy queries
CREATE ALIAS IF NOT EXISTS TOKEN_SET_PARTIAL_RATIO FOR "me.xdrop.fuzzywuzzy.FuzzySearch.tokenSetPartialRatio";

-- Trigger to update meme date
CREATE TRIGGER trigger_meme_updated
BEFORE UPDATE
ON memes FOR EACH ROW
CALL "com.systems.fele.memendex_server.meme.MemeUpdatedTrigger"