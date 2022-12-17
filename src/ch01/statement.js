export {statement};
import createStatementData from "./createStatementData";

// HTML 버전 기능 추가를 위한 함수 분리
function statement(invoice, plays) {
    return renderPlainText(createStatementData(invoice, plays));
}

function renderPlainText(data) {
    let result = `청구 내역 (고객명: ${data.customer})\n`;

    for (let perf of data.performances) {
        // 청구 내역 출력
        result += ` ${perf.play.name} : ${usd(perf.amount)} (${perf.audience} 석)\n`;
    }

    result += `총액: ${usd(data.totalAmount)}\n`;
    result += `적립 포인트: ${data.totalVolumeCredits}점\n`;
    return result;

    function usd(aNumber) {
        return new Intl.NumberFormat('en-US', {
            style: 'currency', currency: 'USD', minimumFractionDigits: 2
        }).format(aNumber / 100);
    }
}