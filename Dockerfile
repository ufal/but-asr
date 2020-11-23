FROM maven:3-jdk-11 as build

ADD . /src 

WORKDIR /src
RUN mvn package


FROM openliberty/open-liberty:kernel-slim-java11-openj9-ubi
COPY --chown=1001:0  src/main/liberty/config/server.xml /config/
RUN sed -e '/webApplication/g' -i /config/server.xml
RUN features.sh
COPY --chown=1001:0 --from=build  /src/target/asr_lid-client.war /config/dropins/
RUN configure.sh
