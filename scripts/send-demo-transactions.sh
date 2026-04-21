#!/usr/bin/env bash
set -euo pipefail

PAYMENT_BASE_URL="${PAYMENT_BASE_URL:-http://localhost:8081}"
PAYMENT_ENDPOINT="${PAYMENT_BASE_URL}/api/transactions"
REQUEST_DELAY_SECONDS="${REQUEST_DELAY_SECONDS:-0.03}"
PROCESSING_WAIT_SECONDS="${PROCESSING_WAIT_SECONDS:-18}"
DRY_RUN="${DRY_RUN:-false}"
STOP_ON_ERROR="${STOP_ON_ERROR:-true}"
PAYMENT_SCHEMA_CHECK="${PAYMENT_SCHEMA_CHECK:-true}"

sent=0
failed=0

countries=(US DE FR GB ES NL CA AU SE IT PL BR JP SG AE)
currencies=(USD EUR RUB)
categories=(GROCERY FUEL RESTAURANT ELECTRONICS TRAVEL GAMING PHARMACY CLOTHING BOOKS COFFEE)

new_country_accounts=()
new_country_source_countries=()
high_new_country_accounts=()
high_new_country_source_countries=()
frequency_accounts=()
high_frequency_accounts=()
frequency_new_country_accounts=()
frequency_new_country_source_countries=()

require_command() {
    local command_name="$1"

    if ! command -v "$command_name" >/dev/null 2>&1; then
        echo "Required command is missing: $command_name" >&2
        exit 1
    fi
}

new_uuid() {
    uuidgen | tr '[:upper:]' '[:lower:]'
}

country_for_index() {
    local index="$1"
    echo "${countries[$((index % ${#countries[@]}))]}"
}

different_country_for_index() {
    local index="$1"
    echo "${countries[$(((index + 5) % ${#countries[@]}))]}"
}

currency_for_index() {
    local index="$1"
    echo "${currencies[$((index % ${#currencies[@]}))]}"
}

category_for_index() {
    local index="$1"
    echo "${categories[$((index % ${#categories[@]}))]}"
}

is_true() {
    [[ "$1" == "true" || "$1" == "1" || "$1" == "yes" ]]
}

