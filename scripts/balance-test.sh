set -u
cd "$(dirname "$0")/.." || exit 1
. scripts/lib.sh
S=$RANDOM
read A TA < <(register Alice "a$S@ex.com")
read B TB < <(register Bob "b$S@ex.com")
read C TC < <(register Carol "c$S@ex.com")
G=$(curl -s -X POST $API/groups -H "Authorization: Bearer $TA" -H 'Content-Type: application/json' -d '{"name":"Goa Trip"}' | jget id)
for U in $B $C; do curl -s -o /dev/null -X POST $API/groups/$G/members -H "Authorization: Bearer $TA" -H 'Content-Type: application/json' -d "{\"userId\":$U}"; done
echo "alice=$A bob=$B carol=$C group=$G"

line "Alice pays 900 for dinner, split equally"
curl -s -o /dev/null -X POST $API/groups/$G/expenses -H "Authorization: Bearer $TA" -H 'Content-Type: application/json' \
 -d "{\"description\":\"Dinner\",\"amount\":900.00,\"paidBy\":$A,\"splitType\":\"EQUAL\",\"expenseDate\":\"2026-09-01\",\"participants\":[{\"userId\":$A},{\"userId\":$B},{\"userId\":$C}]}"
line "Bob pays 300 for the cab, split equally"
curl -s -o /dev/null -X POST $API/groups/$G/expenses -H "Authorization: Bearer $TB" -H 'Content-Type: application/json' \
 -d "{\"description\":\"Cab\",\"amount\":300.00,\"paidBy\":$B,\"splitType\":\"EQUAL\",\"expenseDate\":\"2026-09-02\",\"participants\":[{\"userId\":$A},{\"userId\":$B},{\"userId\":$C}]}"

line "Balances  (expect Alice +500, Bob -100, Carol -400; sum 0)"
curl -s $API/groups/$G/balances -H "Authorization: Bearer $TA" | pp
curl -s $API/groups/$G/balances -H "Authorization: Bearer $TA" | python3 -c "
import sys,json
d=json.load(sys.stdin); t=sum(float(x['net']) for x in d)
print('  sum of all nets = %.2f' % t); assert abs(t)<1e-9, 'BALANCES DO NOT SUM TO ZERO'
print('  sums to zero OK')"

line "Simplified  (expect 2 payments, not 4)"
curl -s $API/groups/$G/balances/simplified -H "Authorization: Bearer $TA" | pp

line "Try removing Carol while she owes 400 -> expect 400"
curl -s -w " [%{http_code}]\n" -X DELETE $API/groups/$G/members/$C -H "Authorization: Bearer $TA"

line "Carol settles up: pays Alice 400"
curl -s -X POST $API/groups/$G/settlements -H "Authorization: Bearer $TC" -H 'Content-Type: application/json' \
 -d "{\"fromUserId\":$C,\"toUserId\":$A,\"amount\":400.00,\"note\":\"UPI\"}" | pp

line "Balances after settling  (expect Alice +100, Bob -100, Carol 0)"
curl -s $API/groups/$G/balances -H "Authorization: Bearer $TA" | pp

line "Simplified now  (expect a single payment Bob -> Alice 100)"
curl -s $API/groups/$G/balances/simplified -H "Authorization: Bearer $TA" | pp

line "Carol is square, so she can now leave -> expect 204"
curl -s -o /dev/null -w "[%{http_code}]\n" -X DELETE $API/groups/$G/members/$C -H "Authorization: Bearer $TC"

line "Alice's balances across all her groups"
curl -s $API/users/me/balances -H "Authorization: Bearer $TA" | pp

line "Settlement history"
curl -s $API/groups/$G/settlements -H "Authorization: Bearer $TA" | pp

line "Settlement with yourself -> expect 400"
curl -s -w " [%{http_code}]\n" -X POST $API/groups/$G/settlements -H "Authorization: Bearer $TA" -H 'Content-Type: application/json' \
 -d "{\"fromUserId\":$A,\"toUserId\":$A,\"amount\":10.00}"

line "Non-member peeks at balances -> expect 403"
read D TD < <(register Dan "d$S@ex.com")
curl -s -w " [%{http_code}]\n" $API/groups/$G/balances -H "Authorization: Bearer $TD"
