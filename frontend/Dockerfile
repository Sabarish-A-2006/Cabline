FROM node:22-alpine AS build
WORKDIR /web
ARG VITE_GOOGLE_OAUTH_ENABLED=false
ENV VITE_GOOGLE_OAUTH_ENABLED=$VITE_GOOGLE_OAUTH_ENABLED
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /web/dist /usr/share/nginx/html
EXPOSE 80
