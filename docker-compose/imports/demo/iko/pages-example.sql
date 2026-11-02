-- Mock ADPs for the config pages example (nl-portal-issues#143). Runs after init-db.sql on a fresh iko-database.
--
-- One generic connector 'pages-mock' with operation 'get' calls WireMock (:8083, same encrypted host as index-api) at
-- /pages-mock/<path>?id=<id>. Each ADP's endpoint_transform turns IKO's context (idParam, sortParams, filterParams)
-- into headers; for a paged container the route filters by status and slices the array itself, because IKO does not
-- page. Paged results come back as {totaal, content}.

INSERT INTO public.connector (id, name, tag, connector_code, version, is_active, status) VALUES (
    '7a0e4d52-0b1f-4c39-9a51-6f2b8e4c1a01', 'pages-mock', 'pages-mock', $code$
- route:
    id: 'direct:iko:connector:pages-mock'
    errorHandler:
      noErrorHandler: {}
    from:
      uri: 'direct:iko:connector:pages-mock'
      steps:
        - log:
            message: "pages-mock ${header.path} done"
- route:
    id: 'direct:iko:endpoint:transform:pages-mock.get'
    errorHandler:
      noErrorHandler: {}
    from:
      uri: 'direct:iko:endpoint:transform:pages-mock.get'
      steps:
        - removeHeaders:
            pattern: 'CamelHttp*'
        - setHeader:
            name: CamelHttpMethod
            constant: GET
        - setHeader:
            name: Accept
            constant: application/json
        - setBody:
            constant: ''
        - toD:
            uri: '${variable.configProperties[host]}/pages-mock/${header.path}?id=${header.id}'
        - unmarshal:
            json: {}
        - choice:
            when:
              - simple: "${header.size} != null && ${header.size} != ''"
                steps:
                  - setBody:
                      jq:
                        expression: >-
                          [.[] | select(header("status") == "" or .status == header("status"))]
                          | {totaal: length,
                             content: .[((header("page") | tonumber) * (header("size") | tonumber)):][:(header("size") | tonumber)]}
        - log:
            message: "pages-mock ${header.path} id=${header.id} page=${header.page} size=${header.size} status=${header.status}"
$code$, '1.0.0', true, 'DRAFT');

INSERT INTO public.connector_endpoint (id, name, connector_id, operation) VALUES
    ('7a0e4d52-0b1f-4c39-9a51-6f2b8e4c1a02', 'get', '7a0e4d52-0b1f-4c39-9a51-6f2b8e4c1a01', 'get');

INSERT INTO public.connector_instance (id, name, connector_id, tag, config, api_specification_url) VALUES
    ('7a0e4d52-0b1f-4c39-9a51-6f2b8e4c1a03', 'pages-mock', '7a0e4d52-0b1f-4c39-9a51-6f2b8e4c1a01', 'pages-mock', NULL, NULL);

-- http://host.docker.internal:8083, encrypted with IKO_CRYPTO_KEY from docker-compose.yml (same value as index-api)
INSERT INTO public.connector_instance_config (connector_instance_id, key, value) VALUES
    ('7a0e4d52-0b1f-4c39-9a51-6f2b8e4c1a03', 'host', 'akjHrersZI4aw4RNfn8eEFE3A9WeQjird7FHONGT/SUxHGz+A6vUG7YpZ8EM+cs9DHVCZvz+LdyHFu5z');

INSERT INTO public.aggregated_data_profile
    (id, name, primary_endpoint, transform, roles, connector_instance_id, connector_endpoint_id, endpoint_transform, version, is_active, status)
VALUES
    ('7a0e4d52-0b1f-4c39-9a51-6f2b8e4c1a11', 'personen', NULL, '.', 'ROLE_JENV',
     '7a0e4d52-0b1f-4c39-9a51-6f2b8e4c1a03', '7a0e4d52-0b1f-4c39-9a51-6f2b8e4c1a02',
     '{path: "personen", id: .idParam}', '1.0.0', true, 'DRAFT'),
    ('7a0e4d52-0b1f-4c39-9a51-6f2b8e4c1a12', 'vergunningen', NULL, '{vergunningen: .}', 'ROLE_JENV',
     '7a0e4d52-0b1f-4c39-9a51-6f2b8e4c1a03', '7a0e4d52-0b1f-4c39-9a51-6f2b8e4c1a02',
     '{path: "vergunningen", id: .idParam, page: (.sortParams.vergunningen.pageNumber // 0), size: (.sortParams.vergunningen.pageSize // ""), status: (.filterParams.vergunningen.status // "")}',
     '1.0.0', true, 'DRAFT'),
    ('7a0e4d52-0b1f-4c39-9a51-6f2b8e4c1a13', 'bedrijf', NULL, '.', 'ROLE_JENV',
     '7a0e4d52-0b1f-4c39-9a51-6f2b8e4c1a03', '7a0e4d52-0b1f-4c39-9a51-6f2b8e4c1a02',
     '{path: "bedrijf", id: .idParam}', '1.0.0', true, 'DRAFT'),
    -- Public profile: the id is a postcode typed by the user (spaces removed, upper-cased), not a BSN/KVK.
    ('7a0e4d52-0b1f-4c39-9a51-6f2b8e4c1a14', 'parkeerplekken', NULL, '{plekken: .}', 'ROLE_JENV',
     '7a0e4d52-0b1f-4c39-9a51-6f2b8e4c1a03', '7a0e4d52-0b1f-4c39-9a51-6f2b8e4c1a02',
     '{path: "parkeerplekken", id: (.idParam | split(" ") | join("") | ascii_upcase), page: (.sortParams.plekken.pageNumber // 0), size: (.sortParams.plekken.pageSize // ""), status: ""}',
     '1.0.0', true, 'DRAFT');
