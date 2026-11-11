# 빌드 단계
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true
COPY src src
RUN ./gradlew --no-daemon bootJar -x test

# 실행 단계 (JRE만)
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --no-create-home pliview
COPY --from=build /app/build/libs/*.jar app.jar
USER pliview
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=mysql
ENTRYPOINT ["java", "-jar", "app.jar"]
