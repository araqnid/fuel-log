define(['app/MemoBus', 'jquery', 'app/stores/identity'],
function(MemoBus, $, identity) {
    class Purchases {
        constructor() {
            this.bus = new MemoBus("Purchases");
            this.userId = null;
        }
        subscribe(handlers, owner) {
            this.bus.subscribeAll(handlers, owner);
        }
        unsubscribe(owner) {
            this.bus.unsubscribe(owner);
        }
        start() {
            identity.subscribe({
                localIdentity: user => {
                    this.userId = user.userId;
                    this.bus.dispatch("purchaseList", null);
                    if (this.userId) {
                        this._startLoading();
                    }
                }
            }, this);
            BUS.subscribe("NewFuelPurchaseEntry.PurchaseSubmitted", id => {
                this.kick();
            }, this)
        }
        kick() {
            if (this.userId) this._startLoading();
        }
        _stopLoading() {
            if (this._loading) {
                this._loading.abort();
                this._loading = null;
            }
        }
        _startLoading() {
            this._stopLoading();
            this._loading = $.ajax({
                headers: { accept: 'application/json' },
                url: "/_api/fuel",
                success: (data, status, xhr) => {
                    console.log("success; ", data, this);
                    this.bus.dispatch("purchaseList", data);
                },
                error: (xhr, status, ex) => {

                },
                complete: (xhr, status) => {
                    this._loading = null;
                }
            })
        }
    }
    return new Purchases();
});
