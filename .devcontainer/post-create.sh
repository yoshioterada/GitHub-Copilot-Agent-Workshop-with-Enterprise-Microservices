#!/usr/bin/env bash
set -euo pipefail

echo "🚀 Setting up Ski Shop Microservices development environment (post-create) ..."

export SHELL=/bin/bash
export PATH=$JAVA_HOME/bin:$PATH
export DOCKER_API_VERSION=1.43

# Resolve workspace folder
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
WS_DIR="${WORKSPACE_FOLDER:-/workspaces/GitHub-Copilot-Agent-Workshop-with-Enterprise-Microservices}"
COMPOSE_FILE="${COMPOSE_FILE:-${SCRIPT_DIR}/docker-compose.yml}"

LOG_DIR="${WS_DIR}/logs"

# Append helpful aliases once (idempotent)
if ! grep -q "Ski Shop Development Aliases" ~/.bashrc 2>/dev/null; then
cat >> ~/.bashrc <<'EOF'

# Ski Shop Development Aliases
alias ll="ls -la"
alias la="ls -la"
alias ..="cd .."
alias build="./build.sh"
alias services="./start-services.sh"
alias stop-services="./stop-services.sh"
alias logs="tail -f logs/*.log"

# Maven shortcuts
alias mvn-clean="mvn clean compile"
alias mvn-test="mvn test"
alias mvn-package="mvn clean package -DskipTests"
alias mvn-run="mvn spring-boot:run"

# Git shortcuts
alias gs="git status"
alias ga="git add"
alias gc="git commit"
alias gp="git push"
alias gl="git log --oneline"
alias kafka-topics="/opt/kafka/bin/kafka-topics.sh"

echo "🚀 Ski Shop Development Environment Ready!"
EOF
fi

