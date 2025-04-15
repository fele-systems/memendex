create table MEMES (
  ID long not null AUTO_INCREMENT,
  FILENAME varchar(255) not null,
  DESCRIPTION varchar(4096) null,
  PRIMARY KEY ( ID )
);

create table TAGS (
  ID long not null AUTO_INCREMENT,
  SCOPE varchar(255) not null,
  "VALUE" varchar(255) null,
  PRIMARY KEY ( ID )
);

create table TAGS_TO_MEMES (
  ID long not null AUTO_INCREMENT,
  MEME_ID long not null,
  TAG_ID long not null,
  FOREIGN KEY (MEME_ID) REFERENCES MEMES(ID),
  FOREIGN KEY (TAG_ID) REFERENCES TAGS(ID)
);

create alias TOKEN_SET_PARTIAL_RATIO FOR "me.xdrop.fuzzywuzzy.FuzzySearch.tokenSetPartialRatio"