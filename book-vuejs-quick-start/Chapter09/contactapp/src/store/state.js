import CONF from '../Config'

export default {
    isloading: false,
    contact: { no: 0, name: '', tel: '', address: '', photo: '' },
    contactlist: {
        pageno: 1,
        pagesize: CONF.PAGESIZE,
        totalCount: 0,
        contacts: []
    }
}