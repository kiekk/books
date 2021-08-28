function echoFunction(message, callback) {
    return callback(message);
}
// 1
let responseMessage = echoFunction('hello world', message => message);
console.log(responseMessage);
let callbackEcho = message => message;
responseMessage = echoFunction('happy world', callbackEcho);
console.log(responseMessage);
