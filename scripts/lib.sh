# Shared helpers for the smoke scripts. Source, do not execute.
API=${API:-http://localhost:8080/api/v1}
PASSWORD=${PASSWORD:-password123}

line(){ printf '\n\033[1m%s\033[0m\n' "$*"; }
pp(){ python3 -m json.tool; }
jget(){ python3 -c "import sys,json;print(json.load(sys.stdin)['$1'])"; }

# register <name> <email>  -> prints "<userId> <accessToken>"
register() {
  local body token uid
  body=$(curl -s -X POST "$API/auth/register" -H 'Content-Type: application/json' \
    -d "{\"name\":\"$1\",\"email\":\"$2\",\"password\":\"$PASSWORD\"}")
  token=$(printf '%s' "$body" | jget accessToken) || { echo "REGISTER FAILED: $body" >&2; return 1; }
  # The token carries the user id as its subject, but the API is the contract, so ask it.
  uid=$(curl -s "$API/users/me" -H "Authorization: Bearer $token" | jget id)
  echo "$uid $token"
}

# auth <token> -> the header argument pair
authh(){ echo "Authorization: Bearer $1"; }
