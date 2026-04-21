#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

if [[ -f "$ROOT_DIR/.env" ]]; then
    set -a
    # shellcheck disable=SC1091
    source "$ROOT_DIR/.env"
    set +a
fi

command="${1:-help}"
RUNTIME_DIR="$ROOT_DIR/.run"
DASHBOARD_UI_PID_FILE="$RUNTIME_DIR/dashboard-ui.pid"
DASHBOARD_UI_LOG_FILE="$RUNTIME_DIR/dashboard-ui.log"
DASHBOARD_UI_BUILD_LOG_FILE="$RUNTIME_DIR/dashboard-ui-build.log"
DASHBOARD_UI_JAR="$ROOT_DIR/dashboard-service/target/dashboard-service-1.0-SNAPSHOT.jar"
DASHBOARD_UI_SCREEN_SESSION="fraud-dashboard-ui"
CLICKHOUSE_HTTP_PORT="${CLICKHOUSE_HTTP_PORT:-8123}"

is_running() {
    local pid="$1"
    kill -0 "$pid" >/dev/null 2>&1
}

port_8084_pid() {
    lsof -tiTCP:8084 -sTCP:LISTEN 2>/dev/null || true
}

screen_session_running() {
    if ! command -v screen >/dev/null 2>&1; then
        return 1
    fi

    local sessions
    sessions="$(screen -ls 2>/dev/null || true)"
    grep -F "$DASHBOARD_UI_SCREEN_SESSION" <<< "$sessions" >/dev/null 2>&1
}

wait_for_http() {
    local url="$1"
    local name="$2"
    local attempts="${3:-60}"

    for _ in $(seq 1 "$attempts"); do
        if curl -fsS "$url" >/dev/null 2>&1; then
            return 0
        fi
        sleep 1
    done

    echo "$name is not ready at $url"
    return 1
}

ensure_dashboard_ui_infra() {
    echo "Starting ClickHouse for Dashboard UI..."
    docker compose up -d clickhouse

    if ! wait_for_http "http://localhost:${CLICKHOUSE_HTTP_PORT}/ping" "ClickHouse"; then
        echo "Dashboard UI needs ClickHouse. Check logs with: docker compose logs clickhouse"
        return 1
    fi
}

wait_for_dashboard_ui() {
    local pid="${1:-}"

    for _ in {1..45}; do
        if [[ -n "$pid" ]] && ! is_running "$pid"; then
            echo "Dashboard UI process exited during startup"
            echo "Logs: $DASHBOARD_UI_LOG_FILE"
            tail -80 "$DASHBOARD_UI_LOG_FILE"
            return 1
        fi

        if curl -fsS "http://localhost:8084/actuator/health" >/dev/null 2>&1; then
            echo "Dashboard UI is ready: http://localhost:8084"
            return 0
        fi

        sleep 1
    done

    echo "Dashboard UI is still starting. Check logs: $DASHBOARD_UI_LOG_FILE"
}

start_dashboard_ui_process() {
    : > "$DASHBOARD_UI_LOG_FILE"

    if command -v screen >/dev/null 2>&1; then
        screen -dmS "$DASHBOARD_UI_SCREEN_SESSION" bash -lc "cd $(printf '%q' "$ROOT_DIR") && exec java -jar $(printf '%q' "$DASHBOARD_UI_JAR") --spring.kafka.listener.auto-startup=false > $(printf '%q' "$DASHBOARD_UI_LOG_FILE") 2>&1"
        echo "Starting Dashboard UI on http://localhost:8084 (screen=$DASHBOARD_UI_SCREEN_SESSION)"
        return 0
    fi

    nohup java -jar "$DASHBOARD_UI_JAR" \
        --spring.kafka.listener.auto-startup=false \
        > "$DASHBOARD_UI_LOG_FILE" 2>&1 < /dev/null &

    local pid="$!"
    echo "$pid" > "$DASHBOARD_UI_PID_FILE"
    disown "$pid" >/dev/null 2>&1 || true
    echo "Starting Dashboard UI on http://localhost:8084 (pid=$pid)"
    return 0
}

start_dashboard_ui() {
    mkdir -p "$RUNTIME_DIR"

    if [[ -f "$DASHBOARD_UI_PID_FILE" ]]; then
        local existing_pid
        existing_pid="$(cat "$DASHBOARD_UI_PID_FILE")"
        if [[ -n "$existing_pid" ]] && is_running "$existing_pid"; then
            echo "Dashboard UI is already running on http://localhost:8084 (pid=$existing_pid)"
            return 0
        fi
        rm -f "$DASHBOARD_UI_PID_FILE"
    fi

    local port_pid
    port_pid="$(port_8084_pid)"
    if [[ -n "$port_pid" ]]; then
        echo "Port 8084 is already used by pid=$port_pid"
        echo "Stop it first with: ./run.sh dashboard-ui-stop"
        return 1
    fi

    if screen_session_running; then
        screen -S "$DASHBOARD_UI_SCREEN_SESSION" -X quit >/dev/null 2>&1 || true
        sleep 1
    fi

    ensure_dashboard_ui_infra

    echo "Packaging dashboard-service..."
    if ! mvn -pl dashboard-service -am -DskipTests package > "$DASHBOARD_UI_BUILD_LOG_FILE" 2>&1; then
        echo "Failed to package dashboard-service"
        echo "Build log: $DASHBOARD_UI_BUILD_LOG_FILE"
        tail -80 "$DASHBOARD_UI_BUILD_LOG_FILE"
        return 1
    fi

    echo "Logs: $DASHBOARD_UI_LOG_FILE"
    start_dashboard_ui_process

    local pid=""
    if [[ -f "$DASHBOARD_UI_PID_FILE" ]]; then
        pid="$(cat "$DASHBOARD_UI_PID_FILE")"
    fi
    wait_for_dashboard_ui "$pid"

    local running_pid
    running_pid="$(port_8084_pid)"
    if [[ -n "$running_pid" ]]; then
        echo "$running_pid" > "$DASHBOARD_UI_PID_FILE"
    fi
}

