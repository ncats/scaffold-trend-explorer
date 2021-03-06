-- CREATE TABLE "FRAGMENT_CLASS" 
--    (	"CLASS_ID" NUMBER(*,0) NOT NULL ENABLE, 
-- 	"SMILES" VARCHAR2(4000 BYTE) NOT NULL ENABLE, 
-- 	"ACOUNT" NUMBER(*,0), 
-- 	"BCOUNT" NUMBER(*,0), 
-- 	"SYMMETRY" NUMBER(*,0), 
-- 	"COMPLEXITY" NUMBER(*,0), 
-- 	"HASHKEY1" VARCHAR2(6 BYTE), 
-- 	"HASHKEY2" VARCHAR2(12 BYTE), 
-- 	"HASHKEY3" VARCHAR2(18 BYTE), 
-- 	"SNR" NUMBER(10,5), 
-- 	"APT" NUMBER(10,8), 
-- 	"INSTANCES" NUMBER(*,0), 
-- 	 CONSTRAINT "FRAGMENT_CLASS_PK" PRIMARY KEY ("CLASS_ID"),
-- 	 CONSTRAINT "FRAGMENT_CLASS_UK1" UNIQUE ("SMILES"));
 
-- COMMENT ON COLUMN "FRAGMENT_CLASS"."APT" IS 'average pairwise tanimoto';
-- COMMENT ON COLUMN "FRAGMENT_CLASS"."INSTANCES" IS 'number of unique instances';
 
-- CREATE INDEX "FRAG_CLASS_APT_INDEX" ON "FRAGMENT_CLASS" ("APT");
-- CREATE INDEX "FRAG_CLASS_INST_INDEX" ON "FRAGMENT_CLASS" ("INSTANCES");
-- CREATE INDEX "FRAG_CLASS_SNR_INDEX" ON "FRAGMENT_CLASS" ("SNR");
-- CREATE INDEX "FRAG_CLASS_ACOUNT_INDEX" ON "FRAGMENT_CLASS" ("ACOUNT");
-- CREATE INDEX "FRAG_CLASS_BCOUNT_INDEX" ON "FRAGMENT_CLASS" ("BCOUNT");
-- CREATE INDEX "FRAG_CLASS_COMPLEX_INDEX" ON "FRAGMENT_CLASS" ("COMPLEXITY");
-- CREATE INDEX "FRAG_CLASS_HKEY1_INDEX" ON "FRAGMENT_CLASS" ("HASHKEY1");
-- CREATE INDEX "FRAG_CLASS_HKEY2_INDEX" ON "FRAGMENT_CLASS" ("HASHKEY2");
-- CREATE INDEX "FRAG_CLASS_HKEY3_INDEX" ON "FRAGMENT_CLASS" ("HASHKEY3");
-- CREATE INDEX "FRAG_CLASS_SYM_INDEX" ON "FRAGMENT_CLASS" ("SYMMETRY");

-- CREATE SEQUENCE "FRAGMENT_CLASS_SEQ";

-- CREATE OR REPLACE TRIGGER "FRAGMENT_CLASS_TRG" 
-- 	before insert on "FRAGMENT_CLASS"    
-- 	for each row begin     
--   	   if inserting then       
-- 	      if :NEW."CLASS_ID" is null then
-- 	          select FRAGMENT_CLASS_SEQ.nextval into :NEW."CLASS_ID" 
-- 	              from dual;       
-- 	      end if;    
-- 	   end if; 
-- 	end;
-- /

-- ALTER TRIGGER "FRAGMENT_CLASS_TRG" ENABLE;
-- /

-- CREATE TABLE "FRAGMENT_INSTANCES" 
--    (	"INSTANCE_ID" NUMBER(*,0) NOT NULL ENABLE, 
-- 	"MOLREGNO" NUMBER NOT NULL ENABLE, 
-- 	"CHEMBL_ID" VARCHAR2(32) NOT NULL ENABLE, 
-- 	"CLASS_ID" NUMBER NOT NULL ENABLE, 
-- 	"MOL" CLOB, 
-- 	"SMILES" VARCHAR2(4000 BYTE), 
-- 	"HASHKEY1" VARCHAR2(6 BYTE) NOT NULL ENABLE, 
-- 	"HASHKEY2" VARCHAR2(12 BYTE) NOT NULL ENABLE, 
-- 	"HASHKEY3" VARCHAR2(18 BYTE) NOT NULL ENABLE, 
-- 	"ADIFF" NUMBER(*,0), 
-- 	 CONSTRAINT "FRAGMENT_INSTANCES_PK" PRIMARY KEY ("INSTANCE_ID"));
 
