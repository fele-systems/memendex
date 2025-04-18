create table if not exists MEMES (
  ID long not null AUTO_INCREMENT,
  FILENAME varchar(255) not null,
  DESCRIPTION varchar(4096) null,

  -- These two should always be the last columns for making the trigger MemeUpdatedTrigger work
  -- even after changes in the table definition
  CREATED TIMESTAMP(2) WITH TIME ZONE DEFAULT (CURRENT_TIMESTAMP(2)),
  UPDATED TIMESTAMP(2) WITH TIME ZONE,

  PRIMARY KEY ( ID )
);

create table if not exists TAGS (
  ID long not null AUTO_INCREMENT,
  SCOPE varchar(255) not null,
  "VALUE" varchar(255) null,
  PRIMARY KEY ( ID )
);

create table if not exists TAGS_TO_MEMES (
  ID long not null AUTO_INCREMENT,
  MEME_ID long not null,
  TAG_ID long not null,
  FOREIGN KEY (MEME_ID) REFERENCES MEMES(ID),
  FOREIGN KEY (TAG_ID) REFERENCES TAGS(ID)
);

create alias if not exists TOKEN_SET_PARTIAL_RATIO FOR "me.xdrop.fuzzywuzzy.FuzzySearch.tokenSetPartialRatio";

CREATE TRIGGER MEME_UPDATED
BEFORE UPDATE
ON MEMES FOR EACH ROW
CALL "com.systems.fele.memendex_server.meme.MemeUpdatedTrigger"