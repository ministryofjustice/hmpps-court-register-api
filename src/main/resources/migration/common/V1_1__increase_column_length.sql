drop view court_text_search;

alter table court_type alter column id TYPE varchar(80);
alter table court alter column id TYPE varchar(80);
alter table court alter column "type" TYPE varchar(80);
alter table building alter column court_code type varchar(80);
alter table building alter column sub_code type varchar(80);

CREATE OR REPLACE VIEW public.court_text_search
AS SELECT DISTINCT c.id,
    to_tsvector(concat_ws(' '::text, c.id, c.court_name, c.court_description, ct.description, COALESCE(b.sub_code, ''::character varying), COALESCE(b.building_name, ''::character varying), COALESCE(b.street, ''::character varying), COALESCE(b.locality, ''::character varying), COALESCE(b.town, ''::character varying), COALESCE(b.county, ''::character varying), COALESCE(b.postcode, ''::character varying), regexp_replace(COALESCE(b.postcode, ''::character varying)::text, ' '::text, ''::text), COALESCE(cn.type, ''::character varying), COALESCE(cn.detail, ''::character varying), regexp_replace(COALESCE(cn.detail, ''::character varying)::text, ' '::text, ''::text))) AS textsearchvector
   FROM court c
     JOIN court_type ct ON c.type::text = ct.id::text
     LEFT JOIN building b ON c.id::text = b.court_code::text
     LEFT JOIN contact cn ON b.id = cn.building_id;