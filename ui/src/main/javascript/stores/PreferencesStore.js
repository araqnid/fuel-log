import {AutoRefreshLoader} from "../util/Loaders";
import {UserDataStore} from "../util/Stores";

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
        case "IdentityStore/localUserIdentity":
            return initialState;
        default:
            return state;
    }
};

export default class PreferencesStore extends UserDataStore {
    constructor(redux) {
        super(redux);
    }

    newLoader() {
        return new AutoRefreshLoader("_api/user/preferences", 30 * 1000, {
            foundData: data => {
                this.dispatch({ type: "PreferencesStore/loaded", payload: data });
            },

            loadError: ex => {
                this.dispatch({ type: "PreferencesStore/loaded", payload: ex, error: true });
            }
        });
    }
}
