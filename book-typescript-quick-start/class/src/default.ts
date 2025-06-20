class Account {
    balance: number

    get getBalance() {
        return this.balance
    }

    set setBalance(amount: number) {
        this.balance += amount
    }

    deposite(depositeAmount: number) {
        this.setBalance = depositeAmount
    }

    constructor(defaultBalance: number = 0, protected bankName: string = 'happy bank',
                readonly interestRate: number = 0.1) {
        this.balance = defaultBalance
    }

    getInterestRate() {
        return this.interestRate
    }

    getDefaultBalance() {
        // 생성자 매개변수 defaultBalance는 private이므로 this. 으로 호출 불가
        // return this.defaultBalance
    }
}

class MyAccount extends Account {
    constructor() {
        super();
        this.deposite(1000)
        this.setBalance = 1000
        console.log(`2번) 적금 : ${this.balance}원, ${this.getBalance}원 / 
        이율 : ${this.interestRate}, ${this.getInterestRate()}% /
        은행명 : ${this.bankName} `)
    }
}

let account = new Account()
console.log(`1번) 적금 : ${account.balance}원, ${account.getBalance}원 /
            이율 : ${account.interestRate}, ${account.getInterestRate()}%`)

let myAccount = new MyAccount()

/*
    실행 결과
    1번) 적금 : 0원, 0원 /
            이율 : 0.1, 0.1%
    2번) 적금 : 2000원, 2000원 /
            이율 : 0.1, 0.1% /
            은행명 : happy bank
 */