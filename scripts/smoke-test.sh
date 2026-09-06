set -u
API=http://localhost:8080/api/v1
S=$RANDOM
jqid() { python3 -c "import sys,json;print(json.load(sys.stdin)['id'])"; }
line() { printf '\n\033[1m%s\033[0m\n' "$*"; }

line "1. Register three users"
A=$(curl -s -X POST $API/users -H 'Content-Type: application/json' -d "{\"name\":\"Alice\",\"email\":\"alice$S@ex.com\"}" | jqid)
B=$(curl -s -X POST $API/users -H 'Content-Type: application/json' -d "{\"name\":\"Bob\",\"email\":\"bob$S@ex.com\"}"     | jqid)
C=$(curl -s -X POST $API/users -H 'Content-Type: application/json' -d "{\"name\":\"Carol\",\"email\":\"carol$S@ex.com\"}" | jqid)
D=$(curl -s -X POST $API/users -H 'Content-Type: application/json' -d "{\"name\":\"Dan\",\"email\":\"dan$S@ex.com\"}"     | jqid)
echo "alice=$A bob=$B carol=$C dan=$D (dan stays outside the group)"

line "2. GET /users/me as Alice"
curl -s $API/users/me -H "X-User-Id: $A"; echo

line "3. Duplicate email -> expect 409"
curl -s -w " [%{http_code}]\n" -X POST $API/users -H 'Content-Type: application/json' -d "{\"name\":\"Alice again\",\"email\":\"alice$S@ex.com\"}"

line "4. Validation: blank name + bad email -> expect 400 with fieldErrors"
curl -s -w " [%{http_code}]\n" -X POST $API/users -H 'Content-Type: application/json' -d '{"name":"","email":"nope"}'

line "5. Missing X-User-Id -> expect 401"
curl -s -w " [%{http_code}]\n" $API/users/me

line "6. Alice creates a group"
G=$(curl -s -X POST $API/groups -H "X-User-Id: $A" -H 'Content-Type: application/json' -d '{"name":"Goa Trip","currency":"INR"}' | jqid)
echo "group=$G"

line "7. Alice adds Bob and Carol"
curl -s -w " [%{http_code}]\n" -X POST $API/groups/$G/members -H "X-User-Id: $A" -H 'Content-Type: application/json' -d "{\"userId\":$B}"
curl -s -w " [%{http_code}]\n" -X POST $API/groups/$G/members -H "X-User-Id: $A" -H 'Content-Type: application/json' -d "{\"userId\":$C}"

line "8. Adding Bob twice -> expect 409"
curl -s -w " [%{http_code}]\n" -X POST $API/groups/$G/members -H "X-User-Id: $A" -H 'Content-Type: application/json' -d "{\"userId\":$B}"

line "9. Group detail as Bob (a member)"
curl -s $API/groups/$G -H "X-User-Id: $B" | python3 -m json.tool

line "10. Dan is NOT a member -> expect 403"
curl -s -w " [%{http_code}]\n" $API/groups/$G -H "X-User-Id: $D"

line "11. Bob (a plain MEMBER) tries to add Dan -> expect 403"
curl -s -w " [%{http_code}]\n" -X POST $API/groups/$G/members -H "X-User-Id: $B" -H 'Content-Type: application/json' -d "{\"userId\":$D}"

line "12. Alice (sole ADMIN) tries to leave -> expect 400"
curl -s -w " [%{http_code}]\n" -X DELETE $API/groups/$G/members/$A -H "X-User-Id: $A"

line "13. Carol leaves on her own -> expect 204"
curl -s -o /dev/null -w "[%{http_code}]\n" -X DELETE $API/groups/$G/members/$C -H "X-User-Id: $C"

line "14. Alice lists her groups (memberCount should now be 2)"
curl -s $API/groups -H "X-User-Id: $A" | python3 -m json.tool

line "15. Unknown user id in header -> expect 401"
curl -s -w " [%{http_code}]\n" $API/users/me -H "X-User-Id: 99999999"
