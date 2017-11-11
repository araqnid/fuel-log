import {Datum, StoreBase} from "../util/Stores";

export default class PreferencesStore extends StoreBase {
    constructor(identity) {
        super();
        this._requesting = null;
        this._sleeping = null;
        this.user = null;
        this.identity = identity;
        this.refreshInterval = 30 * 1000;
        this._preferences = new Datum(this, "preferences");
        this._loadFailure = new Datum(this, "loadFailure");
    }

    get preferences() {
        return this._preferences.facade();
    }

    get loadFailure() {
        return this._loadFailure.facade();
    }

    begin() {
        this.identity.localUserIdentity.subscribe(this, user => {
            this._preferences.value = null;
            this._cancel();
            this.user = user;
            if (user)
                this._requesting = this._beginRequest();
        });
    }
    abort() {
        this._cancel();
        this.identity.unsubscribe(this);
    }
    _cancel() {
        if (this._requesting) {
            this._requesting.abort();
            this._requesting = null;
        }
        if (this._sleeping) {
            clearTimeout(this._sleeping);
            this._sleeping = null;
        }
    }
    _beginRequest() {
        this.get("_api/user/preferences")
            .then(({data}) => {
                this._preferences.value = data;
                this._loadFailure.value = null;
            })
            .catch((ex) => {
                this._preferences.value = null;
                this._loadFailure = { status: ex.status, exception: ex };
            })
            .then(() => {
                this._requesting = null;
                this._sleeping = setTimeout(this._tick.bind(this), this.refreshInterval);
            });
    }
    _tick() {
        this._sleeping = null;
        this._requesting = this._beginRequest();
    }
};
