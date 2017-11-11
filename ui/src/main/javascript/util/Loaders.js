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

    callDelete(url, requestConfig) {
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

export function serialise(data) {
    const parts = [];
    Object.keys(data).forEach(key => {
        const value = data[key];
        if (Array.isArray(value)) {
            value.forEach(v => {
                parts.push(encodeURIComponent(key + "[]") + "=" + encodeURIComponent(v));
            });
        }
        else {
            parts.push(encodeURIComponent(key) + "=" + encodeURIComponent(value));
        }
    });
    return parts.join("&");
}

export class OneShotLoader extends AjaxLoaderBase {
    constructor(url, { found = _.noop, error = _.noop, finishedLoading = _.noop }) {
        super();
        this._dispatch = { found, error, finishedLoading };
        this._url = url;
    }

    begin() {
        super.begin();
        this.get(this._url)
            .then(({ data }) => {
                if (this.running)
                    this._dispatch.found(data);
            })
            .catch(ex => {
                if (this.running)
                    this._dispatch.error(ex);
            })
            .then(() => {
                this._finished = true;
                if (this.running)
                    this._dispatch.finishedLoading();
            });
    }

    get finished() {
        return this._finished;
    }
}

export class DelegatingLoader extends LoaderBase {
    constructor() {
        super();
        this._loaders = [];
    }

    begin() {
        super.begin();
        this._loaders.forEach(x => x.begin());
    }

    abort() {
        if (this.running) {
            this._loaders.forEach(x => x.abort());
        }
        super.abort();
    }

    register(loader) {
        this._loaders.push(loader);
        if (this.running) loader.begin();
    }
}

export function combineLoaders(loaders) {
    if (loaders.length === 1)
        return loaders[0];
    const combined = new DelegatingLoader();
    loaders.forEach(loader => {
        combined.register(loader);
    });
    return combined;
}

function implementShouldComponentReloadFromPropertyNames(propertyNames) {
    return (thisProps, nextProps) => _.some(propertyNames, n => thisProps[n] !== nextProps[n]);
}

function implementShouldComponentReload(input) {
    if (_.isFunction(input))
        return input;
    if (_.isArray(input))
        return implementShouldComponentReloadFromPropertyNames(input);
    throw "shouldComponentReload should be either a function or an array of property names";
}

// https://facebook.github.io/react/docs/higher-order-components.html
export function withLoaders(resetState, shouldComponentReload, createLoaders) {
    const reloadPredicate = implementShouldComponentReload(shouldComponentReload);

    const initialState = typeof resetState === "function" ? resetState(undefined) : resetState;
    const resetStateImpl = typeof resetState === "function" ? resetState : () => initialState;

    return WrappedComponent => {
        const wrapper = class extends React.Component {
            constructor(props) {
                super(props);
                this.state = initialState;
                this._loader = null;
            }

            componentDidMount() {
                this._beginLoading(this.props);
            }

            componentWillReceiveProps(newProps) {
                if (reloadPredicate(this.props, newProps)) {
                    this._abortLoading();
                    this.setState(resetStateImpl(this.state), () => {
                        this._beginLoading(newProps);
                    });
                }
            }

            componentWillUnmount() {
                this._abortLoading();
            }

            render() {
                const mergedProps = { ...this.props, ...this.state };
                return <WrappedComponent { ...mergedProps } />;
            }

            _beginLoading(props) {
                if (this._loader) return;
                const newLoaders = createLoaders(props, () => this.state, update => this.setState(update));
                this._loader = _.isArray(newLoaders) ? combineLoaders(newLoaders) : newLoaders;
                this._loader.begin();
            }

            _abortLoading() {
                if (!this._loader) return;
                this._loader.abort();
                this._loader = null;
            }
        };

        if (process.env.NODE_ENV !== "production") {
            wrapper.displayName = "withLoaders(" + (WrappedComponent.displayName || WrappedComponent.name || "Component") + ")";
        }

        return wrapper;
    };
}
