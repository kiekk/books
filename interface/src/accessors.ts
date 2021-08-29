// 1
class Student {
    name: string
    birthYear: number
}

let student = new Student()

// Setter
student.name = 'happy'
student.birthYear = 2017

// Getter
console.log(student.name)
console.log(student.birthYear)

// 2
class Student2 {
    private studentName: string
    private studentBirthYear: number

    get name(): string {
        return this.studentName
    }

    set name(name: string) {
        if(name.indexOf('happy') !== 0) {
            this.studentName = name
        }
    }

    get birthYear(): number {
        return this.studentBirthYear
    }

    set birthYear(year: number) {
        if(year <= 2000) {
            this.studentBirthYear = year
        }
    }
}

let student2 = new Student2()
student2.birthYear = 2001
console.log(`1번 : ${student2.birthYear}`)

student2.birthYear = 2000
console.log(`2번 : ${student2.birthYear}`)

student2.name = 'happy'
console.log(`3번 : ${student2.name}`)

student2.name = 'lucky'
console.log(`4번 : ${student2.name}`)


