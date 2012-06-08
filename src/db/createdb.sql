--
-- PostgreSQL database dump
--

SET statement_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;

--
-- Name: scm2pgsql; Type: COMMENT; Schema: -; Owner: postgres
--

COMMENT ON DATABASE scm2pgsql IS 'This database will house the SCM information of users, changes etc.';


--
-- Name: plpgsql; Type: EXTENSION; Schema: -; Owner: 
--

CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;

--
-- Name: EXTENSION plpgsql; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';

SET search_path = public, pg_catalog;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: commits; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE commits (
    id integer NOT NULL,
    commit_id character varying(255),
    author character varying(255),
    author_email character varying(255),
    comments text,
    commit_date timestamp with time zone,
    branch_id character varying(255)
);

--
-- Name: commits_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE commits_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: commits_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE commits_id_seq OWNED BY commits.id;


--
-- Name: files; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE files (
    id integer NOT NULL,
    file_id character varying(255),
    file_name character varying(255),
    commit_id character varying(255),
    raw_file text
);


--
-- Name: files_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE files_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: files_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE files_id_seq OWNED BY files.id;


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY commits ALTER COLUMN id SET DEFAULT nextval('commits_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY files ALTER COLUMN id SET DEFAULT nextval('files_id_seq'::regclass);


--
-- Name: public; Type: ACL; Schema: -; Owner: postgres
--

REVOKE ALL ON SCHEMA public FROM PUBLIC;
GRANT ALL ON SCHEMA public TO PUBLIC;

CREATE TABLE branches (
    branch_id character varying (255),
    branch_name character varying(100),
    commit_id character varying(255)
);

CREATE TABLE changes (
	commit_id character varying (255),
	file_id character varying (255),
	change_type character varying(10)
);

CREATE TABLE source_trees (
	commit_id character varying(255),
	file_id character varying(255)
);

CREATE TABLE IF NOT EXISTS file_diffs (
	file_id character varying(255) NOT NULL,
	new_commit_id character varying(255) NOT NULL,
	old_commit_id character varying(255) NOT NULL,
	diff_text text,
	char_start integer NOT NULL,
	char_end integer NOT NULL,
	diff_type character varying (30)
);

CREATE TABLE networks (
	new_commit_id varchar(255),
	old_commit_id varchar(255),
	network_id integer NOT NULL PRIMARY KEY
);

CREATE SEQUENCE networks_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE networks_id_seq OWNED BY networks.network_id;

ALTER TABLE ONLY networks ALTER COLUMN network_id SET DEFAULT nextval('networks_id_seq'::regclass);

CREATE TABLE nodes (
	id varchar(255),
	label varchar(255),
	network_id integer references networks(network_id) on delete cascade,
	PRIMARY KEY(id)
);

CREATE TABLE edges (
	source varchar(255),
	target varchar(255),
	weight real,
	is_fuzzy boolean,
	network_id integer references networks(network_id) on delete cascade
);

CREATE TABLE IF NOT EXISTS owners (
	commit_id character varying(255) NOT NULL,
	source_commit_id character varying(255) NOT NULL,
	owner_id character varying(255) NOT NULL,
	file_id character varying(255) NOT NULL,
	char_start integer NOT NULL,
	char_end integer NOT NULL,
	change_type varchar(12)
);

CREATE TABLE IF NOT EXISTS commit_family (
	parent character varying(255) NOT NULL,
	child character varying(255) NOT NULL
);	
