var result, stripScripts;

stripScripts = function (value) {
    var pattern = /<script[^>]*>((\n|\r|.)*?)<\/script>/img;
    return value.replace(pattern, '');
}

result = stripScripts('<script type="text/javascript">var a = 123;</script>script 이외');
show(result);

result = stripScripts('<script></script><script></script>script 두 개 작성');
show(result);
