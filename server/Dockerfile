# Multi-stage theo docs/kien-truc/07-chiu-loi-chi-phi.md 7.5
FROM maven:3-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/target/refound-server.jar app.jar
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xss512k -XX:MaxMetaspaceSize=128m -XX:ReservedCodeCacheSize=48m"
EXPOSE 8080
USER 1000
ENTRYPOINT ["java", "-jar", "app.jar"]
