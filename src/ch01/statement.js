export {statement};

// 공연료 청구 프로그램
function statement(invoice, plays) {
    let totalAmount = 0;
    let volumeCredits = 0;
    let result = `청구 내역 (고객명: ${invoice.customer})\n`;

    const format = new Intl.NumberFormat('en-US', {
        style: 'currency', currency: 'USD', minimumFractionDigits: 2
    }).format;

    // 변수 인라인하기
    /*
        개인적으로는 이 방법은 별로인 것 같다.
        play 라는 매개변수를 제거한다는 장점이 있다고 하지만,
        오히려 매번 play 대신 인라인 변수를 사용함으로써 가독성을 떨어트리는 것 같다.
     */
    for (let perf of invoice.performances) {
        let thisAmount = amountFor(perf);
        // 포인트 적립
        volumeCredits += Math.max(perf.audience - 30, 0);

        // 희극 관객 5명마다 추가 포인트 제공
        if ('comedy' === playFor(perf).type) {
            volumeCredits += Math.floor(perf.audience / 5);
        }

        // 청구 내역 출력
        result += ` ${playFor(perf).name} : ${format(thisAmount / 100)} (${perf.audience} 석)\n`;
        totalAmount += thisAmount;
    }

    result += `총액: ${format(totalAmount / 100)}\n`;
    result += `적립 포인트: ${volumeCredits}점\n`;
    return result;

    // 임시 변수를 질의 함수로 변경
    function playFor(aPerformance) {
        return plays[aPerformance.playID];
    }

    // 중첩 함수로 선언
    function amountFor(aPerformance) {
        let result = 0;

        switch (playFor(aPerformance).type) {
            case 'tragedy': // 비극
                result = 40000;
                if (aPerformance.audience > 30) {
                    result += 10000 * (aPerformance.audience - 30);
                }
                break;
            case 'comedy': // 희극
                result = 30000;
                if (aPerformance.audience > 20) {
                    result += 10000 + 500 * (aPerformance.audience - 20);
                }
                result += 300 * aPerformance.audience;
                break;
            default:
                throw new Error(`알 수 없는 장르: ${playFor(aPerformance).type}`);
        }

        return result;
    }
}