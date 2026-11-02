--
-- PostgreSQL database dump
--

\restrict RnMdfscsStEQafSOoWEOLVJ40pD0ESkf7r3UtXPKB9BcbsqL66spTDXfKk1877n

-- Dumped from database version 18.6 (Debian 18.6-1.pgdg13+2)
-- Dumped by pg_dump version 18.6

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: aggregated_data_profile; Type: TABLE; Schema: public; Owner: iko-admin
--

CREATE TABLE public.aggregated_data_profile (
    id uuid CONSTRAINT profile_id_not_null NOT NULL,
    name character varying(255),
    primary_endpoint uuid,
    transform text,
    roles character varying(255),
    connector_instance_id uuid,
    connector_endpoint_id uuid,
    cache_enabled boolean DEFAULT false NOT NULL,
    cache_ttl integer DEFAULT 0 NOT NULL,
    endpoint_transform text DEFAULT '.'::text,
    version character varying(50) DEFAULT '1.0.0'::character varying NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    jsonschema text,
    status character varying(10) DEFAULT 'DRAFT'::character varying NOT NULL
);


ALTER TABLE public.aggregated_data_profile OWNER TO "iko-admin";

--
-- Name: connector; Type: TABLE; Schema: public; Owner: iko-admin
--

CREATE TABLE public.connector (
    id uuid NOT NULL,
    name character varying(255) NOT NULL,
    tag character varying(255),
    connector_code text,
    version character varying(50) DEFAULT '1.0.0'::character varying NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    status character varying(10) DEFAULT 'DRAFT'::character varying NOT NULL
);


ALTER TABLE public.connector OWNER TO "iko-admin";

--
-- Name: connector_endpoint; Type: TABLE; Schema: public; Owner: iko-admin
--

CREATE TABLE public.connector_endpoint (
    id uuid NOT NULL,
    name character varying(255) NOT NULL,
    connector_id uuid NOT NULL,
    operation character varying(255) NOT NULL
);


ALTER TABLE public.connector_endpoint OWNER TO "iko-admin";

--
-- Name: connector_endpoint_role; Type: TABLE; Schema: public; Owner: iko-admin
--

CREATE TABLE public.connector_endpoint_role (
    id uuid NOT NULL,
    connector_endpoint_id uuid NOT NULL,
    connector_instance_id uuid NOT NULL,
    role character varying(255) NOT NULL
);


ALTER TABLE public.connector_endpoint_role OWNER TO "iko-admin";

--
-- Name: connector_instance; Type: TABLE; Schema: public; Owner: iko-admin
--

CREATE TABLE public.connector_instance (
    id uuid NOT NULL,
    name character varying(255) NOT NULL,
    connector_id uuid NOT NULL,
    tag character varying(255),
    config text,
    api_specification_url text
);


ALTER TABLE public.connector_instance OWNER TO "iko-admin";

--
-- Name: connector_instance_config; Type: TABLE; Schema: public; Owner: iko-admin
--

CREATE TABLE public.connector_instance_config (
    connector_instance_id uuid NOT NULL,
    key character varying(255) NOT NULL,
    value text NOT NULL
);


ALTER TABLE public.connector_instance_config OWNER TO "iko-admin";

--
-- Name: endpoint; Type: TABLE; Schema: public; Owner: iko-admin
--

CREATE TABLE public.endpoint (
    id uuid CONSTRAINT search_id_not_null NOT NULL,
    name character varying(255) CONSTRAINT search_name_not_null NOT NULL,
    route_id character varying(500) CONSTRAINT search_route_id_not_null NOT NULL,
    created_on timestamp without time zone DEFAULT CURRENT_TIMESTAMP CONSTRAINT search_created_on_not_null NOT NULL,
    modified_on timestamp without time zone,
    is_primary boolean DEFAULT false CONSTRAINT search_is_primary_not_null NOT NULL,
    is_active boolean DEFAULT true CONSTRAINT search_is_active_not_null NOT NULL
);


