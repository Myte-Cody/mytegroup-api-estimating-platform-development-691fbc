# Build stage
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm install
RUN npm i -g @nestjs/cli
COPY . .
RUN nest build

# Prod stage
FROM node:20-alpine
WORKDIR /app
RUN apk add --no-cache curl redis
COPY --from=build /app/dist ./dist
COPY package*.json ./
RUN npm install --only=production

# Entrypoint to start local Redis (when REDIS_URL points to localhost), API, and worker
COPY docker/entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh
EXPOSE 80

# Add labels for project identification
LABEL org.opencontainers.image.project_id="mytegroup-api-estimating-platform-development-691fbc"
LABEL org.opencontainers.image.organization="" 

CMD ["/entrypoint.sh"]
