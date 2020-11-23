FROM maven:3-jdk-11 as build
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline

ADD . /build/src 

WORKDIR /build/src
RUN mvn package


FROM openliberty/open-liberty:kernel-slim-java11-openj9-ubi
COPY --chown=1001:0  src/main/liberty/config/server.xml /config/
RUN sed -e '/webApplication/g' -i /config/server.xml
RUN features.sh
COPY --chown=1001:0 --from=build  /build/src/target/asr_lid-client.war /config/dropins/
RUN configure.sh
