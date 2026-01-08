FROM mcr.microsoft.com/devcontainers/java:21

# Install base packages
RUN apt-get update && export DEBIAN_FRONTEND=noninteractive \
    && apt-get -y install --no-install-recommends \
        build-essential \
        curl \
        git \
        htop \
        jq \
        nano \
        netcat-openbsd \
        postgresql-client \
        python3 \
        python3-pip \
        redis-tools \
        tree \
        unzip \
        vim \
        wget \
    && apt-get autoremove -y && apt-get clean -y \
    && rm -rf /var/lib/apt/lists/*

# Install Node.js
RUN curl -fsSL https://deb.nodesource.com/setup_20.x | bash - \
    && apt-get install -y nodejs \
    && rm -rf /var/lib/apt/lists/*

# Install Docker CLI
RUN curl -fsSL https://get.docker.com | sh

# Install Docker Compose
RUN curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose \
    && chmod +x /usr/local/bin/docker-compose

# Install Kafka CLI tools (fetch latest version dynamically with IPv4 enforced retries)
ARG SCALA_VERSION=2.13
ARG KAFKA_DEFAULT_VERSION=4.1.1
ARG KAFKA_VERSION
RUN set -eux; \
    API_URL="https://api.github.com/repos/apache/kafka/releases/latest"; \
    if [ -z "${KAFKA_VERSION:-}" ]; then \
        API_RESPONSE=$(curl -4 -H "Accept: application/vnd.github+json" -H "User-Agent: devcontainer" -fsSL --retry 5 --retry-delay 10 --connect-timeout 20 "$API_URL" || true); \
        if [ -n "$API_RESPONSE" ]; then \
            PARSED_VERSION=$(printf '%s' "$API_RESPONSE" | jq -r 'select(.tag_name != null) | .tag_name' 2>/dev/null || true); \
            if [ -n "$PARSED_VERSION" ] && [ "$PARSED_VERSION" != "null" ]; then \
                KAFKA_VERSION=$(printf '%s' "$PARSED_VERSION" | sed 's/^v//'); \
            fi; \
        fi; \
    fi; \
    if [ -z "${KAFKA_VERSION:-}" ]; then \
        KAFKA_VERSION=$(curl -4 -fsSL --retry 5 --retry-delay 10 --connect-timeout 20 https://downloads.apache.org/kafka/ | grep -oP '(?<=href=")[0-9]+\.[0-9]+\.[0-9]+(?=/")' | sort -V | tail -1 || true); \
    fi; \
    if [ -z "${KAFKA_VERSION:-}" ]; then \
        KAFKA_VERSION="$KAFKA_DEFAULT_VERSION"; \
    fi; \
    ARCHIVE="kafka_${SCALA_VERSION}-${KAFKA_VERSION}.tgz"; \
    PRIMARY_URL="https://downloads.apache.org/kafka/${KAFKA_VERSION}/${ARCHIVE}"; \
    FALLBACK_URL="https://dlcdn.apache.org/kafka/${KAFKA_VERSION}/${ARCHIVE}"; \
    curl -4 -fSL --retry 5 --retry-delay 10 --connect-timeout 20 -o kafka.tgz "$PRIMARY_URL" \
    || curl -4 -fSL --retry 5 --retry-delay 10 --connect-timeout 20 -o kafka.tgz "$FALLBACK_URL"; \
    tar -xzf kafka.tgz; \
    mv "kafka_${SCALA_VERSION}-${KAFKA_VERSION}" /opt/kafka; \
    rm kafka.tgz; 

# Install useful development tools
RUN npm install -g @angular/cli @vue/cli create-react-app

# Install Maven (latest stable version)
RUN MAVEN_VERSION="3.9.12" \
    && curl -4 -fSL --retry 5 --retry-delay 10 --connect-timeout 20 -o "apache-maven-${MAVEN_VERSION}-bin.tar.gz" "https://dlcdn.apache.org/maven/maven-3/3.9.12/binaries/apache-maven-3.9.12-bin.tar.gz" \
    && tar -xzf "apache-maven-${MAVEN_VERSION}-bin.tar.gz" \
    && mv "apache-maven-${MAVEN_VERSION}" /opt/maven \
    && rm "apache-maven-${MAVEN_VERSION}-bin.tar.gz" \
    && ln -s /opt/maven/bin/mvn /usr/local/bin/mvn

# Set environment variables - using correct Java path
ENV MAVEN_HOME=/opt/maven
ENV PATH=$PATH:$MAVEN_HOME/bin
ENV JAVA_HOME=/usr/lib/jvm/msopenjdk-current
ENV KAFKA_HOME=/opt/kafka

# Create workspace directory (align with devcontainer.json workspaceFolder)
WORKDIR /workspaces/java-skishop-microservices

# Set up git safe directory, configure Maven settings, and create useful aliases
RUN git config --global --add safe.directory /workspaces/java-skishop-microservices \
    && mkdir -p /root/.m2 \
    && echo '<?xml version="1.0" encoding="UTF-8"?>' > /root/.m2/settings.xml \
    && echo '<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"' >> /root/.m2/settings.xml \
    && echo '          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"' >> /root/.m2/settings.xml \
    && echo '          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0' >> /root/.m2/settings.xml \
    && echo '                              http://maven.apache.org/xsd/settings-1.0.0.xsd">' >> /root/.m2/settings.xml \
    && echo '  <localRepository>/root/.m2/repository</localRepository>' >> /root/.m2/settings.xml \
    && echo '  <interactiveMode>false</interactiveMode>' >> /root/.m2/settings.xml \
    && echo '  <offline>false</offline>' >> /root/.m2/settings.xml \
    && echo '</settings>' >> /root/.m2/settings.xml

# Create useful aliases
RUN echo 'alias ll="ls -la"' >> /root/.bashrc \
    && echo 'alias la="ls -la"' >> /root/.bashrc \
    && echo 'alias ..="cd .."' >> /root/.bashrc \
    && echo 'alias grep="grep --color=auto"' >> /root/.bashrc \
    && echo 'alias mvn-clean="mvn clean compile"' >> /root/.bashrc \
    && echo 'alias mvn-test="mvn test"' >> /root/.bashrc \
    && echo 'alias mvn-package="mvn clean package -DskipTests"' >> /root/.bashrc \
    && echo 'alias dc="docker-compose"' >> /root/.bashrc \
    && echo 'alias dcu="docker-compose up -d"' >> /root/.bashrc \
    && echo 'alias dcd="docker-compose down"' >> /root/.bashrc \
    && echo 'alias kafka-topics="/opt/kafka/bin/kafka-topics.sh"'  >> /root/.bashrc \
    && echo 'alias dcl="docker-compose logs -f"' >> /root/.bashrc

# Keep container running
CMD ["sleep", "infinity"]
