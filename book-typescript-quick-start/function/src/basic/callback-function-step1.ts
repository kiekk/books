function echoFunction(message: string, callback) {
    return callback(message);
}

// 1
let responseMessage = echoFunction('hello world', message => message);
console.log(responseMessage);

// 2
// 콜백 함수의 선언을 외부로 분리하여 함수를 더 간결하게 호출
// type EchoCallbackType = (message: string) => void
// let callbackEcho: EchoCallbackType = message => message
// responseMessage = echoFunction('happy world', callbackEcho);
// console.log(responseMessage);