ALTER TABLE public.endpoint OWNER TO "iko-admin";

--
-- Name: flyway_schema_history; Type: TABLE; Schema: public; Owner: iko-admin
--

CREATE TABLE public.flyway_schema_history (
    installed_rank integer NOT NULL,
    version character varying(50),
    description character varying(200) NOT NULL,
    type character varying(20) NOT NULL,
    script character varying(1000) NOT NULL,
    checksum integer,
    installed_by character varying(100) NOT NULL,
    installed_on timestamp without time zone DEFAULT now() NOT NULL,
    execution_time integer NOT NULL,
    success boolean NOT NULL
);


ALTER TABLE public.flyway_schema_history OWNER TO "iko-admin";

--
-- Name: logging_event_id_seq; Type: SEQUENCE; Schema: public; Owner: iko-admin
--

CREATE SEQUENCE public.logging_event_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.logging_event_id_seq OWNER TO "iko-admin";

--
-- Name: logging_event; Type: TABLE; Schema: public; Owner: iko-admin
--

CREATE TABLE public.logging_event (
    timestmp bigint NOT NULL,
    formatted_message text NOT NULL,
    logger_name character varying(254) NOT NULL,
    level_string character varying(254) NOT NULL,
    thread_name character varying(254),
    reference_flag smallint,
    arg0 character varying(254),
    arg1 character varying(254),
    arg2 character varying(254),
    arg3 character varying(254),
    caller_filename character varying(254) NOT NULL,
    caller_class character varying(254) NOT NULL,
    caller_method character varying(254) NOT NULL,
    caller_line character(4) NOT NULL,
    event_id bigint DEFAULT nextval('public.logging_event_id_seq'::regclass) NOT NULL
);


ALTER TABLE public.logging_event OWNER TO "iko-admin";

--
-- Name: logging_event_exception; Type: TABLE; Schema: public; Owner: iko-admin
--

CREATE TABLE public.logging_event_exception (
    event_id bigint NOT NULL,
    i smallint NOT NULL,
    trace_line text NOT NULL
);


ALTER TABLE public.logging_event_exception OWNER TO "iko-admin";

--
-- Name: logging_event_property; Type: TABLE; Schema: public; Owner: iko-admin
--

CREATE TABLE public.logging_event_property (
    event_id bigint NOT NULL,
    mapped_key character varying(254) NOT NULL,
    mapped_value character varying(1024)
);


ALTER TABLE public.logging_event_property OWNER TO "iko-admin";

--
-- Name: relation; Type: TABLE; Schema: public; Owner: iko-admin
--

CREATE TABLE public.relation (
    id uuid NOT NULL,
    aggregated_data_profile_id uuid CONSTRAINT relation_profile_id_not_null NOT NULL,
    source_id uuid,
    source_to_endpoint_mapping text,
    endpoint_id character varying(255),
    transform text,
    connector_instance_id uuid,
    connector_endpoint_id uuid,
    property_name character varying(255) NOT NULL,
    cache_enabled boolean DEFAULT false NOT NULL,
    cache_ttl integer DEFAULT 0 NOT NULL
);


ALTER TABLE public.relation OWNER TO "iko-admin";

--
-- Data for Name: aggregated_data_profile; Type: TABLE DATA; Schema: public; Owner: iko-admin
--

COPY public.aggregated_data_profile (id, name, primary_endpoint, transform, roles, connector_instance_id, connector_endpoint_id, cache_enabled, cache_ttl, endpoint_transform, version, is_active, jsonschema, status) FROM stdin;
3d337ec9-5a12-4636-b13e-fc63f710a0c9	index-api	\N	.	ROLE_JENV	c03a7420-3c72-4232-9f5d-185d270a70b8	83bf9e9d-5b50-458a-9fe8-3e001f1842ea	f	0	{}	1.0.0	t	\N	DRAFT
\.


--
-- Data for Name: connector; Type: TABLE DATA; Schema: public; Owner: iko-admin
--

