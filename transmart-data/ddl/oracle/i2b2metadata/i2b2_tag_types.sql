--
-- Type: SEQUENCE; Owner: I2B2METADATA; Name: I2B2_TAG_TYPES_TAG_TYPE_ID_SEQ
--
CREATE SEQUENCE  "I2B2METADATA"."I2B2_TAG_TYPES_TAG_TYPE_ID_SEQ"  MINVALUE 1 MAXVALUE 999999999999999999999999999 INCREMENT BY 1 START WITH 1 CACHE 20 NOORDER  NOCYCLE  NOPARTITION ;
--
-- Type: TABLE; Owner: I2B2METADATA; Name: I2B2_TAG_TYPES
--
 CREATE TABLE "I2B2METADATA"."I2B2_TAG_TYPES"
  (	"TAG_TYPE_ID" NUMBER(18,0) NOT NULL ENABLE,
"TAG_TYPE" VARCHAR2(255 BYTE) NOT NULL ENABLE,
"SOLR_FIELD_NAME" VARCHAR2(255 BYTE),
"NODE_TYPE" VARCHAR2(255 BYTE) NOT NULL ENABLE,
"VALUE_TYPE" VARCHAR2(255 BYTE) NOT NULL ENABLE,
"SHOWN_IF_EMPTY" CHAR(1 BYTE) NOT NULL ENABLE,
"index" NUMBER,
 CONSTRAINT "I2B2_TAG_TYPES_PKEY" PRIMARY KEY ("TAG_TYPE_ID")
 USING INDEX
 TABLESPACE "INDX"  ENABLE,
 CONSTRAINT "I2B2_TAG_TYPES_NODE_TAG_KEY" UNIQUE ("NODE_TYPE", "TAG_TYPE")
 USING INDEX
 TABLESPACE "INDX"  ENABLE
  ) SEGMENT CREATION DEFERRED
NOCOMPRESS LOGGING
 TABLESPACE "TRANSMART" ;
--
-- Type: TRIGGER; Owner: I2B2METADATA; Name: TRG_I2B2_TAG_TYPES_ID
--
  CREATE OR REPLACE TRIGGER "I2B2METADATA"."TRG_I2B2_TAG_TYPES_ID"
   before insert on "I2B2METADATA"."I2B2_TAG_TYPES"
   for each row
begin
   if inserting then
      if :NEW."TAG_TYPE_ID" is null then
         select I2B2_TAG_TYPES_TAG_TYPE_ID_SEQ.nextval into :NEW."TAG_TYPE_ID" from dual;
      end if;
   end if;
end;
/
ALTER TRIGGER "I2B2METADATA"."TRG_I2B2_TAG_TYPES_ID" ENABLE;
