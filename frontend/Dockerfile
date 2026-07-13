# syntax=docker/dockerfile:1
# Build from the frontend/ pnpm workspace root (the app consumes @nl-portal/* via workspace:*).
#   Local (compose) — compile from source:
#     docker build -t nl-portal-frontend frontend/
#   CI/release — reuse the build-once output (packages/app/build/ already produced):
#     docker build --build-arg BUILD_MODE=ci -t nl-portal-frontend frontend/
# BuildKit (default since Docker 23.0) skips the unused build stage entirely.
ARG BUILD_MODE=local

# --- local: compile the app together with its workspace lib deps (topological order) ---
FROM node:24-alpine AS build-local
WORKDIR /workspace
COPY . .
ENV CI=true
RUN npm i -g pnpm@11 && pnpm i && pnpm --filter "nl-portal-app..." build

# --- ci: reuse the pre-built app output from the context (build-once); no compile ---
FROM alpine AS build-ci
WORKDIR /workspace
COPY packages/app/build ./packages/app/build

# --- select the build source via BUILD_MODE; BuildKit prunes the other stage ---
FROM build-${BUILD_MODE} AS build

FROM nginxinc/nginx-unprivileged:stable-alpine AS image

# Remove default config that listens on 8080
RUN rm /etc/nginx/conf.d/default.conf

# Static files (built app), owned by nginx user 101
COPY --chown=101:101 --from=build /workspace/packages/app/build /usr/share/nginx/html

# nginx config template + entrypoint (static source, mode-independent — copied from context)
COPY --chown=101:101 packages/app/conf/nginx.conf /etc/nginx/templates/nginx.conf.template
COPY --chown=101:101 --chmod=+x packages/app/entrypoint.sh /docker-entrypoint.d/40-envsubst-config.sh

EXPOSE 8081

LABEL org.opencontainers.image.description="NL Portal Frontend."

CMD ["nginx", "-g", "daemon off;"]
