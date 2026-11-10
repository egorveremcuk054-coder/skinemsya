# Сборка
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Сначала только зависимости: этот слой кэшируется, пока не меняется pom.xml
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src src
RUN ./mvnw -B -q package -DskipTests

# Запуск
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN useradd --system --uid 1001 app
COPY --from=build /app/target/*.jar app.jar
USER app

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
