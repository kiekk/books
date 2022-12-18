let defaultOwner = {firstName: "Martin", lastName: "Fowler"};

// Object.assign 함수를 사용하여 불변 객체를 전달.
// 외부에서 값을 조작할 수 없도록 한다.
export function getDefaultOwner() {
    return Object.assign({}, defaultOwner);
}

export function setDefaultOwner(arg) {
    defaultOwner = arg;
}