# Stage 1: Build
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
ARG BUILD_TIMESTAMP
RUN echo "Building at: $BUILD_TIMESTAMP"
COPY gradle/ gradle/
COPY gradlew build.gradle settings.gradle ./
RUN ./gradlew dependencies --no-daemon || true
COPY src/ src/
RUN ./gradlew clean bootJar --no-daemon -x test

# Stage 2: Runtime
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