print_header() {
    local utc_hour_raw
    local utc_hour
    utc_hour_raw="$(date -u +%H)"
    utc_hour=$((10#$utc_hour_raw))

    cat <<EOF
Sending demo traffic to: $PAYMENT_ENDPOINT

Plan:
  - 190 transaction create requests
  - verified baseline transactions
  - HIGH_AMOUNT fraud
  - NEW_COUNTRY fraud
  - HIGH_FREQUENCY fraud
  - combinations: HIGH_AMOUNT+NEW_COUNTRY, HIGH_AMOUNT+HIGH_FREQUENCY, HIGH_FREQUENCY+NEW_COUNTRY
  - NIGHT_TIME candidates

NightTimeRule note:
  The rule uses event timestamp in UTC and checks hour 0..5 with amount > 3000.
  Current UTC hour is: $utc_hour
  NIGHT_TIME candidates are fraud only if this script is run during UTC 00:00..05:59.

EOF
}

wait_for_payment_service() {
    if is_true "$DRY_RUN"; then
        return 0
    fi

    echo "Waiting for payment-service at ${PAYMENT_BASE_URL}/actuator/health ..."

    for _ in {1..45}; do
        if curl -fsS "${PAYMENT_BASE_URL}/actuator/health" >/dev/null 2>&1; then
            echo "payment-service is ready"
            return 0
        fi
        sleep 1
    done

    echo "payment-service is not available. Start it with: ./run.sh payment" >&2
    exit 1
}

validate_payment_schema_if_possible() {
    if is_true "$DRY_RUN" || ! is_true "$PAYMENT_SCHEMA_CHECK"; then
        return 0
    fi

    if ! command -v docker >/dev/null 2>&1; then
        return 0
    fi

    if ! docker inspect fds-postgres-payment >/dev/null 2>&1; then
        return 0
    fi

    local schema_ready
    schema_ready="$(docker exec fds-postgres-payment psql \
        -U "${POSTGRES_USER:-postgres}" \
        -d payment_db \
        -Atc "select to_regclass('public.transactions') is not null and to_regclass('public.outbox_events') is not null;" 2>/dev/null || true)"

    if [[ "$schema_ready" != "t" ]]; then
        cat >&2 <<'EOF'
payment_db does not contain required payment-service tables:
  - transactions
  - outbox_events

Most likely payment-service was started before Docker infrastructure was reset.
Restart payment-service so Flyway can run migrations against the current database.

Typical local fix:
  1. Stop the old payment-service process or IntelliJ run configuration.
  2. Start it again: ./run.sh payment
  3. Run this script again: ./run.sh demo-transactions

EOF
        exit 1
    fi
}

wait_for_processing() {
    local reason="$1"

    echo
    echo "Waiting ${PROCESSING_WAIT_SECONDS}s for async processing: $reason"
    sleep "$PROCESSING_WAIT_SECONDS"
    echo
}

post_transaction() {
    local scenario="$1"
    local expected="$2"
    local account_id="$3"
    local amount="$4"
    local currency="$5"
    local country="$6"
    local merchant_category="$7"
    local request_number=$((sent + 1))
    local payload

    payload=$(cat <<EOF
{"accountId":"$account_id","amount":$amount,"currency":"$currency","country":"$country","merchantCategory":"$merchant_category"}
EOF
)

    if is_true "$DRY_RUN"; then
        printf '[%03d] DRY RUN %-28s expected=%-42s account=%s amount=%s %s country=%s category=%s\n' \
            "$request_number" "$scenario" "$expected" "$account_id" "$amount" "$currency" "$country" "$merchant_category"
        sent=$((sent + 1))
        return 0
    fi

    local response_file
    local http_code
    response_file="$(mktemp)"
    http_code="$(curl -sS -o "$response_file" -w "%{http_code}" \
        -X POST "$PAYMENT_ENDPOINT" \
        -H "Content-Type: application/json" \
        --data "$payload" || true)"

    if [[ "$http_code" =~ ^2[0-9][0-9]$ ]]; then
        printf '[%03d] HTTP %s %-28s expected=%-42s account=%s amount=%s %s country=%s category=%s\n' \
            "$request_number" "$http_code" "$scenario" "$expected" "$account_id" "$amount" "$currency" "$country" "$merchant_category"
    else
        failed=$((failed + 1))
        printf '[%03d] HTTP %s FAILED %-22s expected=%s account=%s\n' \
            "$request_number" "$http_code" "$scenario" "$expected" "$account_id" >&2
        cat "$response_file" >&2
        echo >&2

        if is_true "$STOP_ON_ERROR"; then
            rm -f "$response_file"
            exit 1
        fi
    fi

    rm -f "$response_file"
    sent=$((sent + 1))
    sleep "$REQUEST_DELAY_SECONDS"
}

send_verified_baseline() {
    echo "Phase 1. Verified baseline and direct fraud scenarios"

    for i in {1..40}; do
        post_transaction \
            "verified-baseline" \
            "VERIFIED" \
            "$(new_uuid)" \
            "$((80 + i)).50" \
            "$(currency_for_index "$i")" \
            "$(country_for_index "$i")" \
            "$(category_for_index "$i")"
    done
}

send_high_amount() {
    for i in {1..20}; do
        post_transaction \
            "high-amount" \
            "HIGH_AMOUNT" \
            "$(new_uuid)" \
            "$((11000 + (i * 137))).00" \
            "$(currency_for_index "$i")" \
            "$(country_for_index "$((i + 2))")" \
            "$(category_for_index "$((i + 3))")"
    done
}

send_country_seed_transactions() {
    local account_id
    local country

    for i in {1..15}; do
        account_id="$(new_uuid)"
        country="$(country_for_index "$i")"
        new_country_accounts+=("$account_id")
        new_country_source_countries+=("$country")

        post_transaction \
            "new-country-seed" \
            "VERIFIED seed for NEW_COUNTRY" \
            "$account_id" \
            "$((120 + i)).00" \
            "$(currency_for_index "$i")" \
            "$country" \
            "$(category_for_index "$i")"
    done

    for i in {1..5}; do
        account_id="$(new_uuid)"
        country="$(country_for_index "$((i + 7))")"
        high_new_country_accounts+=("$account_id")
        high_new_country_source_countries+=("$country")

        post_transaction \
            "high-new-seed" \
            "VERIFIED seed for HIGH_AMOUNT+NEW_COUNTRY" \
            "$account_id" \
            "$((180 + i)).00" \
            "$(currency_for_index "$i")" \
            "$country" \
            "$(category_for_index "$((i + 4))")"
    done
}

send_frequency_warmups() {
    local account_id
    local country

    echo "Phase 2. Frequency warmups"

    for i in {1..8}; do
        account_id="$(new_uuid)"
        frequency_accounts+=("$account_id")
        country="$(country_for_index "$((i + 1))")"

        for n in {1..3}; do
            post_transaction \
                "frequency-warmup" \
                "warms HIGH_FREQUENCY" \
                "$account_id" \
                "$((90 + n)).00" \
                "$(currency_for_index "$i")" \
                "$country" \
                "$(category_for_index "$((i + n))")"
        done
    done

    for i in {1..5}; do
        account_id="$(new_uuid)"
        high_frequency_accounts+=("$account_id")
        country="$(country_for_index "$((i + 3))")"

        for n in {1..3}; do
            post_transaction \
                "high-frequency-warmup" \
                "warms HIGH_AMOUNT+HIGH_FREQUENCY" \
                "$account_id" \
                "$((110 + n)).00" \
                "$(currency_for_index "$i")" \
                "$country" \
                "$(category_for_index "$((i + n + 1))")"
        done
    done

    for i in {1..5}; do
        account_id="$(new_uuid)"
        country="$(country_for_index "$((i + 6))")"
        frequency_new_country_accounts+=("$account_id")
        frequency_new_country_source_countries+=("$country")

        for n in {1..3}; do
            post_transaction \
                "freq-new-warmup" \
                "warms HIGH_FREQUENCY and country state" \
                "$account_id" \
                "$((130 + n)).00" \
                "$(currency_for_index "$i")" \
                "$country" \
                "$(category_for_index "$((i + n + 2))")"
        done
    done
}

send_new_country_frauds() {
    echo "Phase 3. State-dependent fraud scenarios"

    local length="${#new_country_accounts[@]}"
    local i
    for ((i = 0; i < length; i++)); do
        post_transaction \
            "new-country" \
            "NEW_COUNTRY" \
            "${new_country_accounts[$i]}" \
            "$((220 + i)).00" \
            "$(currency_for_index "$i")" \
            "$(different_country_for_index "$i")" \
            "$(category_for_index "$((i + 5))")"
    done

    length="${#high_new_country_accounts[@]}"
    for ((i = 0; i < length; i++)); do
        post_transaction \
            "high-new-country" \
            "HIGH_AMOUNT + NEW_COUNTRY" \
            "${high_new_country_accounts[$i]}" \
            "$((14500 + (i * 250))).00" \
            "$(currency_for_index "$i")" \
            "$(different_country_for_index "$((i + 7))")" \
            "$(category_for_index "$((i + 6))")"
    done
}

send_frequency_frauds() {
    local length="${#frequency_accounts[@]}"
    local i

    for ((i = 0; i < length; i++)); do
        post_transaction \
            "frequency" \
            "HIGH_FREQUENCY" \
            "${frequency_accounts[$i]}" \
            "$((210 + i)).00" \
            "$(currency_for_index "$i")" \
            "$(country_for_index "$((i + 1))")" \
            "$(category_for_index "$((i + 2))")"

        post_transaction \
            "frequency" \
            "HIGH_FREQUENCY" \
            "${frequency_accounts[$i]}" \
            "$((230 + i)).00" \
            "$(currency_for_index "$((i + 1))")" \
            "$(country_for_index "$((i + 1))")" \
            "$(category_for_index "$((i + 3))")"
    done

    length="${#high_frequency_accounts[@]}"
    for ((i = 0; i < length; i++)); do
        post_transaction \
            "high-frequency" \
            "HIGH_AMOUNT + HIGH_FREQUENCY" \
            "${high_frequency_accounts[$i]}" \
            "$((16000 + (i * 500))).00" \
            "$(currency_for_index "$i")" \
            "$(country_for_index "$((i + 3))")" \
            "$(category_for_index "$((i + 7))")"
    done

    length="${#frequency_new_country_accounts[@]}"
    for ((i = 0; i < length; i++)); do
        post_transaction \
            "freq-new-country" \
            "HIGH_FREQUENCY + NEW_COUNTRY" \
            "${frequency_new_country_accounts[$i]}" \
            "$((330 + i)).00" \
            "$(currency_for_index "$i")" \
            "$(different_country_for_index "$((i + 6))")" \
            "$(category_for_index "$((i + 8))")"
    done
}

send_night_candidates() {
    local utc_hour_raw
    local utc_hour
    local expected
    utc_hour_raw="$(date -u +%H)"
    utc_hour=$((10#$utc_hour_raw))

    if ((utc_hour >= 0 && utc_hour <= 5)); then
        expected="NIGHT_TIME"
    else
        expected="VERIFIED outside UTC night window"
    fi

    for i in {1..10}; do
        post_transaction \
            "night-candidate" \
            "$expected" \
            "$(new_uuid)" \
            "$((3500 + (i * 10))).00" \
            "$(currency_for_index "$i")" \
            "$(country_for_index "$((i + 9))")" \
            "$(category_for_index "$((i + 1))")"
    done
}

main() {
    require_command uuidgen

    if ! is_true "$DRY_RUN"; then
        require_command curl
    fi

    print_header
    wait_for_payment_service
    validate_payment_schema_if_possible

    send_verified_baseline
    send_high_amount
    send_country_seed_transactions
    send_frequency_warmups

    wait_for_processing "country seeds, outbox publishing and Kafka Streams frequency windows"

    send_new_country_frauds
    send_frequency_frauds
    send_night_candidates

    echo
    echo "Done. Sent: $sent, failed: $failed"
    echo
    echo "Useful checks:"
    echo "  Kafka UI:       http://localhost:8090"
    echo "  Dashboard UI:   http://localhost:8084"
    echo "  Payment health: ${PAYMENT_BASE_URL}/actuator/health"
}

main "$@"
