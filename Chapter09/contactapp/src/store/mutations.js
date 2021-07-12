import Constant from '../Constant'

// Mutatios 기능만 구현
export default {
    [Constant.FETCH_CONTACTS]: (state, payload) => {
        state.contactlist = payload.contactlist
    },
    [Constant.FETCH_CONTACT_ONE]: (state, payload) => {
        state.contact = payload.contact
    },
    [Constant.INITIALIZE_CONTACT_ONE]: (state) => {
        state.contact = { no: '', name: '', tel: '', address: '', photo: '' }
    }
}