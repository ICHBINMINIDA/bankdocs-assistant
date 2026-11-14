From maven : 3.9-eclipse-temurin-21 as build
workdir /app
copy pom.xml .
run mvn -q dependency:go-offline
copy src ./src
RUN mvn -q package -DskipTests


fROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*:jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
