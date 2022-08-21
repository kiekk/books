var removeClass = function (cn, el) {
    var pattern = new RegExp('(?:^|\\s+)' + cn + '(?:\\s+|$)');
    el.className = el.className.replace(pattern, ' ');
}

var node = document.getElementById('regClass');
show(node.className);

removeClass('twoClass', node);
show(node.className);

//<div id="regClass" class="oneClass twoClass lastClass"></div>
