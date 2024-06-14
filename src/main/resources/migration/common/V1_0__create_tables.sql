-- public.court_type definition

-- Drop table

-- DROP TABLE court_type;

CREATE TABLE court_type (
	id varchar(3) NOT NULL,
	description varchar(80) NOT NULL,
	CONSTRAINT court_type_pkey PRIMARY KEY (id)
);

INSERT INTO court_type (id, description) VALUES('CMT', 'Court Martial')
,('COA', 'Court of Appeal')
,('COU', 'County Court/County Divorce Ct')
,('CRN', 'Crown Court')
,('DIS', 'District Court (Scottish)')
,('HGH', 'High Court (Scottish)')
,('MAG', 'Magistrates Court')
,('OEW', 'Court Outside England/Wales')
,('SHF', 'Sherriff''s Court (Scottish)')
,('YTH', 'Youth Court')
,('COM', 'Community')
,('IMM', 'Immigration Court')
,('OTH', 'Other Court');


-- public.court definition

-- Drop table

-- DROP TABLE court;

CREATE TABLE court (
	id varchar(6) NOT NULL PRIMARY KEY,
	court_name varchar(80) NOT NULL,
	court_description varchar(200) NULL,
	"type" varchar(3) NOT NULL,
	active bool NOT NULL,
	created_datetime timestamp NOT NULL,
	last_updated_datetime timestamp NOT NULL,
	CONSTRAINT court_detail_type_fkey FOREIGN KEY ("type") REFERENCES court_type(id)
);
CREATE INDEX court_detail_idx ON public.court USING btree (type);


-- public.building definition

-- DROP FUNCTION public.court_code_exists(varchar);

CREATE OR REPLACE FUNCTION public.court_code_exists(court_code character varying)
 RETURNS boolean
 LANGUAGE plpgsql
AS $function$
begin
    return exists(select 1 from court c where c.id = court_code);
end
$function$
;

-- Drop table

-- DROP TABLE building;

CREATE TABLE building (
	id SERIAL PRIMARY KEY,
	court_code varchar(6) NOT NULL,
	sub_code varchar(6) NULL,
	building_name varchar(50) NULL,
	street varchar(80) NULL,
	locality varchar(80) NULL,
	town varchar(80) NULL,
	county varchar(80) NULL,
	postcode varchar(8) NULL,
	country varchar(16) NULL,
	created_datetime timestamp NOT NULL,
	last_updated_datetime timestamp NOT NULL,
	active bool DEFAULT true NOT NULL,
	CONSTRAINT sub_code_already_exists CHECK ((NOT court_code_exists(sub_code))),
	CONSTRAINT building_court_code_fkey FOREIGN KEY (court_code) REFERENCES court(id)
);
CREATE UNIQUE INDEX building_subcode_udx ON public.building USING btree (court_code, ((sub_code IS NULL))) WHERE (sub_code IS NULL);
CREATE UNIQUE INDEX building_udx ON public.building USING btree (sub_code);


-- public.contact definition

-- Drop table

-- DROP TABLE contact;

CREATE TABLE contact (
	id SERIAL PRIMARY KEY,
	building_id INTEGER NOT NULL,
	"type" varchar(5) NOT NULL,
	detail varchar(80) NULL,
	created_datetime timestamp NOT NULL,
	last_updated_datetime timestamp NOT NULL,
	CONSTRAINT contact_building_id_fkey FOREIGN KEY (building_id) REFERENCES building(id)
);

-- public.court_text_search source

CREATE OR REPLACE VIEW public.court_text_search
AS SELECT DISTINCT c.id,
    to_tsvector(concat_ws(' '::text, c.id, c.court_name, c.court_description, ct.description, COALESCE(b.sub_code, ''::character varying), COALESCE(b.building_name, ''::character varying), COALESCE(b.street, ''::character varying), COALESCE(b.locality, ''::character varying), COALESCE(b.town, ''::character varying), COALESCE(b.county, ''::character varying), COALESCE(b.postcode, ''::character varying), regexp_replace(COALESCE(b.postcode, ''::character varying)::text, ' '::text, ''::text), COALESCE(cn.type, ''::character varying), COALESCE(cn.detail, ''::character varying), regexp_replace(COALESCE(cn.detail, ''::character varying)::text, ' '::text, ''::text))) AS textsearchvector
   FROM court c
     JOIN court_type ct ON c.type::text = ct.id::text
     LEFT JOIN building b ON c.id::text = b.court_code::text
     LEFT JOIN contact cn ON b.id = cn.building_id;