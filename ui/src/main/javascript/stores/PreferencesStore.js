import _ from "lodash";
import {AjaxLoaderBase} from "../util/Loaders";

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

class PreferencesLoader extends AjaxLoaderBase {
    constructor({ foundData = _.noop, loadError = _.noop, finishedLoading = _.noop, startingRefresh = _.noop }) {
        super();
        this._sleeping = null;
        this._refreshInterval = 30 * 1000;
        this._dispatch = { foundData, loadError, finishedLoading, startingRefresh };
    }

    begin() {
        super.begin();
        this._beginRequest();
    }

    abort() {
        if (this._sleeping) {
            clearTimeout(this._sleeping);
        }
        super.abort();
    }

    _beginRequest() {
        this.get("_api/user/preferences")
            .then(({data}) => {
                this._dispatch.foundData(data);
            })
            .catch((ex) => {
                this._dispatch.loadError(ex);
            })
            .then(() => {
                this._dispatch.finishedLoading();
                this._sleeping = setTimeout(this._tick.bind(this), this._refreshInterval);
            });
    }

    _tick() {
        this._sleeping = null;
        this._dispatch.startingRefresh();
        this._beginRequest();
    }
}

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
                this._loader = new PreferencesLoader({
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
