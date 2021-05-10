var BASE_URL = '/api'

export default {
    //한 페이지에 보여줄 사이즈
    PAGESIZE: 5,

    //전체 연락처 
    FETCH: BASE_URL + "/contacts",

    //연락처 추가
    ADD: BASE_URL + "/contacts",

    //연락처 업데이트
    UPDATE: BASE_URL + "/contacts/${no}",

    //연락처 한건 조회
    FETCH_ONE: BASE_URL + "/contacts/${no}",

    //연락처 삭제
    DELETE: BASE_URL + "/contacts/${no}",

    //연락처 사진 업로드 -> 변경
    UPDATE_PHOTO: BASE_URL + "/contacts/${no}/photo"
}