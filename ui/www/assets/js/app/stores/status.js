define(['app/MemoBus', 'jquery'],
function(MemoBus, $) {
    class Status {
        constructor() {
            this.bus = new MemoBus("Status");
            this.statusRequest = null;
            this.readinessRequest = null;
            this.versionRequest = null;
            this.timer = null;
            this.paused = true;
            this.refreshInterval = 500; // millis
            this.bus.dispatch("refreshState", { paused: this.paused, interval: this.refreshInterval });
        }
        subscribe(handlers, owner) {
            const wasEmpty = this.bus.isEmpty();
            this.bus.subscribeAll(handlers, owner);
            if (wasEmpty && !this.bus.isEmpty()) {
                this._beginRequest();
            }
        }
        unsubscribe(owner) {
            this.bus.unsubscribe(owner);
            if (this.bus.isEmpty()) {
                this._abortRequests();
                if (this.timer !== null) {
                    clearTimeout(this.timer);
                    this.timer = null;
                }
            }
        }
        _requestInProgress() {
            return this.statusRequest !== null || this.readinessRequest !== null || this.versionRequest !== null;
        }
        _abortRequests() {
            if (this.statusRequest !== null) {
                this.statusRequest.abort();
                this.statusRequest = null;
            }
            if (this.readinessRequest !== null) {
                this.readinessRequest.abort();
                this.readinessRequest = null;
            }
            if (this.versionRequest !== null) {
                this.versionRequest.abort();
                this.versionRequest = null;
            }
        }
        pause() {
            this.paused = true;
            if (this.timer !== null) {
                clearTimeout(this.timer);
                this.timer = null;
            }
            this.bus.dispatch("refreshState", { paused: this.paused, interval: this.refreshInterval });
        }
        unpause() {
            this.paused = false;
            if (this.timer === null && !this._requestInProgress() && !this.bus.isEmpty()) {
                this._schedule();
            }
            this.bus.dispatch("refreshState", { paused: this.paused, interval: this.refreshInterval });
        }
        updateRefreshInterval(interval) {
            this.refreshInterval = interval;
            this.bus.dispatch("refreshState", { paused: this.paused, interval: this.refreshInterval });
        }
        kick() {
            if (this._requestInProgress()) return;
            if (this.timer !== null) {
                clearTimeout(this.timer);
                this.timer = null;
            }
            this._beginRequest();
        }
        _beginRequest() {
            this._beginStatusRequest();
            this._beginReadinessRequest();
            this._beginVersionRequest();
        }
        _beginStatusRequest() {
            this.statusRequest = $.ajax({
                headers: { accept: 'application/json' },
                url: "/_api/info/status",
                success: (data, status, xhr) => {
                    this.bus.dispatch("status", data);
                },
                error: (xhr, status, ex) => {
                    this.bus.dispatch("status.error", ex);
                },
                complete: (xhr, status) => {
                    this.statusRequest = null;
                    this._requestCompleted();
                }
            });
        }
        _beginReadinessRequest() {
            this.readinessRequest = $.ajax({
                headers: { accept: 'text/plain' },
                url: "/_api/info/readiness",
                success: (data, status, xhr) => {
                    this.bus.dispatch("readiness", data);
                },
                error: (xhr, status, ex) => {
                    this.bus.dispatch("readiness.error", ex);
                },
                complete: (xhr, status) => {
                    this.readinessRequest = null;
                    this._requestCompleted();
                }
            });
        }
        _beginVersionRequest() {
            this.versionRequest = $.ajax({
                headers: { accept: 'application/json' },
                url: "/_api/info/version",
                success: (data, status, xhr) => {
                    this.bus.dispatch("version", data);
                },
                error: (xhr, status, ex) => {
                    this.bus.dispatch("version.error", ex);
                },
                complete: (xhr, status) => {
                    this.versionRequest = null;
                    this._requestCompleted();
                }
            });
        }
        _requestCompleted() {
            if (this.statusRequest === null && this.readinessRequest === null && this.versionRequest === null)
                this._schedule();
        }
        _timerTick() {
            this.timer = null;
            if (!this.paused) {
                this._beginRequest();
            }
        }
        _schedule() {
            this.timer = setTimeout(this._timerTick.bind(this), this.refreshInterval);
        }
    }
    return new Status();
});
