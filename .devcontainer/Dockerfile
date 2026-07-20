# Dev container for building the Hermes Android app in GitHub Codespaces.
# Installs JDK 17 + the Android SDK (cmdline-tools, build-tools 34, platform 34)
# so `./gradlew assembleDebug` works out of the box.

FROM mcr.microsoft.com/devcontainers/base:ubuntu-22.04

ENV DEBIAN_FRONTEND=noninteractive \
    ANDROID_HOME=/opt/android-sdk \
    JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64

# --- JDK 17 ---
RUN apt-get update && apt-get install -y --no-install-recommends \
        openjdk-17-jdk \
        unzip \
        curl \
        git \
        ca-certificates \
    && rm -rf /var/lib/apt/lists/*

# --- Android command-line tools ---
ARG ANDROID_CMDTOOLS=11076708
RUN mkdir -p ${ANDROID_HOME}/cmdline-tools \
    && curl -fsSL "https://dl.google.com/android/repository/commandlinetools-linux-${ANDROID_CMDTOOLS}_latest.zip" -o /tmp/cmdtools.zip \
    && unzip -q /tmp/cmdtools.zip -d ${ANDROID_HOME}/cmdline-tools \
    && mv ${ANDROID_HOME}/cmdline-tools/cmdline-tools ${ANDROID_HOME}/cmdline-tools/latest \
    && rm /tmp/cmdtools.zip

ENV PATH="${ANDROID_HOME}/cmdline-tools/latest/bin:${PATH}"

# Accept licenses + install build tools / platform
RUN yes | sdkmanager --licenses || true \
    && sdkmanager "build-tools;34.0.0" "platforms;android-34" "platform-tools"

# make gradle use the SDK
ENV ANDROID_SDK_ROOT=${ANDROID_HOME}

# --- Gradle + wrapper (so ./gradlew works in Codespaces) ---
ARG GRADLE_VERSION=8.9
RUN curl -fsSL "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" -o /tmp/gradle.zip \
    && unzip -q /tmp/gradle.zip -d /opt \
    && rm /tmp/gradle.zip \
    && ln -s /opt/gradle-${GRADLE_VERSION}/bin/gradle /usr/local/bin/gradle

# Generate the gradle wrapper so the repo is buildable with ./gradlew
WORKDIR /workspace
COPY . /workspace
RUN gradle wrapper --gradle-version ${GRADLE_VERSION} --no-daemon
