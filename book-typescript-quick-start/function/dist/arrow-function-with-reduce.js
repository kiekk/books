function getSum(nums) {
    return nums.reduce((a, b) => a + b);
}
let numSum = getSum([1, 2, 3, 4, 5]);
console.log(`numSum=${numSum}`);
