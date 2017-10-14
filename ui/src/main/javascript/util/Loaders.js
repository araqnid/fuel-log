import React from "react";
import _ from "lodash";
import microajax from "./microajax";

function asArray(x) {
    if (_.isArray(x))
        return x;
    else
        return [x];
}

export class LoaderBase {
    constructor() {
        this._aborted = false;
        this._ajaxOngoing = [];
        this._timer = null;
    }

    begin() {
    }

    abort() {
        this._aborted = true;
        _.clone(this._ajaxOngoing).forEach(req => req.abort());
        if (this._timer) {
            clearTimeout(this._timer);
        }
    }

    get aborted() {
        return this._aborted;
    }

    callAjax(options) {
        return new Promise((resolve, reject) => {
            let completed = false;
            const req = microajax({ ...options,
                success: (data, status, xhr) => {
                    completed = true;
                    resolve(data);
                },
                error: (xhr, status, e) => {
                    completed = true;
                    reject(e);
                },
                complete: (xhr, status) => {
                    if (!completed) reject(null);
                }
            });
            this._ajaxOngoing.push(req);
        });
    }

    _ajax(options) {
        if (!options.complete) {
            options.complete = this._ajaxComplete.bind(this);
        }
        else if (_.isArray(options.complete)) {
            options.complete.push(this._ajaxComplete.bind(this));
        }
        else {
            options.complete = [options.complete, this._ajaxComplete.bind(this)];
        }
        if (options.success) {
            const underlying = options.success;
            options.success = data => {
                if (this._aborted) return;
                underlying(data);
            };
        }
        if (options.error) {
            const underlying = options.error;
            options.error = data => {
                if (this._aborted) return;
                underlying(data);
            };
        }
        const req = microajax(options);
        this._ajaxOngoing.push(req);
    }

    _ajaxComplete(xhr, status) {
        _.remove(this._ajaxOngoing, xhr);
    }

    setTimeout(callback, interval) {
        this._timer = setTimeout(() => {
            this._timer = null;
            if (this._aborted) return;
            callback.call(this);
        }, interval);
    }
}

export class OneShotLoader extends LoaderBase {
    constructor(url, found) {
        super();
        this._dispatchFound = found;
        this._url = url;
    }

    begin() {
        this._ajax({
            url: this._url,
            success: data => {
                this._dispatchFound(data);
            }
        })
    }
}

function implementShouldComponentReloadFromPropertyNames(propertyNames) {
    return (thisProps, nextProps) => {
        return _(propertyNames).filter(n => thisProps[n] !== nextProps[n]).size() > 0;
    };
}

function implementShouldComponentReload(input) {
    if (_.isFunction(input))
        return input;
    if (_.isArray(input))
        return implementShouldComponentReloadFromPropertyNames(input);
    throw "shouldComponentReload should be either a function or an array of property names";
}

// https://facebook.github.io/react/docs/higher-order-components.html
export function withLoaders(WrappedComponent, initialState, shouldComponentReload, createLoaders, resetStateOnReload = true) {
    const reloadPredicate = implementShouldComponentReload(shouldComponentReload);

    const wrapper = class extends React.Component {
        constructor(props) {
            super(props);
            this.state = initialState;
        }

        componentDidMount() {
            this._beginLoading(this.props);
        }

        componentWillReceiveProps(newProps) {
            if (reloadPredicate(this.props, newProps)) {
                this._abortLoading();
                if (resetStateOnReload) {
                    this.setState(initialState, () => {
                        this._beginLoading(newProps);
                    });
                }
                else {
                    this._beginLoading(newProps);
                }
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
            if (this._loaders) return;
            const newLoaders = createLoaders(props, () => this.state, update => this.setState(update));
            this._loaders = asArray(newLoaders);
            this._loaders.forEach(loader => loader.begin());
        }

        _abortLoading() {
            if (!this._loaders) return;
            this._loaders.forEach(loader => loader.abort());
            this._loaders = null;
        }
    };

    wrapper.displayName = "withLoaders(" + (WrappedComponent.displayName || WrappedComponent.name || "Component") + ")";

    return wrapper;
}
