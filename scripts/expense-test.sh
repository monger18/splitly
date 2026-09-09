set -u
API=http://localhost:8080/api/v1
S=$RANDOM
id()  { python3 -c "import sys,json;print(json.load(sys.stdin)['id'])"; }
line(){ printf '\n\033[1m%s\033[0m\n' "$*"; }
shares(){ python3 -c "
import sys,json
d=json.load(sys.stdin)
tot=sum(float(s['amount']) for s in d['shares'])
print('  amount', d['amount'], '| shares', [f\"{s['name']}:{s['amount']}\" for s in d['shares']], '| sum %.2f' % tot)
assert abs(tot-float(d['amount']))<1e-9, 'SHARES DO NOT SUM TO TOTAL'
print('  reconciles OK')
"; }

A=$(curl -s -X POST $API/users -H 'Content-Type: application/json' -d "{\"name\":\"Alice\",\"email\":\"a$S@ex.com\"}" | id)
B=$(curl -s -X POST $API/users -H 'Content-Type: application/json' -d "{\"name\":\"Bob\",\"email\":\"b$S@ex.com\"}"   | id)
C=$(curl -s -X POST $API/users -H 'Content-Type: application/json' -d "{\"name\":\"Carol\",\"email\":\"c$S@ex.com\"}" | id)
G=$(curl -s -X POST $API/groups -H "X-User-Id: $A" -H 'Content-Type: application/json' -d '{"name":"Goa Trip","currency":"INR"}' | id)
curl -s -o /dev/null -X POST $API/groups/$G/members -H "X-User-Id: $A" -H 'Content-Type: application/json' -d "{\"userId\":$B}"
curl -s -o /dev/null -X POST $API/groups/$G/members -H "X-User-Id: $A" -H 'Content-Type: application/json' -d "{\"userId\":$C}"
echo "alice=$A bob=$B carol=$C group=$G"

post()   { curl -s -X POST $API/groups/$G/expenses -H "X-User-Id: $A" -H 'Content-Type: application/json' -d "$1"; }
postAs() { curl -s -X POST $API/groups/$G/expenses -H "X-User-Id: $1" -H 'Content-Type: application/json' -d "$2"; }

line "EQUAL — 900 three ways (divides cleanly)"
post "{\"description\":\"Dinner\",\"amount\":900.00,\"paidBy\":$A,\"splitType\":\"EQUAL\",\"expenseDate\":\"2026-09-01\",\"participants\":[{\"userId\":$A},{\"userId\":$B},{\"userId\":$C}]}" | shares

line "EQUAL — 10.00 three ways (the awkward one)"
E=$(postAs $B "{\"description\":\"Chai\",\"amount\":10.00,\"paidBy\":$B,\"splitType\":\"EQUAL\",\"expenseDate\":\"2026-09-02\",\"participants\":[{\"userId\":$A},{\"userId\":$B},{\"userId\":$C}]}")
echo "$E" | shares
EID=$(echo "$E" | id)

line "EXACT — 60/40"
post "{\"description\":\"Cab\",\"amount\":100.00,\"paidBy\":$A,\"splitType\":\"EXACT\",\"expenseDate\":\"2026-09-02\",\"participants\":[{\"userId\":$A,\"value\":60.00},{\"userId\":$B,\"value\":40.00}]}" | shares

line "PERCENTAGE — 33.33/33.33/33.34 of 10.00"
post "{\"description\":\"Snacks\",\"amount\":10.00,\"paidBy\":$C,\"splitType\":\"PERCENTAGE\",\"expenseDate\":\"2026-09-03\",\"participants\":[{\"userId\":$A,\"value\":33.33},{\"userId\":$B,\"value\":33.33},{\"userId\":$C,\"value\":33.34}]}" | shares

line "SHARES — 2:1:1 of 100.00"
post "{\"description\":\"Pizza\",\"amount\":100.00,\"paidBy\":$A,\"splitType\":\"SHARES\",\"expenseDate\":\"2026-09-03\",\"participants\":[{\"userId\":$A,\"value\":2},{\"userId\":$B,\"value\":1},{\"userId\":$C,\"value\":1}]}" | shares

line "EXACT that does not add up -> expect 400"
curl -s -w " [%{http_code}]\n" -X POST $API/groups/$G/expenses -H "X-User-Id: $A" -H 'Content-Type: application/json' \
  -d "{\"description\":\"Bad\",\"amount\":100.00,\"paidBy\":$A,\"splitType\":\"EXACT\",\"expenseDate\":\"2026-09-03\",\"participants\":[{\"userId\":$A,\"value\":60.00},{\"userId\":$B,\"value\":30.00}]}"

line "PERCENTAGE not summing to 100 -> expect 400"
curl -s -w " [%{http_code}]\n" -X POST $API/groups/$G/expenses -H "X-User-Id: $A" -H 'Content-Type: application/json' \
  -d "{\"description\":\"Bad\",\"amount\":100.00,\"paidBy\":$A,\"splitType\":\"PERCENTAGE\",\"expenseDate\":\"2026-09-03\",\"participants\":[{\"userId\":$A,\"value\":50},{\"userId\":$B,\"value\":40}]}"

line "Duplicate participant -> expect 400"
curl -s -w " [%{http_code}]\n" -X POST $API/groups/$G/expenses -H "X-User-Id: $A" -H 'Content-Type: application/json' \
  -d "{\"description\":\"Bad\",\"amount\":100.00,\"paidBy\":$A,\"splitType\":\"EQUAL\",\"expenseDate\":\"2026-09-03\",\"participants\":[{\"userId\":$A},{\"userId\":$A}]}"

line "Negative amount -> expect 400 with fieldErrors"
curl -s -w " [%{http_code}]\n" -X POST $API/groups/$G/expenses -H "X-User-Id: $A" -H 'Content-Type: application/json' \
  -d "{\"description\":\"Bad\",\"amount\":-5,\"paidBy\":$A,\"splitType\":\"EQUAL\",\"expenseDate\":\"2026-09-03\",\"participants\":[{\"userId\":$A}]}"

line "Unknown splitType -> expect 400"
curl -s -w " [%{http_code}]\n" -X POST $API/groups/$G/expenses -H "X-User-Id: $A" -H 'Content-Type: application/json' \
  -d "{\"description\":\"Bad\",\"amount\":10,\"paidBy\":$A,\"splitType\":\"SOMEHOW\",\"expenseDate\":\"2026-09-03\",\"participants\":[{\"userId\":$A}]}"

line "Paged list (newest first)"
curl -s "$API/groups/$G/expenses?page=0&size=3" -H "X-User-Id: $A" | python3 -m json.tool

line "Update expense $EID to SHARES 3:1 -> re-split"
curl -s -X PUT $API/expenses/$EID -H "X-User-Id: $B" -H 'Content-Type: application/json' \
  -d "{\"description\":\"Chai (updated)\",\"amount\":10.00,\"paidBy\":$B,\"splitType\":\"SHARES\",\"expenseDate\":\"2026-09-02\",\"participants\":[{\"userId\":$A,\"value\":3},{\"userId\":$B,\"value\":1}]}" | shares

line "Carol (not creator, not admin) deletes Bob's expense -> expect 403"
curl -s -w " [%{http_code}]\n" -X DELETE $API/expenses/$EID -H "X-User-Id: $C"

line "Bob deletes his own -> expect 204, then GET -> 404"
curl -s -o /dev/null -w "delete [%{http_code}]\n" -X DELETE $API/expenses/$EID -H "X-User-Id: $B"
curl -s -o /dev/null -w "get    [%{http_code}]\n" $API/expenses/$EID -H "X-User-Id: $B"
