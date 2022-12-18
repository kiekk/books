let defaultOwner = {firstName: "Martin", lastName: "Fowler"};

// 객체 자체를 전달하면 외부에서 객체를 조작할 수 있다.
export function getDefaultOwner() {
    return defaultOwner;
}

export function setDefaultOwner(arg) {
    defaultOwner = arg;
}