stop_dashboard_ui() {
    local pid=""

    if [[ -f "$DASHBOARD_UI_PID_FILE" ]]; then
        pid="$(cat "$DASHBOARD_UI_PID_FILE")"
    fi

    if [[ -z "$pid" ]] || ! is_running "$pid"; then
        pid="$(port_8084_pid)"
    fi

    if [[ -z "$pid" ]]; then
        if screen_session_running; then
            echo "Stopping Dashboard UI screen session"
            screen -S "$DASHBOARD_UI_SCREEN_SESSION" -X quit >/dev/null 2>&1 || true
            rm -f "$DASHBOARD_UI_PID_FILE"
            return 0
        fi

        echo "Dashboard UI is not running"
        rm -f "$DASHBOARD_UI_PID_FILE"
        return 0
    fi

    echo "Stopping Dashboard UI (pid=$pid)"
    kill "$pid" >/dev/null 2>&1 || true

    for _ in {1..20}; do
        if ! is_running "$pid"; then
            if screen_session_running; then
                screen -S "$DASHBOARD_UI_SCREEN_SESSION" -X quit >/dev/null 2>&1 || true
            fi
            rm -f "$DASHBOARD_UI_PID_FILE"
            echo "Dashboard UI stopped"
            return 0
        fi
        sleep 0.5
    done

    echo "Dashboard UI did not stop gracefully, forcing shutdown"
    kill -9 "$pid" >/dev/null 2>&1 || true
    if screen_session_running; then
        screen -S "$DASHBOARD_UI_SCREEN_SESSION" -X quit >/dev/null 2>&1 || true
    fi
    rm -f "$DASHBOARD_UI_PID_FILE"
}

dashboard_ui_status() {
    local pid=""

    if [[ -f "$DASHBOARD_UI_PID_FILE" ]]; then
        pid="$(cat "$DASHBOARD_UI_PID_FILE")"
    fi

    if [[ -n "$pid" ]] && is_running "$pid"; then
        echo "Dashboard UI is running on http://localhost:8084 (pid=$pid)"
        return 0
    fi
    rm -f "$DASHBOARD_UI_PID_FILE"

    pid="$(port_8084_pid)"
    if [[ -n "$pid" ]]; then
        if screen_session_running; then
            echo "Dashboard UI is running on http://localhost:8084 (pid=$pid)"
            return 0
        fi
        echo "Something is listening on http://localhost:8084 (pid=$pid)"
        return 0
    fi

    if screen_session_running; then
        echo "Dashboard UI screen session exists, but port 8084 is not listening. Check logs: $DASHBOARD_UI_LOG_FILE"
        return 1
    fi

    echo "Dashboard UI is not running"
}

print_help() {
    cat <<'USAGE'
Usage:
  ./run.sh infra-up          Start Kafka, Kafka UI, PostgreSQL and ClickHouse
  ./run.sh infra-down        Stop local infrastructure
  ./run.sh infra-reset       Stop local infrastructure and remove Docker volumes
  ./run.sh infra-ps          Show infrastructure status
  ./run.sh infra-logs        Follow infrastructure logs
  ./run.sh compile           Compile all modules without tests

  ./run.sh payment           Run payment-service
  ./run.sh fraud             Run fraud-analyzer-service
  ./run.sh notification      Run notification-service
  ./run.sh dashboard         Run dashboard-service with Kafka listeners
  ./run.sh dashboard-ui      Run dashboard-service without Kafka listeners
  ./run.sh dashboard-ui-start
                            Package and start dashboard UI preview in the background
  ./run.sh dashboard-ui-stop
                            Stop dashboard UI preview
  ./run.sh dashboard-ui-status
                            Show dashboard UI preview status
  ./run.sh dashboard-ui-logs
                            Follow dashboard UI preview logs
  ./run.sh demo-transactions
                            Send 190 demo payment requests with verified and fraud cases
USAGE
}

case "$command" in
    infra-up)
        docker compose up -d
        ;;
    infra-down)
        docker compose down
        ;;
    infra-reset)
        docker compose down -v
        ;;
    infra-ps)
        docker compose ps
        ;;
    infra-logs)
        docker compose logs -f
        ;;
    compile)
        mvn -DskipTests compile
        ;;
    payment)
        mvn -pl payment-service spring-boot:run
        ;;
    fraud)
        mvn -pl fraud-analyzer-service spring-boot:run
        ;;
    notification)
        mvn -pl notification-service spring-boot:run
        ;;
    dashboard)
        mvn -pl dashboard-service spring-boot:run
        ;;
    dashboard-ui)
        ensure_dashboard_ui_infra
        mvn -pl dashboard-service spring-boot:run -Dspring-boot.run.arguments="--spring.kafka.listener.auto-startup=false"
        ;;
    dashboard-ui-start)
        start_dashboard_ui
        ;;
    dashboard-ui-stop)
        stop_dashboard_ui
        ;;
    dashboard-ui-status)
        dashboard_ui_status
        ;;
    dashboard-ui-logs)
        mkdir -p "$RUNTIME_DIR"
        touch "$DASHBOARD_UI_LOG_FILE"
        tail -f "$DASHBOARD_UI_LOG_FILE"
        ;;
    demo-transactions)
        "$ROOT_DIR/scripts/send-demo-transactions.sh"
        ;;
    help|--help|-h)
        print_help
        ;;
    *)
        echo "Unknown command: $command" >&2
        echo >&2
        print_help >&2
        exit 1
        ;;
esac
