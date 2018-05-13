import React from "react";
import _ from "lodash";
import axios from "axios";

const INITIAL = Symbol("INITIAL");
const RUNNING = Symbol("RUNNING");
const ABORTED = Symbol("ABORTED");

export class LoaderBase {
    constructor() {
        this._state = INITIAL;
    }

    begin() {
        this._state = RUNNING;
    }

    abort() {
        this._state = ABORTED;
    }

    get state() {
        return this._state;
    }

    get running() {
        return this._state === RUNNING;
    }

    get aborted() {
        return this._state === ABORTED;
    }
}

export class AjaxLoaderBase extends LoaderBase {
    constructor() {
        super();
        this._ajaxOngoing = [];
        this._cancelSources = [];
    }

    abort() {
        super.abort();
        _.clone(this._ajaxOngoing).forEach(req => req.abort());
        this._cancelSources.forEach(source => {
            source.cancel();
        });
    }

    get(url, requestConfig) {
        const cancelSource = axios.CancelToken.source();
        const promise = axios.get(url, { ...requestConfig, cancelToken: cancelSource.token });
        this._registerCancellation(cancelSource, promise);
        return promise;
    }

    ["delete"](url, requestConfig) {
        const cancelSource = axios.CancelToken.source();
        const promise = axios.delete(url, { ...requestConfig, cancelToken: cancelSource.token });
        this._registerCancellation(cancelSource, promise);
        return promise;
    }

    post(url, data, requestConfig) {
        const cancelSource = axios.CancelToken.source();
        const promise = axios.post(url, data, { ...requestConfig, cancelToken: cancelSource.token });
        this._registerCancellation(cancelSource, promise);
        return promise;
    }

    _registerCancellation(cancelSource, promise) {
        this._cancelSources.push(cancelSource);
        promise.catch((ex) => {}).then(() => {
            _.remove(this._cancelSources, cancelSource);
        });
    }
}

export class AutoRefreshLoader extends AjaxLoaderBase {
    constructor(url, refreshInterval, { foundData = _.noop, loadError = _.noop, finishedLoading = _.noop, startingRefresh = _.noop }) {
        super();
        this._url = url;
        this._refreshInterval = refreshInterval;
        this._sleeping = null;
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
        this.get(this._url)
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