mkdir -p "${LOG_DIR}"
chmod +x "${WS_DIR}"/*.sh 2>/dev/null || true

echo "🔍 Verifying installation..."
echo "Java version:"; java --version || true
echo "Maven version:"; mvn --version || true

echo "✅ Base shell customization done. Evaluating optional steps..."

CHECK_INFRA_SERVICES=${CHECK_INFRA_SERVICES:-true}
MAVEN_GO_OFFLINE=${MAVEN_GO_OFFLINE:-once}
START_INFRA_SERVICES=${START_INFRA_SERVICES:-true}
COMPOSE_SERVICES="${COMPOSE_SERVICES:-postgres redis kafka elasticsearch}"

echo "⚙️  Flags -> CHECK_INFRA_SERVICES=${CHECK_INFRA_SERVICES} | MAVEN_GO_OFFLINE=${MAVEN_GO_OFFLINE}"

resolve_compose_cmd() {
    if command -v docker compose >/dev/null 2>&1; then echo "docker compose"; return 0; fi
    if command -v docker >/dev/null 2>&1 && docker compose version >/dev/null 2>&1; then echo "docker compose"; return 0; fi
    return 1
}

resolve_host_or_localhost() {
    local host="$1"
    if getent hosts "$host" >/dev/null 2>&1; then
        printf '%s' "$host"
    else
        printf 'localhost'
    fi
}

ensure_infra_services() {
    local cmd_str
    cmd_str="$(resolve_compose_cmd)" || { echo "⚠️  docker compose/ docker compose が見つかりません。インフラ自動起動をスキップします。"; return 1; }
    local -a cmd
    read -r -a cmd <<<"${cmd_str}"

    local -a services
    read -r -a services <<<"${COMPOSE_SERVICES}"

    if [ ! -f "${COMPOSE_FILE}" ]; then
        echo "⚠️  COMPOSE_FILE=${COMPOSE_FILE} が存在しません。インフラ自動起動をスキップします。"
        return 1
    fi

    if ! docker info >/dev/null 2>&1; then
        echo "⚠️  Docker デーモンに接続できません。インフラ自動起動をスキップします。"
        return 1
    fi

    echo "🔧 インフラサービスを起動します: ${services[*]}"
    "${cmd[@]}" -f "${COMPOSE_FILE}" up -d "${services[@]}"
}

run_infra_checks() {
    echo "⏳ Waiting for infrastructure services to start..."
    sleep 5
    echo "🔍 Checking service health..."

    local PGHOST="${PGHOST:-${POSTGRES_HOST:-postgres}}"
    local PGPORT="${PGPORT:-5432}"
    local PGUSER="${PGUSER:-${POSTGRES_USER:-postgres}}"
    local PGDATABASE="${PGDATABASE:-${POSTGRES_DB:-skishop}}"
    local REDIS_HOST="${REDIS_HOST:-redis}"
    local REDIS_PORT="${REDIS_PORT:-6379}"
    local REDIS_PASSWORD="${REDIS_PASSWORD:-}"
    local KAFKA_BOOTSTRAP_SERVERS="${KAFKA_BOOTSTRAP_SERVERS:-kafka:9092}"
    local KAFKA_HOSTPORT
    KAFKA_HOSTPORT="$(printf '%s' "$KAFKA_BOOTSTRAP_SERVERS" | cut -d, -f1)"
    local ES_HOST="${ES_HOST:-elasticsearch}"
    local ES_PORT="${ES_PORT:-9200}"
        # Codespaces や Dev Container では devcontainer が compose ネットワーク外にいる場合があるため、名前解決できなければ localhost にフォールバック
        PGHOST="$(resolve_host_or_localhost "$PGHOST")"
        REDIS_HOST="$(resolve_host_or_localhost "$REDIS_HOST")"
        ES_HOST="$(resolve_host_or_localhost "$ES_HOST")"
        # kafka host:port -> fallback host only
        local KAFKA_HOST="${KAFKA_HOSTPORT%%:*}"
        local KAFKA_PORT="${KAFKA_HOSTPORT##*:}"
        KAFKA_HOST="$(resolve_host_or_localhost "$KAFKA_HOST")"
        KAFKA_HOSTPORT="${KAFKA_HOST}:${KAFKA_PORT}"

    if command -v pg_isready &> /dev/null; then
        echo "Checking PostgreSQL connection..."
        for i in {1..30}; do
            if pg_isready -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$PGDATABASE" > /dev/null 2>&1; then
                echo "✅ PostgreSQL is ready"; break; fi
            echo "Waiting for PostgreSQL... ($i/30)"; sleep 2; done
    else
        echo "ℹ️  pg_isready not found, skipping PostgreSQL check"
    fi

    if command -v redis-cli &> /dev/null; then
        echo "Checking Redis connection..."
        for i in {1..20}; do
            if redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" ping > /dev/null 2>&1; then
                echo "✅ Redis is ready"; break; fi
            if [ -n "$REDIS_PASSWORD" ] && redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" -a "$REDIS_PASSWORD" ping > /dev/null 2>&1; then
                echo "✅ Redis is ready (auth)"; break; fi
            echo "Waiting for Redis... ($i/20)"; sleep 2; done
    else
        echo "ℹ️  redis-cli not found, skipping Redis check"
    fi

    if command -v "/opt/kafka/bin/kafka-topics.sh" &> /dev/null; then
        echo "Checking Kafka connection..."
        for i in {1..30}; do
            if "/opt/kafka/bin/kafka-topics.sh" --bootstrap-server localhost:29092 --list > /dev/null 2>&1; then
                echo "✅ Kafka is reachable"; break; fi
            echo "Waiting for Kafka... ($i/30)"; sleep 3; done
    else
        echo "ℹ️  kafka-topics not found, skipping Kafka connectivity check"
    fi

    echo "Checking Elasticsearch connection..."
    for i in {1..20}; do
        if curl -fsS "http://${ES_HOST}:${ES_PORT}/_cluster/health" > /dev/null 2>&1; then
            echo "✅ Elasticsearch healthy"; break; fi
        echo "Waiting for Elasticsearch... ($i/20)"; sleep 3; done
}

maybe_create_kafka_topics() {
    if command -v "/opt/kafka/bin/kafka-topics.sh" &> /dev/null; then
        echo "📢 Setting up Kafka topics (idempotent)..."
        local topics=(user-events order-events inventory-events payment-events notification-events)
        for t in "${topics[@]}"; do
            "/opt/kafka/bin/kafka-topics.sh" --bootstrap-server localhost:29092 --create --if-not-exists --topic "$t" --partitions 3 --replication-factor 1 || true
        done
        echo "📋 Available Kafka topics:"; "/opt/kafka/bin/kafka-topics.sh" --bootstrap-server localhost:29092 --list || true
    else
        echo "ℹ️  kafka-topics CLI 未インストールのためトピック作成スキップ"
    fi
}

maybe_go_offline() {
    case "${MAVEN_GO_OFFLINE}" in
        false|off|no)
            echo "⏭  Skipping mvn dependency:go-offline (flag=${MAVEN_GO_OFFLINE})";;
        once)
            local marker="${WS_DIR}/.mvn_go_offline_done"
            if [ -f "$marker" ]; then
                echo "⏭  Skipping go-offline (already done once). Remove $marker to force rerun."; return 0; fi
            printf "📦 Running mvn dependency:go-offline (once)...\nNow downloading many dependency libraries...(Please wait, It will take few minutes to finish.)\n"
            mvn -q dependency:go-offline || echo "⚠️  go-offline encountered issues"
            touch "$marker";;
        always|true|yes)
            echo "📦 Running mvn dependency:go-offline (always)..."; mvn -q dependency:go-offline || echo "⚠️  go-offline encountered issues";;
        *)
            echo "ℹ️  Unknown MAVEN_GO_OFFLINE='${MAVEN_GO_OFFLINE}', treating as 'once'"; MAVEN_GO_OFFLINE=once; maybe_go_offline; return;;
    esac
}

if [ "${CHECK_INFRA_SERVICES}" = "true" ] || [ "${CHECK_INFRA_SERVICES}" = "1" ]; then
    if [ "${START_INFRA_SERVICES}" = "true" ] || [ "${START_INFRA_SERVICES}" = "1" ]; then
        ensure_infra_services || echo "⚠️  インフラ起動に失敗またはスキップしました。ヘルスチェックのみ実行します。"
    else
        echo "⏭  インフラ自動起動スキップ (START_INFRA_SERVICES=${START_INFRA_SERVICES})"
    fi
    run_infra_checks
    maybe_create_kafka_topics
else
    echo "⏭  Infra health checks skipped (CHECK_INFRA_SERVICES=${CHECK_INFRA_SERVICES})"
fi

maybe_go_offline

# Set up database schemas if init scripts are available
echo "🗄️ Setting up database schemas..."
if [ -f "${WS_DIR}/scripts/init-databases.sql" ]; then
    echo "Database initialization script found, schemas should be created automatically"
else
    echo "No database initialization script found, you may need to create schemas manually"
fi

# Display useful information
echo ""
echo "🎉 Development environment setup complete!"
echo ""
echo "📍 Available Services:"
echo "  - PostgreSQL:     localhost:5432 (user: skishop_user, pass: skishop_password)"
echo "  - Redis:          localhost:6379 (pass: redis_password)"
echo "  - Kafka:          localhost:9092"
echo "  - Elasticsearch:  localhost:9200"
echo "  - Prometheus:     localhost:9090"
echo "  - Grafana:        localhost:3001 (admin/admin)"
echo "  - MailHog:        localhost:8025"
echo ""
echo "🔧 Useful Commands:"
echo "  mvn clean compile                    # Compile all modules"
echo "  mvn clean package -DskipTests       # Package all modules"
echo "  mvn spring-boot:run -pl <module>    # Run specific service"
echo "  docker-compose logs -f <service>    # View service logs"
echo "  kafka-topics --bootstrap-server kafka:9092 --list  # List Kafka topics"
echo ""
echo "🚀 Ready for development! (post-create complete)"
