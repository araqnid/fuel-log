import {Datum, StoreBase} from "../util/Stores";

export default class PurchasesStore extends StoreBase {
    constructor(identity) {
        super();
        this.identity = identity;
        this.userId = null;
        this.refreshInterval = 30 * 1000;
        this._loading = null;
        this._sleeping = null;
        this._purchaseList = new Datum(this, "purchaseList");
        this._loadFailure = new Datum(this, "loadFailure");
    }

    get purchaseList() {
        return this._purchaseList.facade();
    }

    get loadFailure() {
        return this._loadFailure.facade();
    }

    begin() {
        this.identity.localUserIdentity.subscribe(this, user => {
            if (user) {
                this.userId = user.user_id;
            }
            else {
                this.userId = null;
            }
            this._purchaseList.value = null;
            if (this.userId) {
                this._startLoading();
            }
            else {
                this._stopLoading();
            }
        });
    }
    abort() {
        super.abort();
        this.identity.unsubscribe(this);
    }
    kick() {
        if (this.userId) this._startLoading();
    }
    submit(newPurchase) {
        this._ajax({
            url: "/_api/fuel",
            type: "POST",
            contentType: "application/json",
            data: newPurchase,
            success: (data, status, xhr) => {
                if (xhr.status !== 201) {
                    this.emit("purchaseSubmissionFailed", {status: xhr.status, exception: null, data: data});
                    return;
                }
                const location = xhr.getResponseHeader('Location');
                const matches = location.match(/\/_api\/fuel\/(.+)$/);
                if (!matches) {
                    console.warn("invalid creation URI", location);
                    return;
                }
                const fuelPurchaseId = matches[1];
                this.kick();
                this.emit("purchaseSubmitted", fuelPurchaseId);
            },
            error: (xhr, status, ex) => {
                this.emit("purchaseSubmissionFailed", {status: status, exception: ex});
            }
        });
    }
    _stopLoading() {
        if (this._loading) {
            this._loading.abort();
            this._loading = null;
        }
        if (this._sleeping) {
            clearTimeout(this._sleeping);
            this._sleeping = null;
        }
    }
    _startLoading() {
        this._stopLoading();
        this._loading = this._ajax({
            url: "/_api/fuel",
            success: (data, status, xhr) => {
                this._purchaseList.value = data;
                this._loadFailure.value = null;
            },
            error: (xhr, status, ex) => {
                this._purchaseList.value = null;
                this._loadFailure.value = { status: status, exception: ex };
            },
            complete: (xhr, status) => {
                this._loading = null;
                if (this.userId) {
                    this._sleeping = setTimeout(this._tick.bind(this), this.refreshInterval);
                }
            }
        })
    }
    _tick() {
        this._sleeping = null;
        if (this.userId) {
            this._startLoading();
        }
    }
}
