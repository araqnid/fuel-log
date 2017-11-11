import {combineReducers} from "redux";
import {bindActionPayload, Datum, StoreBase} from "../util/Stores";

export const reducer = combineReducers({
    purchaseList: bindActionPayload("PurchasesStore/purchaseList"),
    loadFailure: bindActionPayload("PurchasesStore/loadFailure")
});

export const actions = dispatch => ({
    begin(purchasesStore) {
        purchasesStore.purchaseList.listen(v => dispatch({ type: "PurchasesStore/purchaseList", payload: v }));
        purchasesStore.loadFailure.listen(v => dispatch({ type: "PurchasesStore/loadFailure", payload: v }));
    }
});

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
        this.post("_api/fuel", newPurchase)
            .then(({data, status, headers}) => {
                if (status !== 201) {
                    this.emit("purchaseSubmissionFailed", {status, exception: null, data});
                    return;
                }
                const location = headers.location;
                const matches = location.match(/\/_api\/fuel\/(.+)$/);
                if (!matches) {
                    console.warn("invalid creation URI", location);
                    return;
                }
                const fuelPurchaseId = matches[1];
                this.kick();
                this.emit("purchaseSubmitted", fuelPurchaseId);
            })
            .catch(response => {
                this.emit("purchaseSubmissionFailed", {status: response.status, exception: response});
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
        this._loading = this.get("/_api/fuel")
            .then(({data}) => {
                this._purchaseList.value = data;
                this._loadFailure.value = null;
            })
            .catch((response) => {
                this._purchaseList.value = null;
                this._loadFailure.value = { status: response.status, exception: response };
            })
            .then(() => {
                this._loading = null;
                if (this.userId) {
                    this._sleeping = setTimeout(this._tick.bind(this), this.refreshInterval);
                }
            });
    }
    _tick() {
        this._sleeping = null;
        if (this.userId) {
            this._startLoading();
        }
    }
}
