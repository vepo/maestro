FROM eclipse-temurin:25-jre
COPY maestro-operator/target/maestro-operator-*.jar /app/operator.jar
ENTRYPOINT ["java", "-jar", "/app/operator.jar"]
