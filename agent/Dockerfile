# Build the standalone agent (plain Java) and run it on a slim JRE.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

COPY pom.xml ./
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q -Dmaven.test.skip=true package dependency:copy-dependencies \
    -DincludeScope=runtime -DoutputDirectory=target/lib

FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

RUN useradd --system --uid 10001 --create-home sentinel

COPY --from=build /build/target/classes ./classes
COPY --from=build /build/target/lib ./lib

# Create the identity/token directory in the image so Docker seeds the
# agent-data volume with sentinel ownership on first run; otherwise the
# non-root agent cannot write its identity file.
RUN mkdir -p /home/sentinel/.sentinel-agent \
    && chown -R sentinel:sentinel /app /home/sentinel

USER sentinel

# The agent persists its identity and device token here. Mount a volume to keep
# them across restarts, otherwise the device re-enrols with a new identity.
ENV SENTINEL_API_URL=http://api:8080
ENV SENTINEL_INTERVAL_SECONDS=30

ENTRYPOINT ["java", "-cp", "classes:lib/*", "com.remi481.sentinel.agent.AgentMain"]
