var lineNumber = 0;

function show(value, el) {
    var ch, result;

    lineNumber += 1;
    ch = document.createElement('div');
    ch.id = 'mc_result_' + lineNumber;
    document.getElementById(el || 'showArea').appendChild(ch);

    result = document.createTextNode(lineNumber + '. ' + value);
    document.getElementById(ch.id).appendChild(result);
}
