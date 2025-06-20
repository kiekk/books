interface IPerson {
    height: number
    getAlias: () => string
    getAge(): number
}

class PoliceOfficer implements IPerson {
    height: number
    getAlias = () => 'happy'
    getAge(): number {
        return 10
    }
    hasClub(): boolean {
        return true
    }
}

let policeMan: IPerson = new PoliceOfficer()
console.log(policeMan.getAlias())
console.log(policeMan.getAge())