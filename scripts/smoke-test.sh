set -u
cd "$(dirname "$0")/.." || exit 1
. scripts/lib.sh
S=$RANDOM

line "Register four users (Dan stays outside the group)"
read A TA < <(register Alice "alice$S@ex.com")
read B TB < <(register Bob   "bob$S@ex.com")
read C TC < <(register Carol "carol$S@ex.com")
read D TD < <(register Dan   "dan$S@ex.com")
echo "alice=$A bob=$B carol=$C dan=$D"

line "GET /users/me as Alice"
curl -s $API/users/me -H "$(authh "$TA")"; echo

line "Search users by email fragment"
curl -s "$API/users/search?email=bob$S" -H "$(authh "$TA")"; echo

line "Alice creates a group"
G=$(curl -s -X POST $API/groups -H "$(authh "$TA")" -H 'Content-Type: application/json' \
  -d '{"name":"Goa Trip","currency":"INR"}' | jget id)
echo "group=$G"

line "Alice adds Bob and Carol -> expect 201 each"
for U in $B $C; do
  curl -s -w " [%{http_code}]\n" -X POST $API/groups/$G/members -H "$(authh "$TA")" \
    -H 'Content-Type: application/json' -d "{\"userId\":$U}"
done

line "Adding Bob twice -> expect 409"
curl -s -w " [%{http_code}]\n" -X POST $API/groups/$G/members -H "$(authh "$TA")" \
  -H 'Content-Type: application/json' -d "{\"userId\":$B}"

line "Group detail as Bob (a member) -> expect 200"
curl -s $API/groups/$G -H "$(authh "$TB")" | pp

line "Dan is not a member -> expect 403"
curl -s -w " [%{http_code}]\n" $API/groups/$G -H "$(authh "$TD")"

line "Bob (plain MEMBER) tries to add Dan -> expect 403 from @PreAuthorize"
curl -s -w " [%{http_code}]\n" -X POST $API/groups/$G/members -H "$(authh "$TB")" \
  -H 'Content-Type: application/json' -d "{\"userId\":$D}"

line "Alice (sole ADMIN) tries to leave -> expect 400"
curl -s -w " [%{http_code}]\n" -X DELETE $API/groups/$G/members/$A -H "$(authh "$TA")"

line "Carol leaves on her own -> expect 204"
curl -s -o /dev/null -w "[%{http_code}]\n" -X DELETE $API/groups/$G/members/$C -H "$(authh "$TC")"

line "Alice lists her groups (memberCount should be 2)"
curl -s $API/groups -H "$(authh "$TA")" | pp

line "Validation: blank group name -> expect 400 with fieldErrors"
curl -s -w " [%{http_code}]\n" -X POST $API/groups -H "$(authh "$TA")" \
  -H 'Content-Type: application/json' -d '{"name":"","currency":"inr"}'

line "No token at all -> expect 401"
curl -s -w " [%{http_code}]\n" $API/groups
