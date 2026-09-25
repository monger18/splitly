set -u
API=http://localhost:8080/api/v1
S=$RANDOM
id(){ python3 -c "import sys,json;print(json.load(sys.stdin)['id'])"; }
line(){ printf '\n\033[1m%s\033[0m\n' "$*"; }
pp(){ python3 -m json.tool; }

A=$(curl -s -X POST $API/users -H 'Content-Type: application/json' -d "{\"name\":\"Alice\",\"email\":\"ba$S@ex.com\"}" | id)
B=$(curl -s -X POST $API/users -H 'Content-Type: application/json' -d "{\"name\":\"Bob\",\"email\":\"bb$S@ex.com\"}"   | id)
C=$(curl -s -X POST $API/users -H 'Content-Type: application/json' -d "{\"name\":\"Carol\",\"email\":\"bc$S@ex.com\"}" | id)
G=$(curl -s -X POST $API/groups -H "X-User-Id: $A" -H 'Content-Type: application/json' -d '{"name":"Goa Trip"}' | id)
for U in $B $C; do curl -s -o /dev/null -X POST $API/groups/$G/members -H "X-User-Id: $A" -H 'Content-Type: application/json' -d "{\"userId\":$U}"; done
echo "alice=$A bob=$B carol=$C group=$G"

line "Alice pays 900 for dinner, split equally"
curl -s -o /dev/null -X POST $API/groups/$G/expenses -H "X-User-Id: $A" -H 'Content-Type: application/json' \
 -d "{\"description\":\"Dinner\",\"amount\":900.00,\"paidBy\":$A,\"splitType\":\"EQUAL\",\"expenseDate\":\"2026-09-01\",\"participants\":[{\"userId\":$A},{\"userId\":$B},{\"userId\":$C}]}"
line "Bob pays 300 for the cab, split equally"
curl -s -o /dev/null -X POST $API/groups/$G/expenses -H "X-User-Id: $B" -H 'Content-Type: application/json' \
 -d "{\"description\":\"Cab\",\"amount\":300.00,\"paidBy\":$B,\"splitType\":\"EQUAL\",\"expenseDate\":\"2026-09-02\",\"participants\":[{\"userId\":$A},{\"userId\":$B},{\"userId\":$C}]}"

line "Balances  (expect Alice +500, Bob -100, Carol -400; sum 0)"
curl -s $API/groups/$G/balances -H "X-User-Id: $A" | pp
curl -s $API/groups/$G/balances -H "X-User-Id: $A" | python3 -c "
import sys,json
d=json.load(sys.stdin); t=sum(float(x['net']) for x in d)
print('  sum of all nets = %.2f' % t); assert abs(t)<1e-9, 'BALANCES DO NOT SUM TO ZERO'
print('  sums to zero OK')"

line "Simplified  (expect 2 payments, not 4)"
curl -s $API/groups/$G/balances/simplified -H "X-User-Id: $A" | pp

line "Try removing Carol while she owes 400 -> expect 400"
curl -s -w " [%{http_code}]\n" -X DELETE $API/groups/$G/members/$C -H "X-User-Id: $A"

line "Carol settles up: pays Alice 400"
curl -s -X POST $API/groups/$G/settlements -H "X-User-Id: $C" -H 'Content-Type: application/json' \
 -d "{\"fromUserId\":$C,\"toUserId\":$A,\"amount\":400.00,\"note\":\"UPI\"}" | pp

line "Balances after settling  (expect Alice +100, Bob -100, Carol 0)"
curl -s $API/groups/$G/balances -H "X-User-Id: $A" | pp

line "Simplified now  (expect a single payment Bob -> Alice 100)"
curl -s $API/groups/$G/balances/simplified -H "X-User-Id: $A" | pp

line "Carol is square, so she can now leave -> expect 204"
curl -s -o /dev/null -w "[%{http_code}]\n" -X DELETE $API/groups/$G/members/$C -H "X-User-Id: $C"

line "Alice's balances across all her groups"
curl -s $API/users/me/balances -H "X-User-Id: $A" | pp

line "Settlement history"
curl -s $API/groups/$G/settlements -H "X-User-Id: $A" | pp

line "Settlement with yourself -> expect 400"
curl -s -w " [%{http_code}]\n" -X POST $API/groups/$G/settlements -H "X-User-Id: $A" -H 'Content-Type: application/json' \
 -d "{\"fromUserId\":$A,\"toUserId\":$A,\"amount\":10.00}"

line "Non-member peeks at balances -> expect 403"
D=$(curl -s -X POST $API/users -H 'Content-Type: application/json' -d "{\"name\":\"Dan\",\"email\":\"bd$S@ex.com\"}" | id)
curl -s -w " [%{http_code}]\n" $API/groups/$G/balances -H "X-User-Id: $D"
