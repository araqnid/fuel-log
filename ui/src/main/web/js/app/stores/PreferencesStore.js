import BaseStore from "app/stores/BaseStore";

export default class PreferencesStore extends BaseStore {
    constructor(identity) {
        super("Preferences");
        this._requesting = null;
        this._sleeping = null;
        this.user = null;
        this.identity = identity;
        this.refreshInterval = 30 * 1000;
    }
    start() {
        this.identity.subscribe({
            localIdentity: user => {
                this.bus.dispatch("preferences", null);
                this._cancel();
                this.user = user;
                if (user)
                    this._requesting = this._beginRequest();
            }
        }, this);
    }
    stop() {
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
        this._ajax({
            url: "/_api/user/preferences",
            success: (data, code, xhr) => {
                this.bus.dispatch("preferences", data);
            },
            error: (xhr, code, ex) => {
                this.bus.dispatch("loadingError", code, ex);
            },
            complete: (xhr, code) => {
                this._requesting = null;
                this._sleeping = setTimeout(this._tick.bind(this), this.refreshInterval);
            }
        })
    }
    _tick() {
        this._sleeping = null;
        this._requesting = this._beginRequest();
    }
};