COPY public.connector (id, name, tag, connector_code, version, is_active, status) FROM stdin;
259d9b73-c67c-4954-bd7b-9a8a537c8c08	index-api	index-api	- route:\n      id: 'direct:iko:connector:index-api'\n      errorHandler:\n          noErrorHandler: {}\n      from:\n          uri: 'direct:iko:connector:index-api'\n          steps:\n            # 2. Keep the interactionId for the status calls\n            - setProperty:\n                name: interactionId\n                simple: '${body[interactieId]}'\n            - setProperty:\n                name: attempts\n                constant: 0\n     \n            # 3. Poll /status/{interactionId} until status == OK (max 30 attempts)\n            - loop:\n                doWhile: true\n                simple: "${exchangeProperty.pollStatus} != 'OK' && ${exchangeProperty.attempts} < 30"\n                steps:\n                  - setProperty:\n                      name: attempts\n                      simple: '${exchangeProperty.attempts}++'\n                  - removeHeaders:\n                      pattern: 'CamelHttp*'\n                  - setHeader:\n                      name: CamelHttpMethod\n                      constant: GET\n                  - setHeader:\n                      name: Accept\n                      constant: application/json\n                  - setBody:\n                      constant: ''\n                  - toD:\n                      uri: '${variable.configProperties[host]}/status/${exchangeProperty.interactionId}'\n                  - unmarshal:\n                      json:\n                        library: Jackson\n                  - setProperty:\n                      name: pollStatus\n                      simple: '${body[status]}'\n                  - log:\n                      message: "Status poll ${exchangeProperty.attempts} for ${exchangeProperty.interactionId}: ${exchangeProperty.pollStatus}"\n                  - choice:\n                      when:\n                        - simple: "${exchangeProperty.pollStatus} != 'OK'"\n                          steps:\n                            - delay:\n                                constant: 500\n     \n            # 4. Fail if we gave up without reaching OK\n            - choice:\n                when:\n                  - simple: "${exchangeProperty.pollStatus} != 'OK'"\n                    steps:\n                      - throwException:\n                          exceptionType: java.lang.IllegalStateException\n                          message: "Status for ${exchangeProperty.interactionId} not OK after ${exchangeProperty.attempts} attempts"\n                otherwise:\n                  steps:\n                    - log:\n                        message: "Interaction ${exchangeProperty.interactionId} completed with status OK"\n- route:\n    id: 'direct:iko:endpoint:transform:index-api.events'\n    errorHandler: \n        noErrorHandler: {}\n    from:\n        uri: 'direct:iko:endpoint:transform:index-api.events'\n        steps:\n            - choice:\n                when:\n                    - simple: "${header.bsn} == null"\n                      steps:\n                          - setHeader:\n                                name: "bsn"\n                                jq:\n                                    expression: ".idParam // header(\\"bsn\\") // empty"\n                                    source: "variable:endpointTransformContext"\n            - removeHeaders:\n                pattern: 'CamelHttp*'\n            - setHeader:\n                name: CamelHttpMethod\n                constant: GET\n            - setHeader:\n                name: Accept\n                constant: application/json\n            - toD:\n                uri: ${variable.configProperties[host]}/events?bsn=${header.bsn}\n            - unmarshal:\n                json: {}\n            - log:\n                message: "HTTP ${header.CamelHttpResponseCode} - Response: ${body}"\n        	1.0.0	t	DRAFT
\.


--
-- Data for Name: connector_endpoint; Type: TABLE DATA; Schema: public; Owner: iko-admin
--

COPY public.connector_endpoint (id, name, connector_id, operation) FROM stdin;
83bf9e9d-5b50-458a-9fe8-3e001f1842ea	events	259d9b73-c67c-4954-bd7b-9a8a537c8c08	events
\.


--
-- Data for Name: connector_endpoint_role; Type: TABLE DATA; Schema: public; Owner: iko-admin
--

