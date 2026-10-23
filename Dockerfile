# ---------- Etapa 1: build ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Copiamos primero el wrapper y el pom para aprovechar la caché de capas
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline

# Ahora el código fuente y el empaquetado
COPY src src
RUN ./mvnw -B clean package -DskipTests

# ---------- Etapa 2: runtime ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Usuario sin privilegios
RUN useradd --system --uid 1001 spring
COPY --from=build /app/target/*.jar app.jar
USER spring

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
