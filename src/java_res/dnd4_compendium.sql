CREATE TABLE 'category' (
'id' TEXT PRIMARY KEY NOT NULL,
'name' TEXT NOT NULL,
'count' INTEGER NOT NULL,
'fields' TEXT NOT NULL,
'type' TEXT NOT NULL,
'order' INTEGER NOT NULL);

INSERT INTO "category" ("id","name","count","fields","type","order") VALUES ('Race','Race','-1','Size,DescriptionAttribute,SourceBook','PC','10');
INSERT INTO "category" ("id","name","count","fields","type","order") VALUES ('Background','Background','-1','Type,Campaign,Skills,SourceBook','PC','20');
INSERT INTO "category" ("id","name","count","fields","type","order") VALUES ('Theme','Theme','-1','SourceBook','PC','30');
INSERT INTO "category" ("id","name","count","fields","type","order") VALUES ('Class','Class','-1','PowerSourceText,RoleName,KeyAbilities,SourceBook','PC','40');
INSERT INTO "category" ("id","name","count","fields","type","order") VALUES ('ParagonPath','Paragon Path','-1','Prerequisite,SourceBook','PC','50');
INSERT INTO "category" ("id","name","count","fields","type","order") VALUES ('EpicDestiny','Epic Destiny','-1','Prerequisite,SourceBook','PC','60');
INSERT INTO "category" ("id","name","count","fields","type","order") VALUES ('Feat','Feat','-1','SourceBook,TierName,TierSort','PC','70');
INSERT INTO "category" ("id","name","count","fields","type","order") VALUES ('Power','Power','-1','Level,ActionType,SourceBook,ClassName','PC','80');
INSERT INTO "category" ("id","name","count","fields","type","order") VALUES ('Ritual','Ritual','-1','Level,ComponentCost,Price,KeySkillDescription,SourceBook','PC','90');
INSERT INTO "category" ("id","name","count","fields","type","order") VALUES ('Companion','Companion','-1','Type,SourceBook','PC','100');
INSERT INTO "category" ("id","name","count","fields","type","order") VALUES ('Item','Item','-1','Cost,Level,Rarity,Category,SourceBook,LevelSort,CostSort','PC','110');

CREATE TABLE 'config' ('key' TEXT PRIMARY KEY NOT NULL, 'value' TEXT);

INSERT INTO "config" ("key","value") VALUES ('version','20160706');

CREATE TABLE 'entry' ('id' TEXT PRIMARY KEY NOT NULL, 'name' TEXT, 'fields' TEXT, 'data' TEXT);