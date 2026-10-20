# Build en dos etapas. El contexto de build es la RAIZ del proyecto (ver docker-compose.yml)
# porque primero hay que instalar deposito-commons en el repositorio Maven local (.m2)
# del contenedor de build (mvn install), igual que en la maquina de desarrollo, y recien
# despues compilar el microservicio, que lo usa como una dependencia mas de su pom.xml.

# ---- Etapa 1: compilar ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY deposito-commons/pom.xml deposito-commons/pom.xml
COPY deposito-commons/src deposito-commons/src
COPY ms-catalogo/pom.xml ms-catalogo/pom.xml
COPY ms-catalogo/src ms-catalogo/src
# (los tests se corren aparte con "mvn verify"; aca solo se empaqueta)
RUN --mount=type=cache,target=/root/.m2     mvn -q -B -f deposito-commons/pom.xml install -DskipTests &&     mvn -q -B -f ms-catalogo/pom.xml package -DskipTests

# ---- Etapa 2: imagen liviana solo con el JRE y el jar ----
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/ms-catalogo/target/ms-catalogo-1.0.0.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
