var result = 'border-bottom-color'.replace(/-[a-z]/ig, function (cvt) {
    return cvt.charAt(1).toUpperCase();
})
show(result);