COPY public.connector_endpoint_role (id, connector_endpoint_id, connector_instance_id, role) FROM stdin;
5c6ca274-b310-4570-8a8e-ccda765679fe	83bf9e9d-5b50-458a-9fe8-3e001f1842ea	c03a7420-3c72-4232-9f5d-185d270a70b8	ROLE_ADMIN
\.


--
-- Data for Name: connector_instance; Type: TABLE DATA; Schema: public; Owner: iko-admin
--

COPY public.connector_instance (id, name, connector_id, tag, config, api_specification_url) FROM stdin;
c03a7420-3c72-4232-9f5d-185d270a70b8	index-api	259d9b73-c67c-4954-bd7b-9a8a537c8c08	index-api	\N	\N
\.


--
-- Data for Name: connector_instance_config; Type: TABLE DATA; Schema: public; Owner: iko-admin
--

COPY public.connector_instance_config (connector_instance_id, key, value) FROM stdin;
c03a7420-3c72-4232-9f5d-185d270a70b8	host	akjHrersZI4aw4RNfn8eEFE3A9WeQjird7FHONGT/SUxHGz+A6vUG7YpZ8EM+cs9DHVCZvz+LdyHFu5z
\.


--
-- Data for Name: endpoint; Type: TABLE DATA; Schema: public; Owner: iko-admin
--

COPY public.endpoint (id, name, route_id, created_on, modified_on, is_primary, is_active) FROM stdin;
a1b2c3d4-e5f6-7890-ab12-cdef34567890	BRP Personen	brpPersonenEndpoint	2026-10-07 09:22:15.694117	\N	t	t
b2c3d4e5-f678-9012-ab34-def456789012	OpenZaak Zaken	openZaakEndpointZaken	2026-10-07 09:22:15.694117	\N	t	t
c3d4e5f6-7890-1234-ab56-ef5678901234	BAG adressen	bagEndpointAdressen	2026-10-07 09:22:15.694117	\N	t	t
d4e5f678-9012-3456-ab78-f67890123456	BAG Adresseerbare Objecten	bagEndpointAdresseerbareObjecten	2026-10-07 09:22:15.694117	\N	f	t
e5f67890-1234-5678-ab90-678901234567	BAG Bronhouders	bagEndpointBronhouders	2026-10-07 09:22:15.694117	\N	f	t
f6789012-3456-7890-ab12-789012345678	BAG Ligplaatsen	bagEndpointLigplaatsen	2026-10-07 09:22:15.694117	\N	f	t
07890123-4567-8901-ab34-890123456789	BAG Nummeraanduidingen	bagEndpointNummeraanduidingen	2026-10-07 09:22:15.694117	\N	f	t
18901234-5678-9012-ab56-901234567890	BAG Openbare Ruimten	bagEndpointOpenbareRuimten	2026-10-07 09:22:15.694117	\N	f	t
29012345-6789-0123-ab78-012345678901	BAG Panden	bagEndpointPanden	2026-10-07 09:22:15.694117	\N	f	t
39012345-6789-0123-ab90-123456789012	BAG Standplaatsen	bagEndpointStandplaatsen	2026-10-07 09:22:15.694117	\N	f	t
49012345-6789-0123-ab12-234567890123	BAG Verblijfsobjecten	bagEndpointVerblijfsobjecten	2026-10-07 09:22:15.694117	\N	f	t
59012345-6789-0123-ab34-345678901234	BAG Woonplaatsen	bagEndpointWoonplaatsen	2026-10-07 09:22:15.694117	\N	f	t
69012345-6789-0123-ab56-456789012345	ObjectsAPI Objecten	objectenApiObjecten	2026-10-07 09:22:15.694117	\N	f	t
\.


--
-- Data for Name: flyway_schema_history; Type: TABLE DATA; Schema: public; Owner: iko-admin
--

