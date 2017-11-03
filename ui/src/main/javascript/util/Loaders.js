import React from "react";
import _ from "lodash";
import microajax from "./microajax";

function asArray(x) {
    if (_.isArray(x))
        return x;
    else
        return [x];
}

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
    }

    begin() {
        super.begin();
    }

    abort() {
        super.abort();
        _.clone(this._ajaxOngoing).forEach(req => req.abort());
    }

    callAjax(options) {
        return new Promise((resolve, reject) => {
            let completed = false;
            const req = microajax({ ...options,
                success: (data, status, xhr) => {
                    completed = true;
                    if (this.aborted) {
                        reject("Aborted")
                    }
                    if (options.includeXhr) {
                        resolve({ data: data, status: status, xhr: xhr });
                    }
                    else {
                        resolve(data);
                    }
                },
                error: (xhr, status, e) => {
                    completed = true;
                    if (this.aborted) {
                        reject("Aborted")
                    }
                    reject(e);
                },
                complete: (xhr, status) => {
                    if (!completed) {
                        if (this.aborted) {
                            reject("Aborted")
                        }
                        reject("Completed without calling success/error");
                    }
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
            options.success = (data, status, xhr) => {
                if (this.aborted) return;
                underlying(data, status, xhr);
            };
        }
        if (options.error) {
            const underlying = options.error;
            options.error = (xhr, code, ex) => {
                if (this.aborted) return;
                underlying(xhr, code, ex);
            };
        }
        const req = microajax(options);
        this._ajaxOngoing.push(req);
    }

    _ajaxComplete(xhr) {
        _.remove(this._ajaxOngoing, xhr);
    }
}

export class OneShotLoader extends AjaxLoaderBase {
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
