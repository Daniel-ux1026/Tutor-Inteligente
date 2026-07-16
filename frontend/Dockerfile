FROM node:20-alpine AS build

WORKDIR /app
RUN corepack enable

COPY frontend/package.json frontend/pnpm-lock.yaml frontend/pnpm-workspace.yaml ./
RUN pnpm install --frozen-lockfile

COPY frontend/ ./

ARG VITE_API_URL
ARG VITE_DEMO_MODE=false
ENV VITE_API_URL=${VITE_API_URL}
ENV VITE_DEMO_MODE=${VITE_DEMO_MODE}

RUN pnpm build

FROM nginxinc/nginx-unprivileged:1.31-alpine

COPY frontend/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 8080
