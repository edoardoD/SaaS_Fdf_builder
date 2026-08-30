FROM gradle:8.10.2-jdk17 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle :service-kotlin:distTar --no-daemon

FROM openjdk:17-slim
EXPOSE 8080
RUN mkdir /app
COPY --from=build /home/gradle/src/service-kotlin/build/distributions/service-kotlin-0.0.1.tar /app/
WORKDIR /app
RUN tar -xvf service-kotlin-0.0.1.tar && rm service-kotlin-0.0.1.tar
ENTRYPOINT ["/app/service-kotlin-0.0.1/bin/service-kotlin"]
