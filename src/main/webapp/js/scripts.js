$(document).ready(function () {/* jQuery toggle layout */
    $('#btnToggle').click(function () {
        if ($(this).hasClass('on')) {
            $('#main .col-md-6').addClass('col-md-4').removeClass('col-md-6');
            $(this).removeClass('on');
        } else {
            $('#main .col-md-4').addClass('col-md-6').removeClass('col-md-4');
            $(this).addClass('on');
        }
    });
});

String.prototype.format = function () {
    var args = arguments;
    return this.replace(/{(\d+)}/g, function (match, number) {
        return typeof args[number] != 'undefined'
            ? args[number]
            : match
            ;
    });
};

function onError() {
    alert('error!');
}

function onSuccess(json, status) {
    var answerTemplate = $("#answerTemplate").html();
    console.log('json', json)
    var template = answerTemplate.format(json.writer, new Date(json.createDate), json.contents, json.answerId);
    $(".qna-comment-slipp-articles").prepend(template);
}

$(".answerWrite input[type='submit']").click(function (e) {
    e.preventDefault();
    var queryString = $("form[name='answer']").serialize();

    $.ajax({
        type: 'post',
        url: '/api/qna/addAnswer.do',
        data: queryString,
        dataType: 'json',
        error: onError,
        success: onSuccess
    })
});