COPY public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) FROM stdin;
1	2025.04.17.1	initial schema	SQL	V2025.04.17.1__initial_schema.sql	1276635901	iko-admin	2026-10-07 09:22:15.52912	15	t
2	2025.04.17.2	initial profile	SQL	V2025.04.17.2__initial_profile.sql	-882336977	iko-admin	2026-10-07 09:22:15.58189	7	t
3	2025.04.18.1	additional profiles	SQL	V2025.04.18.1__additional_profiles.sql	1507264689	iko-admin	2026-10-07 09:22:15.607511	3	t
4	2025.05.14.1	search table	SQL	V2025.05.14.1__search_table.sql	-838312438	iko-admin	2026-10-07 09:22:15.621937	5	t
5	2025.05.14.2	search table default	SQL	V2025.05.14.2__search_table_default.sql	1582902556	iko-admin	2026-10-07 09:22:15.638231	6	t
6	2025.06.18.1	additional column search table	SQL	V2025.06.18.1__additional_column_search_table.sql	-520987388	iko-admin	2026-10-07 09:22:15.65731	4	t
7	2025.07.08.1	rename tables	SQL	V2025.07.08.1__rename_tables.sql	-599478933	iko-admin	2026-10-07 09:22:15.670121	3	t
8	2025.07.24.1	rename tables	SQL	V2025.07.24.1__rename_tables.sql	1216572075	iko-admin	2026-10-07 09:22:15.679674	3	t
9	2025.07.25.1	reload endpoint table	SQL	V2025.07.25.1__reload_endpoint_table.sql	-63451777	iko-admin	2026-10-07 09:22:15.691026	4	t
10	2025.07.25.2	rename col	SQL	V2025.07.25.2__rename_col.sql	965998036	iko-admin	2026-10-07 09:22:15.704076	2	t
11	2025.07.25.3	set active endpoints	SQL	V2025.07.25.3__set_active_endpoints.sql	1057858888	iko-admin	2026-10-07 09:22:15.714352	4	t
12	2025.07.29.1	clear table	SQL	V2025.07.29.1__clear_table.sql	924856221	iko-admin	2026-10-07 09:22:15.729314	5	t
13	2025.07.31.1	add role	SQL	V2025.07.31.1__add_role.sql	-1824576640	iko-admin	2026-10-07 09:22:15.745237	3	t
14	2025.08.13.1	add connector tables	SQL	V2025.08.13.1__add_connector_tables.sql	182406903	iko-admin	2026-10-07 09:22:15.754695	7	t
15	2025.08.13.2	add connector tables	SQL	V2025.08.13.2__add_connector_tables.sql	-751095584	iko-admin	2026-10-07 09:22:15.771104	4	t
16	2025.08.19	add config table	SQL	V2025.08.19__add_config_table.sql	-1289390993	iko-admin	2026-10-07 09:22:15.782784	3	t
17	2025.08.19.1	add connector code	SQL	V2025.08.19.1__add_connector_code.sql	-1683320696	iko-admin	2026-10-07 09:22:15.79366	3	t
18	2025.08.26	add connector to profile	SQL	V2025.08.26__add_connector_to_profile.sql	850615933	iko-admin	2026-10-07 09:22:15.803616	9	t
19	2025.09.05	widen connector instance config value	SQL	V2025.09.05__widen_connector_instance_config_value.sql	-1533958628	iko-admin	2026-10-07 09:22:15.819935	3	t
20	2025.10.01.1	remove-columns	SQL	V2025.10.01.1__remove-columns.sql	127547554	iko-admin	2026-10-07 09:22:15.828889	3	t
21	2025.10.09.1	add relation property name	SQL	V2025.10.09.1__add_relation_property_name.sql	-2133655457	iko-admin	2026-10-07 09:22:15.838733	3	t
22	2025.11.19.1	add cache settings columns	SQL	V2025.11.19.1__add_cache_settings_columns.sql	1417511640	iko-admin	2026-10-07 09:22:15.848934	2	t
23	2025.11.26.1	add relation cache settings columns	SQL	V2025.11.26.1__add_relation_cache_settings_columns.sql	-2112356555	iko-admin	2026-10-07 09:22:15.859869	2	t
24	2025.12.02.1	add endpoint transform column	SQL	V2025.12.02.1__add_endpoint_transform_column.sql	-1487157793	iko-admin	2026-10-07 09:22:15.869544	3	t
25	2026.01.20.1	rename adp role to roles	SQL	V2026.01.20.1__rename_adp_role_to_roles.sql	912738141	iko-admin	2026-10-07 09:22:15.879664	2	t
26	2026.01.29.1	add versioning	SQL	V2026.01.29.1__add_versioning.sql	16324497	iko-admin	2026-10-07 09:22:15.890478	10	t
27	2026.02.20.1	add adp jsonschema	SQL	V2026.02.20.1__add_adp_jsonschema.sql	786261108	iko-admin	2026-10-07 09:22:15.909387	2	t
28	2026.03.12.1	add draft final status	SQL	V2026.03.12.1__add_draft_final_status.sql	1856533015	iko-admin	2026-10-07 09:22:15.918789	4	t
29	2026.03.16.1	add api specification url	SQL	V2026.03.16.1__add_api_specification_url.sql	-1781475077	iko-admin	2026-10-07 09:22:15.931197	2	t
30	2026.06.03.1	add logging event	SQL	V2026.06.03.1__add_logging_event.sql	-1423608844	iko-admin	2026-10-07 09:22:15.939166	8	t
\.


