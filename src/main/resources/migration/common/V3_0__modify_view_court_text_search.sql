DROP VIEW IF EXISTS court_text_search;

CREATE OR REPLACE VIEW court_text_search AS
SELECT DISTINCT
    c.id,
    concat_ws(
            ' ',
            c.id,
            c.court_name,
            c.court_description,
            ct.description,
            COALESCE(b.sub_code, ''),
            COALESCE(b.address_line1, ''),
            COALESCE(b.address_line2, ''),
            COALESCE(b.address_line3, ''),
            COALESCE(b.address_line4, ''),
            COALESCE(b.address_line5, ''),
            COALESCE(b.postcode, ''),
            regexp_replace(COALESCE(b.postcode, ''), ' ', '', 'g'),
            COALESCE(cn.type, ''),
            COALESCE(cn.detail, ''),
            regexp_replace(COALESCE(cn.detail, ''), ' ', '', 'g')
    ) AS plain_text_search
FROM court c
         JOIN court_type ct ON c.type = ct.id
         LEFT JOIN building b ON c.id = b.court_code
         LEFT JOIN contact cn ON b.id = cn.building_id;
