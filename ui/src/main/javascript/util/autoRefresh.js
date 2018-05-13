import Observable from "zen-observable";

class AutoRefreshSubscription {
    constructor(observable, delayMillis, observer) {
        this._observable = observable;
        this._delayMillis = delayMillis;
        this._observer = observer;
        this._subscription = null;
        this._refreshTimer = null;
        this.begin();
    }

    begin() {
        this._subscription = this._observable.subscribe(
            (value) => {
                this._observer.next(value);
            },
            (error) => {
                this._observer.error(error);
                this._subscription = null;
            },
            () => {
                this._subscription = null;
                this._scheduleRefresh();
            }
        )
    }

    _scheduleRefresh() {
        this._refreshTimer = window.setTimeout(() => this._timerTick(), this._delayMillis);
    }

    _timerTick() {
        this._refreshTimer = null;
        this.begin();
    }

    unsubscribe() {
        if (this._subscription) {
            this._subscription.unsubscribe();
            this._subscription = null;
        }
        if (this._refreshTimer) {
            window.clearTimeout(this._refreshTimer);
            this._refreshTimer = null;
        }
    }
}

export default function autoRefresh(delayMillis) {
    return observable => {
        return new Observable(observer => new AutoRefreshSubscription(observable, delayMillis, observer));
    }
}
