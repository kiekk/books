let callbackEcho = message => message;
let callbackEchoWithLength = message => `${message}(${message.length})`;
function echoFunction2(message, callback) {
    return callback(message);
}
let responseEcho = echoFunction2('hello', callbackEcho);
let responseEchoWithLength = echoFunction2('hello', callbackEchoWithLength);
console.log(responseEcho);
console.log(responseEchoWithLength);
