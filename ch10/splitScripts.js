var splitScripts = function (value) {
    var pattern = /<script[^>]*>((\n|\r|.)*?)<\/script>/img;
    this.outer = value.replace(pattern, function () {
        this.inter = arguments[1] + '\n';
        return '';
    });
}

splitScripts('<script type="text/javascript">var a = 123;</script>script 이외');
show(this.outer);
show(this.inter);