--
-- Data for Name: logging_event; Type: TABLE DATA; Schema: public; Owner: iko-admin
--

COPY public.logging_event (timestmp, formatted_message, logger_name, level_string, thread_name, reference_flag, arg0, arg1, arg2, arg3, caller_filename, caller_class, caller_method, caller_line, event_id) FROM stdin;
\.


--
-- Data for Name: logging_event_exception; Type: TABLE DATA; Schema: public; Owner: iko-admin
--

COPY public.logging_event_exception (event_id, i, trace_line) FROM stdin;
\.


--
-- Data for Name: logging_event_property; Type: TABLE DATA; Schema: public; Owner: iko-admin
--

COPY public.logging_event_property (event_id, mapped_key, mapped_value) FROM stdin;
\.


--
-- Data for Name: relation; Type: TABLE DATA; Schema: public; Owner: iko-admin
--

COPY public.relation (id, aggregated_data_profile_id, source_id, source_to_endpoint_mapping, endpoint_id, transform, connector_instance_id, connector_endpoint_id, property_name, cache_enabled, cache_ttl) FROM stdin;
\.


--
-- Name: logging_event_id_seq; Type: SEQUENCE SET; Schema: public; Owner: iko-admin
--

SELECT pg_catalog.setval('public.logging_event_id_seq', 1, false);


--
-- Name: aggregated_data_profile aggregated_data_profile_name_version_unique; Type: CONSTRAINT; Schema: public; Owner: iko-admin
--

ALTER TABLE ONLY public.aggregated_data_profile
    ADD CONSTRAINT aggregated_data_profile_name_version_unique UNIQUE (name, version);


--
-- Name: connector_endpoint connector_endpoint_pkey; Type: CONSTRAINT; Schema: public; Owner: iko-admin
--

ALTER TABLE ONLY public.connector_endpoint
    ADD CONSTRAINT connector_endpoint_pkey PRIMARY KEY (id);


--
-- Name: connector_endpoint_role connector_endpoint_role_pkey; Type: CONSTRAINT; Schema: public; Owner: iko-admin
--

ALTER TABLE ONLY public.connector_endpoint_role
    ADD CONSTRAINT connector_endpoint_role_pkey PRIMARY KEY (id);


--
-- Name: connector_instance connector_instance_pkey; Type: CONSTRAINT; Schema: public; Owner: iko-admin
--

ALTER TABLE ONLY public.connector_instance
    ADD CONSTRAINT connector_instance_pkey PRIMARY KEY (id);


