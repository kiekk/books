export function payAmount(employee) {
    if (employee.isSeparated) return { amount: 0, reasonCode: "SEP" }; // 퇴사
    if (employee.isRetired) return { amount: 0, reasonCode: "RET" }; // 은퇴
    return { amount: 999, reasonCode: "UNICORN" }; // 급여 계산
}