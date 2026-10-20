# ---- Etapa 1: compilar la app React con Vite ----
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
# URL de Keycloak que usa el NAVEGADOR (no la interna de Docker)
ARG VITE_KEYCLOAK_URL=http://localhost:9090
ENV VITE_KEYCLOAK_URL=$VITE_KEYCLOAK_URL
RUN npm run build

# ---- Etapa 2: servir los archivos estaticos con nginx ----
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