--
-- Name: connector connector_pkey; Type: CONSTRAINT; Schema: public; Owner: iko-admin
--

ALTER TABLE ONLY public.connector
    ADD CONSTRAINT connector_pkey PRIMARY KEY (id);


--
-- Name: connector connector_tag_version_unique; Type: CONSTRAINT; Schema: public; Owner: iko-admin
--

ALTER TABLE ONLY public.connector
    ADD CONSTRAINT connector_tag_version_unique UNIQUE (tag, version);


--
-- Name: flyway_schema_history flyway_schema_history_pk; Type: CONSTRAINT; Schema: public; Owner: iko-admin
--

ALTER TABLE ONLY public.flyway_schema_history
    ADD CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank);


--
-- Name: logging_event pk_logging_event; Type: CONSTRAINT; Schema: public; Owner: iko-admin
--

ALTER TABLE ONLY public.logging_event
    ADD CONSTRAINT pk_logging_event PRIMARY KEY (event_id);


--
-- Name: logging_event_exception pk_logging_event_exception; Type: CONSTRAINT; Schema: public; Owner: iko-admin
--

ALTER TABLE ONLY public.logging_event_exception
    ADD CONSTRAINT pk_logging_event_exception PRIMARY KEY (event_id, i);


--
-- Name: logging_event_property pk_logging_event_property; Type: CONSTRAINT; Schema: public; Owner: iko-admin
--

ALTER TABLE ONLY public.logging_event_property
    ADD CONSTRAINT pk_logging_event_property PRIMARY KEY (event_id, mapped_key);


--
-- Name: aggregated_data_profile pk_profile; Type: CONSTRAINT; Schema: public; Owner: iko-admin
--

ALTER TABLE ONLY public.aggregated_data_profile
    ADD CONSTRAINT pk_profile PRIMARY KEY (id);


--
-- Name: relation pk_relation; Type: CONSTRAINT; Schema: public; Owner: iko-admin
--

ALTER TABLE ONLY public.relation
    ADD CONSTRAINT pk_relation PRIMARY KEY (id);


--
-- Name: endpoint search_pkey; Type: CONSTRAINT; Schema: public; Owner: iko-admin
--

ALTER TABLE ONLY public.endpoint
    ADD CONSTRAINT search_pkey PRIMARY KEY (id);


--
-- Name: flyway_schema_history_s_idx; Type: INDEX; Schema: public; Owner: iko-admin
--

CREATE INDEX flyway_schema_history_s_idx ON public.flyway_schema_history USING btree (success);


--
-- Name: idx_adp_name_active; Type: INDEX; Schema: public; Owner: iko-admin
--

CREATE UNIQUE INDEX idx_adp_name_active ON public.aggregated_data_profile USING btree (name) WHERE (is_active = true);


--
-- Name: idx_connector_tag_active; Type: INDEX; Schema: public; Owner: iko-admin
--

CREATE UNIQUE INDEX idx_connector_tag_active ON public.connector USING btree (tag) WHERE (is_active = true);


--
-- Name: idx_logging_event_level_string; Type: INDEX; Schema: public; Owner: iko-admin
--

CREATE INDEX idx_logging_event_level_string ON public.logging_event USING btree (level_string);


--
-- Name: idx_logging_event_timestmp; Type: INDEX; Schema: public; Owner: iko-admin
--

CREATE INDEX idx_logging_event_timestmp ON public.logging_event USING btree (timestmp);


--
-- Name: aggregated_data_profile aggregated_data_profile_connector_endpoint_id_fk; Type: FK CONSTRAINT; Schema: public; Owner: iko-admin
--

ALTER TABLE ONLY public.aggregated_data_profile
    ADD CONSTRAINT aggregated_data_profile_connector_endpoint_id_fk FOREIGN KEY (connector_endpoint_id) REFERENCES public.connector_endpoint(id);


--
-- Name: aggregated_data_profile aggregated_data_profile_connector_instance_id_fk; Type: FK CONSTRAINT; Schema: public; Owner: iko-admin
--

