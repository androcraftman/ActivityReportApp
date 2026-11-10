# Official OpenJDK 21 base image on Ubuntu
FROM --platform=linux/amd64 eclipse-temurin:21-jdk-jammy

# Set non-interactive mode for apt installs
ENV DEBIAN_FRONTEND=noninteractive

# Install essential terminal dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    unzip \
    zip \
    git \
    wget \
    && rm -rf /var/lib/apt/lists/*

# Set up Android SDK paths and environment variables
ENV ANDROID_HOME=/opt/android-sdk
ENV PATH=${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools:${ANDROID_HOME}/emulator:${PATH}

# Download and install Android command line tools (modern stable release 11076708)
RUN mkdir -p ${ANDROID_HOME}/cmdline-tools && \
    curl -o /tmp/cmdline-tools.zip https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip && \
    unzip /tmp/cmdline-tools.zip -d ${ANDROID_HOME}/cmdline-tools && \
    mv ${ANDROID_HOME}/cmdline-tools/cmdline-tools ${ANDROID_HOME}/cmdline-tools/latest && \
    rm /tmp/cmdline-tools.zip

# Programmatically accept all Android SDK licenses
RUN yes | sdkmanager --licenses

# Install platform tools and Android 36 SDK
RUN sdkmanager "platform-tools" "platforms;android-36" "build-tools;36.0.0"

# Create a build directory
WORKDIR /app
