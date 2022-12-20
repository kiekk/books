export function payAmount(employee) {
    let result;
    if (employee.isSeparated) { // 퇴사
        result = {
            amount: 0,
            reasonCode: "SEP"
        }
    } else {
        if (employee.isRetired) { // 은퇴
            result = {
                amount: 0,
                reasonCode: "RET"
            };
        } else {
            // 급여 계산
            result = {
                amount: 999,
                reasonCode: "UNICORN"
            }
        }
    }
    return result;
}