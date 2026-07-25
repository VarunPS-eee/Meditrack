# =============================================================================
#  MediTrack — multi-stage Docker build
#
#  Stage 1 (build)   : full JDK, compiles sources, runs the test suite, builds a jar.
#  Stage 2 (runtime) : JRE only. No compiler, no sources, no test classes.
#
#  The split matters: the JDK image is ~450 MB, the JRE ~180 MB. Everything the
#  build needs but the app does not is discarded at the stage boundary, which
#  shrinks the shipped image and removes the compiler from the attack surface.
#
#  The project has no third-party dependencies (Core Java only), so this build
#  works fully offline once the base images are pulled.
# =============================================================================

# -----------------------------------------------------------------------------
# Stage 1 — build and test
# -----------------------------------------------------------------------------
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /build

# Copy sources only. .dockerignore keeps out/, data/ and .git out of the context.
COPY src ./src

# Compile every source file, then run the manual test suite. If TestRunner exits
# non-zero the image build fails here — tests gate the build rather than merely
# reporting afterwards.
RUN set -eux; \
    find src/main/java -name '*.java' > sources.txt; \
    mkdir -p out; \
    javac -encoding UTF-8 -d out @sources.txt; \
    java -Dfile.encoding=UTF-8 -cp out com.airtribe.meditrack.test.TestRunner

# Package into an executable jar with the entry point recorded in the manifest.
RUN jar --create \
        --file meditrack.jar \
        --main-class com.airtribe.meditrack.Main \
        -C out .

# -----------------------------------------------------------------------------
# Stage 2 — runtime
# -----------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS runtime

LABEL org.opencontainers.image.title="MediTrack" \
      org.opencontainers.image.description="Clinic & Appointment Management System (Core Java)" \
      org.opencontainers.image.version="1.0.0" \
      org.opencontainers.image.vendor="Airtribe Java Track" \
      org.opencontainers.image.authors="Varun P S, Zubair, Sunil Kumar B A" \
      org.opencontainers.image.source="https://github.com/VarunPS-eee/Meditrack"

# UTF-8 everywhere: the console prints the rupee symbol and box-drawing characters,
# which render as '?' under the default POSIX locale.
ENV LANG=C.UTF-8 \
    LC_ALL=C.UTF-8 \
    JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8" \
    MEDITRACK_DATA_DIR=/data

# Run as a non-root user. A console app has no reason to hold root in a container.
RUN addgroup -S meditrack && \
    adduser -S -G meditrack -h /app meditrack && \
    mkdir -p /data && \
    chown -R meditrack:meditrack /data /app

WORKDIR /app

COPY --from=build --chown=meditrack:meditrack /build/meditrack.jar ./meditrack.jar

USER meditrack

# Persisted CSV / serialized state. Mount a host directory here to survive
# container restarts; see docker-compose.yml.
VOLUME ["/data"]

# The app is an interactive console menu, so the container needs stdin attached:
#   docker run -it meditrack --seedDemo
ENTRYPOINT ["java", "-jar", "/app/meditrack.jar"]

# Default arguments — overridden by anything passed to `docker run`.
CMD ["--seedDemo"]