ALTER TABLE ONLY public.aggregated_data_profile
    ADD CONSTRAINT aggregated_data_profile_connector_instance_id_fk FOREIGN KEY (connector_instance_id) REFERENCES public.connector_instance(id);


--
-- Name: connector_endpoint connector_endpoint_connector_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: iko-admin
--

ALTER TABLE ONLY public.connector_endpoint
    ADD CONSTRAINT connector_endpoint_connector_id_fkey FOREIGN KEY (connector_id) REFERENCES public.connector(id) ON DELETE CASCADE;


--
-- Name: connector_endpoint_role connector_endpoint_role_connector_endpoint_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: iko-admin
--

ALTER TABLE ONLY public.connector_endpoint_role
    ADD CONSTRAINT connector_endpoint_role_connector_endpoint_id_fkey FOREIGN KEY (connector_endpoint_id) REFERENCES public.connector_endpoint(id) ON DELETE CASCADE;


--
-- Name: connector_endpoint_role connector_endpoint_role_connector_instance_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: iko-admin
--

ALTER TABLE ONLY public.connector_endpoint_role
    ADD CONSTRAINT connector_endpoint_role_connector_instance_id_fkey FOREIGN KEY (connector_instance_id) REFERENCES public.connector_instance(id) ON DELETE CASCADE;


--
-- Name: connector_instance_config connector_instance_config_connector_instance_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: iko-admin
--

ALTER TABLE ONLY public.connector_instance_config
    ADD CONSTRAINT connector_instance_config_connector_instance_id_fkey FOREIGN KEY (connector_instance_id) REFERENCES public.connector_instance(id) ON DELETE CASCADE;


--
-- Name: connector_instance connector_instance_connector_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: iko-admin
--

ALTER TABLE ONLY public.connector_instance
    ADD CONSTRAINT connector_instance_connector_id_fkey FOREIGN KEY (connector_id) REFERENCES public.connector(id) ON DELETE CASCADE;


--
-- Name: logging_event_exception fk_logging_event_exception_event; Type: FK CONSTRAINT; Schema: public; Owner: iko-admin
--

ALTER TABLE ONLY public.logging_event_exception
    ADD CONSTRAINT fk_logging_event_exception_event FOREIGN KEY (event_id) REFERENCES public.logging_event(event_id);


--
-- Name: logging_event_property fk_logging_event_property_event; Type: FK CONSTRAINT; Schema: public; Owner: iko-admin
--

ALTER TABLE ONLY public.logging_event_property
    ADD CONSTRAINT fk_logging_event_property_event FOREIGN KEY (event_id) REFERENCES public.logging_event(event_id);


--
-- Name: relation fk_relation_on_profile; Type: FK CONSTRAINT; Schema: public; Owner: iko-admin
--

ALTER TABLE ONLY public.relation
    ADD CONSTRAINT fk_relation_on_profile FOREIGN KEY (aggregated_data_profile_id) REFERENCES public.aggregated_data_profile(id);


--
-- Name: relation relation_connector_endpoint_id_fk; Type: FK CONSTRAINT; Schema: public; Owner: iko-admin
--

ALTER TABLE ONLY public.relation
    ADD CONSTRAINT relation_connector_endpoint_id_fk FOREIGN KEY (connector_endpoint_id) REFERENCES public.connector_endpoint(id);


--
-- Name: relation relation_connector_instance_id_fk; Type: FK CONSTRAINT; Schema: public; Owner: iko-admin
--

ALTER TABLE ONLY public.relation
    ADD CONSTRAINT relation_connector_instance_id_fk FOREIGN KEY (connector_instance_id) REFERENCES public.connector_instance(id);


--
-- PostgreSQL database dump complete
--

\unrestrict RnMdfscsStEQafSOoWEOLVJ40pD0ESkf7r3UtXPKB9BcbsqL66spTDXfKk1877n

