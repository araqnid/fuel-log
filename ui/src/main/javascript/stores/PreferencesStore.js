import {UserDataStore} from "../util/Stores";
import autoRefresh from "../util/autoRefresh";
import ajaxObservable from "../util/ajaxObservable";

const initialState = { preferences: null, loadFailure: null };

export const reducer = (state = initialState, action) => {
    switch (action.type) {
        case "PreferencesStore/loaded":
            if (action.error) {
                return { preferences: state.preferences, loadFailure: action.payload };
            }
            else {
                return { preferences: action.payload, loadFailure: null };
            }
        case "PreferencesStore/reset":
            return initialState;
        default:
            return state;
    }
};

const preferencesObservable = ajaxObservable("_api/user/preferences", { headers: { "Accept": "application/json", "X-Requested-With": "XMLHttpRequest" } })
    .map(data => ({ type: "PreferencesStore/loaded", payload: data }));

export default class PreferencesStore extends UserDataStore {
    constructor(redux) {
        super(redux, autoRefresh(30 * 1000)(preferencesObservable));
    }

    onError(error) {
        this.dispatch({ type: "PreferencesStore/loaded", payload: error, error: true });
    }

    reset() {
        this.dispatch({ type: "PreferencesStore/reset" });
    }
}
