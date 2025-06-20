import Constant from '../Constant'

// Mutatios 기능만 구현
export default {
    [Constant.ADD_CONTACT_FORM]: (state) => {
        state.contact = { no: '', name: '', tel: '', address: '', photo: '' }
        state.mode = "add"
        state.currentView = "contactForm";
    },
    [Constant.CANCEL_FORM]: (state) => {
        state.currentView = null
    },
    [Constant.EDIT_CONTACT_FORM]: (state, payload) => {
        state.contact = payload.contact
        state.mode = "update"
        state.currentView = "contactForm"
    },
    [Constant.EDIT_PHOTO_FORM]: (state, payload) => {
        state.contact = payload.contact
        state.currentView = "updatePhoto"
    },
    [Constant.FETCH_CONTACTS]: (state, payload) => {
        state.contactlist = payload.contactlist
    }
}