#!/bin/bash
# generate-ssl.sh – генерация самоподписанного SSL-сертификата для разработки

KEYSTORE_FILE="keystore.p12"
KEYSTORE_PASS="changeit"
KEY_ALIAS="tomcat"
VALIDITY_DAYS=365

# Если файл уже существует – выходим
if [ -f "$KEYSTORE_FILE" ]; then
    echo "✅ SSL certificate already exists: $KEYSTORE_FILE"
    exit 0
fi

# Функция для генерации через keytool
gen_with_keytool() {
    echo "🔐 Generating SSL certificate using keytool..."
    keytool -genkeypair \
        -alias "$KEY_ALIAS" \
        -keyalg RSA \
        -keysize 2048 \
        -storetype PKCS12 \
        -keystore "$KEYSTORE_FILE" \
        -validity "$VALIDITY_DAYS" \
        -dname "CN=localhost, OU=HouseholdStore, O=GALThor, C=RU" \
        -storepass "$KEYSTORE_PASS" \
        -keypass "$KEYSTORE_PASS" \
        -noprompt
    return $?
}

# Функция для генерации через openssl
gen_with_openssl() {
    echo "🔐 Generating SSL certificate using openssl..."
    # Создаём приватный ключ
    openssl genrsa -out server.key 2048
    # Создаём самоподписанный сертификат
    openssl req -new -x509 -key server.key -out server.crt -days "$VALIDITY_DAYS" \
        -subj "/CN=localhost/OU=HouseholdStore/O=GALThor/C=RU" \
        -addext "subjectAltName = DNS:localhost"
    # Объединяем в PKCS12
    openssl pkcs12 -export -in server.crt -inkey server.key \
        -out "$KEYSTORE_FILE" -name "$KEY_ALIAS" \
        -password pass:"$KEYSTORE_PASS"
    local result=$?
    # Удаляем временные файлы
    rm -f server.key server.crt
    return $result
}

# Пробуем keytool, если есть
if command -v keytool &> /dev/null; then
    gen_with_keytool
    if [ $? -eq 0 ]; then
        echo "✅ SSL certificate generated with keytool: $KEYSTORE_FILE"
    else
        echo "❌ keytool failed, trying openssl..."
        gen_with_openssl
    fi
# Иначе пробуем openssl
elif command -v openssl &> /dev/null; then
    gen_with_openssl
    if [ $? -eq 0 ]; then
        echo "✅ SSL certificate generated with openssl: $KEYSTORE_FILE"
    else
        echo "❌ openssl failed. Please generate certificate manually."
        exit 1
    fi
else
    echo "❌ Neither keytool nor openssl found."
    echo "Please install Java JDK (for keytool) or openssl, or generate certificate manually:"
    echo "  keytool -genkeypair -alias tomcat -keyalg RSA -keysize 2048 -storetype PKCS12 -keystore keystore.p12 -validity 365 -dname \"CN=localhost, OU=HouseholdStore, O=GALThor, C=RU\" -storepass changeit -keypass changeit -noprompt"
    exit 1
fi

# Если генерация прошла успешно, копируем в resources (опционально)
if [ -f "$KEYSTORE_FILE" ] && [ -d "src/main/resources" ]; then
    cp "$KEYSTORE_FILE" src/main/resources/
    echo "📋 Certificate also copied to src/main/resources/"
fi