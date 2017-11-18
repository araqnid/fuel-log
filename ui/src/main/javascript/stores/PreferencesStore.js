import {AutoRefreshLoader} from "../util/Loaders";

export const reducer = (state = { preferences: null, loadFailure: null }, action) => {
    switch (action.type) {
        case "PreferencesStore/loaded":
            if (action.error) {
                return { preferences: state.preferences, loadFailure: action.payload };
            }
            else {
                return { preferences: action.payload, loadFailure: null };
            }
        case "PreferencesStore/reset":
            return { preferences: null, loadFailure: null };
        case "IdentityStore/localUserIdentity":
            return { preferences: null, loadFailure: null };
        default:
            return state;
    }
};

export default class PreferencesStore {
    constructor(redux) {
        this._redux = redux;
        this._reduxUnsubscribe = null;
        this._userId = null;
        this._loader = null;
    }

    begin() {
        this._reduxUnsubscribe = this._redux.subscribe(this.onReduxAction.bind(this));
        this.onReduxAction();
    }

    abort() {
        if (this._reduxUnsubscribe) {
            this._reduxUnsubscribe();
        }
        if (this._loader) {
            this._loader.abort();
            this._loader = null;
        }
    }

    get _reduxUserId() {
        const state = this._redux.getState();
        const identity = state.identity;
        if (!identity) return null;
        const localUser = identity.localUserIdentity;
        if (!localUser) return null;
        return localUser.user_id;
    }

    onReduxAction() {
        const nextUserId = this._reduxUserId;
        if (nextUserId !== this._userId) {
            if (this._loader) {
                this._loader.abort();
                this._loader = null;
            }
            this._userId = nextUserId;
            if (nextUserId) {
                this._loader = new AutoRefreshLoader("_api/user/preferences", 30 * 1000, {
                    foundData: data => {
                        this._redux.dispatch({ type: "PreferencesStore/loaded", payload: data });
                    },

                    loadError: ex => {
                        this._redux.dispatch({ type: "PreferencesStore/loaded", payload: ex, error: true });
                    }
                });
                this._loader.begin();
            }
        }
    }
}
