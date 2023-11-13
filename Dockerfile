FROM openjdk:17-ea-11-jdk-slim
VOLUME /tmp
COPY build/libs/dummy-0.0.1-SNAPSHOT.jar DummyDataInserter.jar
ENTRYPOINT ["java", "-jar", "DummyDataInserter.jar"]