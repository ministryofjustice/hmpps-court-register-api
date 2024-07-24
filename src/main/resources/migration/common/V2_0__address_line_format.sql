drop view court_text_search;

ALTER TABLE building RENAME COLUMN building_name TO address_line1;
ALTER TABLE building RENAME COLUMN street TO address_line2;
ALTER TABLE building RENAME COLUMN locality TO address_line3;
ALTER TABLE building RENAME COLUMN town TO address_line4;
ALTER TABLE building RENAME COLUMN county TO address_line5;
ALTER TABLE building DROP COLUMN country;


CREATE OR REPLACE VIEW public.court_text_search
AS SELECT DISTINCT c.id,
    to_tsvector(concat_ws(' '::text, c.id, c.court_name, c.court_description, ct.description, COALESCE(b.sub_code, ''::character varying), COALESCE(b.address_line1, ''::character varying), COALESCE(b.address_line2, ''::character varying), COALESCE(b.address_line4, ''::character varying), COALESCE(b.address_line3, ''::character varying), COALESCE(b.address_line5, ''::character varying), COALESCE(b.postcode, ''::character varying), regexp_replace(COALESCE(b.postcode, ''::character varying)::text, ' '::text, ''::text), COALESCE(cn.type, ''::character varying), COALESCE(cn.detail, ''::character varying), regexp_replace(COALESCE(cn.detail, ''::character varying)::text, ' '::text, ''::text))) AS textsearchvector
   FROM court c
     JOIN court_type ct ON c.type::text = ct.id::text
     LEFT JOIN building b ON c.id::text = b.court_code::text
     LEFT JOIN contact cn ON b.id = cn.building_id;