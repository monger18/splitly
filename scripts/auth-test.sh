set -u
cd "$(dirname "$0")/.." || exit 1
. scripts/lib.sh
S=$RANDOM
EMAIL="auth$S@ex.com"

line "1. Register -> expect 201 with both tokens"
REG=$(curl -s -w '\n%{http_code}' -X POST $API/auth/register -H 'Content-Type: application/json' \
  -d "{\"name\":\"Alice\",\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}")
echo "$REG" | tail -1 | sed 's/^/  status /'
BODY=$(echo "$REG" | head -n -1); echo "$BODY" | pp
ACCESS=$(printf '%s' "$BODY" | jget accessToken)
REFRESH=$(printf '%s' "$BODY" | jget refreshToken)

line "2. Protected endpoint WITHOUT a token -> expect 401"
curl -s -w " [%{http_code}]\n" $API/users/me

line "3. Protected endpoint WITH the token -> expect 200"
curl -s -w " [%{http_code}]\n" $API/users/me -H "$(authh "$ACCESS")"

line "4. Garbage token -> expect 401"
curl -s -w " [%{http_code}]\n" $API/users/me -H "Authorization: Bearer not.a.token"

line "5. REFRESH token used as an access token -> expect 401 (the typ claim)"
curl -s -w " [%{http_code}]\n" $API/users/me -H "$(authh "$REFRESH")"

line "6. Login with the right password -> expect 200"
curl -s -w " [%{http_code}]\n" -X POST $API/auth/login -H 'Content-Type: application/json' \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}" | head -c 120; echo

line "7. Login with the wrong password -> expect 401"
curl -s -w " [%{http_code}]\n" -X POST $API/auth/login -H 'Content-Type: application/json' \
  -d "{\"email\":\"$EMAIL\",\"password\":\"wrong-password\"}"

line "8. Login with an unknown email -> expect 401 and the SAME message as #7"
curl -s -w " [%{http_code}]\n" -X POST $API/auth/login -H 'Content-Type: application/json' \
  -d "{\"email\":\"nobody$S@ex.com\",\"password\":\"$PASSWORD\"}"

line "9. Refresh -> expect 200 with a fresh pair"
curl -s -w " [%{http_code}]\n" -X POST $API/auth/refresh -H 'Content-Type: application/json' \
  -d "{\"refreshToken\":\"$REFRESH\"}" | head -c 160; echo

line "10. ACCESS token used as a refresh token -> expect 401"
curl -s -w " [%{http_code}]\n" -X POST $API/auth/refresh -H 'Content-Type: application/json' \
  -d "{\"refreshToken\":\"$ACCESS\"}"

line "11. Duplicate email -> expect 409"
curl -s -w " [%{http_code}]\n" -X POST $API/auth/register -H 'Content-Type: application/json' \
  -d "{\"name\":\"Alice again\",\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}"

line "12. Short password -> expect 400 with fieldErrors"
curl -s -w " [%{http_code}]\n" -X POST $API/auth/register -H 'Content-Type: application/json' \
  -d "{\"name\":\"Bob\",\"email\":\"short$S@ex.com\",\"password\":\"abc\"}"

line "13. Swagger and health stay public -> expect 200"
curl -s -o /dev/null -w "  health      [%{http_code}]\n" http://localhost:8080/actuator/health
curl -s -o /dev/null -w "  api-docs    [%{http_code}]\n" http://localhost:8080/v3/api-docs
