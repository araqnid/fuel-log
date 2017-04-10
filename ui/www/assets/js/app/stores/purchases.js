define(['app/MemoBus', 'jquery', 'app/stores/identity'],
function(MemoBus, $, identity) {
    class Purchases {
        constructor() {
            this.bus = new MemoBus("Purchases");
            this.userId = null;
            this.refreshInterval = 30 * 1000;
            this._loading = null;
            this._sleeping = null;
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
                    else {
                        this._stopLoading();
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
            if (this._sleeping) {
                clearTimeout(this._sleeping);
                this._sleeping = null;
            }
        }
        _startLoading() {
            this._stopLoading();
            this._loading = $.ajax({
                headers: { accept: 'application/json' },
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
    return new Purchases();
});
