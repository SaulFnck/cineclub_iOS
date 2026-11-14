# syntax=docker/dockerfile:1

# ============================================================
# Etapa 1: compilar el WAR con Maven + Java 17
# ============================================================
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /build

# Copiamos primero el POM para que Docker cachee las dependencias
COPY cineclub_api/pom.xml ./pom.xml
RUN mvn -B -DskipTests dependency:go-offline

# Código fuente y recursos (incluye src/main/webapp)
COPY cineclub_api/src ./src

# Genera target/cineclub_api-1.0-SNAPSHOT.war
RUN mvn -B -DskipTests package

# ============================================================
# Etapa 2: ejecutar sobre Tomcat 10.1 (Jakarta EE 10 / Servlet 6.0)
# ============================================================
FROM tomcat:10.1-jdk17-temurin

# Quitamos las aplicaciones de ejemplo de Tomcat
RUN rm -rf /usr/local/tomcat/webapps/*

# El nombre del WAR define el contexto público: /cineclub_api
COPY --from=build /build/target/cineclub_api-1.0-SNAPSHOT.war \
     /usr/local/tomcat/webapps/cineclub_api.war

# Tomcat 10.1 escucha en 8080. En Render configura PORT=8080
# (ver render.yaml) para que Render escanee el puerto correcto.
ENV PORT=8080
EXPOSE 8080

CMD ["catalina.sh", "run"]