-- COMMENT ON COLUMN "FRAGMENT_INSTANCES"."ADIFF" 
-- 	IS 'number of atoms in parent molecule - atom count of fragment';
 
-- CREATE INDEX "INSTANCES_ADIFF_INDEX" ON "FRAGMENT_INSTANCES" ("ADIFF");
-- CREATE INDEX "INSTANCES_CHEMBL_INDEX" ON "FRAGMENT_INSTANCES" ("CHEMBL_ID");
-- CREATE INDEX "INSTANCES_CLASS_INDEX" ON "FRAGMENT_INSTANCES" ("CLASS_ID");
-- CREATE INDEX "INSTANCES_HASHKEY1_INDEX" ON "FRAGMENT_INSTANCES" ("HASHKEY1");
-- CREATE INDEX "INSTANCES_HASHKEY2_INDEX" ON "FRAGMENT_INSTANCES" ("HASHKEY2");
-- CREATE INDEX "INSTANCES_HASHKEY3_INDEX" ON "FRAGMENT_INSTANCES" ("HASHKEY3");
-- CREATE INDEX "INSTANCES_MOLREGNO_INDEX" ON "FRAGMENT_INSTANCES" ("MOLREGNO");

-- CREATE SEQUENCE "FRAGMENT_INSTANCES_SEQ"; 
-- CREATE OR REPLACE TRIGGER "FRAGMENT_INSTANCES_TRG" 
--  BEFORE INSERT ON FRAGMENT_INSTANCES 
-- 	FOR EACH ROW 
-- 	BEGIN
-- 	  SELECT FRAGMENT_INSTANCES_SEQ.NEXTVAL 
-- 			INTO :NEW.INSTANCE_ID FROM DUAL;
-- 	END;
-- /

-- ALTER TRIGGER "FRAGMENT_INSTANCES_TRG" ENABLE;
-- /

DROP TABLE medmad_log10;
/

CREATE TABLE medmad_log10 AS
SELECT assay_id,
  COUNT(*)             AS nobs,
  median(x)            AS med,
  MEDIAN(ABS(x - med)) AS mad
FROM
  (SELECT assay_id,
    log(10,standard_value)                                     AS x,
    MEDIAN(log(10,standard_value)) OVER(partition BY assay_id) AS med
  FROM activities
  WHERE standard_type in ('Ki','IC50')
  AND standard_units = 'nM'
  AND standard_value > 0
 -- AND standard_flag = 1
  AND standard_relation = '='
  ) sq
GROUP BY assay_id;
/

DELETE FROM medmad_log10 where nobs < 5 or round(mad, 4) = 0;
/

CREATE TABLE activities_robustz
AS
  SELECT a.*,
    (log(10,standard_value) - m.med)/m.mad AS standard_zscore
  FROM activities a,
    medmad_log10 m
  WHERE a.assay_id = m.assay_id
AND standard_value > 0;

CREATE UNIQUE INDEX "ACTIVITIES_ROBUSTZ_PK" ON activities_robustz (activity_id)
CREATE INDEX "ROBUSTZ_ASSAY_INDEX" ON activities_robustz (assay_id);
CREATE INDEX "ROBUSTZ_DOC_INDEX" ON activities_robustz (doc_id);
CREATE INDEX "ROBUSTZ_MOLREGNO_INDEX" ON activities_robustz (molregno);
--CREATE INDEX "ROBUSTZ_RELATION_INDEX" ON "ACTIVITIES_ROBUSTZ" ("STANDARD_RELATION");
--CREATE INDEX "ROBUSTZ_STD_UNITS_INDEX" ON "ACTIVITIES_ROBUSTZ" ("STANDARD_UNITS");
--CREATE INDEX "ROBUSTZ_STD_FLAG_INDEX" ON "ACTIVITIES_ROBUSTZ" ("STANDARD_FLAG");
CREATE INDEX "ROBUSTZ_STD_TYPE_INDEX" ON activities_robustz (standard_type);

DROP INDEX "ROBUSTZ_ZSCORE_INDEX";
CREATE INDEX "ROBUSTZ_ZSCORE_INDEX" ON activities_robustz (standard_zscore);
/

DROP INDEX "set_desc_name_idx";
DROP INDEX "ste_desc_idx";
DROP TABLE ste_descriptors;
CREATE TABLE ste_descriptors (
molregno integer primary key REFERENCES compound_structures (molregno),
qed numeric,
Fsp3 numeric,
logS numeric);
drop table ste_moldoc;
create table ste_moldoc as select molregno, year, journal from activities, docs where activities.doc_id = docs.doc_id and year is not null;
create index idx_moldoc on ste_moldoc(molregno);
