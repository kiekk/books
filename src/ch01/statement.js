export {statement};

// 공연료 청구 프로그램
function statement(invoice, plays) {
    let result = `청구 내역 (고객명: ${invoice.customer})\n`;

    for (let perf of invoice.performances) {
        // 청구 내역 출력
        result += ` ${playFor(perf).name} : ${usd(amountFor(perf))} (${perf.audience} 석)\n`;
    }

    result += `총액: ${usd(totalAmount())}\n`;
    result += `적립 포인트: ${totalVolumeCredits()}점\n`;
    return result;

    function totalAmount() {
        let result = 0;
        for (let perf of invoice.performances) {
            result += amountFor(perf);
        }
        return result;
    }

    function totalVolumeCredits() {
        let volumeCredits = 0;
        for (let perf of invoice.performances) {
            volumeCredits += volumeCreditsFor(perf);
        }
        return volumeCredits;
    }

    function usd(aNumber) {
        return new Intl.NumberFormat('en-US', {
            style: 'currency', currency: 'USD', minimumFractionDigits: 2
        }).format(aNumber / 100);
    }

    // 적립 포인트 계산 코드 함수로 분리
    function volumeCreditsFor(aPerformance) {
        let result = 0;
        result += Math.max(aPerformance.audience - 30, 0);

        // 희극 관객 5명마다 추가 포인트 제공
        if ('comedy' === playFor(aPerformance).type) {
            result += Math.floor(aPerformance.audience / 5);
        }
        return result;

    }

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