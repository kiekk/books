var stripTags = function(base) {
    return base.replace(/<\/?[^>]+>/ig, '');
};

var result = stripTags('<div id="sports">축구</div>');
show(result);
