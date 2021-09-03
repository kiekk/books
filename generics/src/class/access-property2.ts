interface IName {
    name: string
}

class Profile2 implements IName {
    name: string = 'happy'
}

// class에 generic을 선언하면 메소드에서 IName 인터페이스를 제약할 필요가 없습니다.
class Accessor<T extends IName> {
    getKey(obj: T) {
        return obj.name
    }
}