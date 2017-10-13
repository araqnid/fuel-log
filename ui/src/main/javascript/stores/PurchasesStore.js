import BUS from "../message-bus";
import BaseStore from "./BaseStore";

export default class PurchasesStore extends BaseStore {
    constructor(identity) {
        super("Purchases");
        this.identity = identity;
        this.userId = null;
        this.refreshInterval = 30 * 1000;
        this._loading = null;
        this._sleeping = null;
    }
    start() {
        this.identity.subscribe({
            localIdentity: user => {
                if (user) {
                    this.userId = user.userId;
                }
                else {
                    this.userId = null;
                }
                this.bus.dispatch("purchaseList", null);
                if (this.userId) {
                    this._startLoading();
                }
                else {
                    this._stopLoading();
                }
            }
        }, this);
    }
    kick() {
        if (this.userId) this._startLoading();
    }
    submit(newPurchase) {
        this._ajax({
            url: "/_api/fuel",
            method: "POST",
            data: newPurchase,
            success: (data, status, xhr) => {
                if (xhr.status !== 201) {
                    BUS.broadcast("NewFuelPurchaseEntry.PurchaseSubmissionFailed", {status: xhr.status, exception: null, data: data});
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
                BUS.broadcast("NewFuelPurchaseEntry.PurchaseSubmitted", fuelPurchaseId);
            },
            error: (xhr, status, ex) => {
                BUS.broadcast("NewFuelPurchaseEntry.PurchaseSubmissionFailed", {status: status, exception: ex});
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
                this.bus.dispatch("purchaseList", data);
            },
            error: (xhr, status, ex) => {
                this.bus.dispatch("loadFailure", { status: status, exception: ex });
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
