import {AjaxLoaderBase, DelegatingLoader, noopLoader} from "./Loaders";

export function resetOn(r) {
    const predicate = typeof r === "function" ? r : ({type}) => type === r;
    return reducer => {
        const initialState = reducer(undefined, { type: "@@INIT" });
        return (state, action) => predicate(action) ? initialState : reducer(state, action);
    }
}

export function bindActionPayload(type, initialValue = null) {
    const reducer = (state = initialValue, action) => action.type === type && !action.error ? action.payload : state;
    reducer.resetOn = resetEventType => resetOn(resetEventType)(reducer);
    return reducer;
}

export class Datum {
    constructor(owner, name) {
        this._owner = owner;
        this._name = name;
    }

    get value() {
        return this._value;
    }

    set value(value) {
        this._owner.emit(this._name, value);
        this._value = value;
    }

    facade() {
        return {
            listen: (listener) => {
                this._owner.subscribe(window, this._name, listener);
                if (this._value !== undefined) listener.call(actor, this._value);
            },
            "get": () => {
                return this._value;
            },
            subscribe: (actor, listener) => {
                this._owner.subscribe(actor, this._name, listener);
                if (this._value !== undefined) listener.call(actor, this._value);
            },
            unsubscribe: (actor) => {
                this._owner.unsubscribe(actor, this._name);
            }
        };
    }
}

export class StoreBase extends AjaxLoaderBase {
    constructor() {
        super();
        this._listeners = [];
    }

    subscribe(actor, eventType, listener) {
        this._listeners.push([actor, eventType, listener]);
    }

    unsubscribe(actor, eventType) {
        _.remove(this._listeners, elt => elt[0] === actor && elt[1] === eventType);
    }

    unsubscribeAll(actor) {
        _.remove(this._listeners, elt => elt[0] === actor);
    }

    emit(type, data) {
        this._listeners.forEach(([actor, eventType, listener]) => {
            if (eventType === type) {
                listener(data);
            }
        });
    }
}

export class UserDataStore {
    constructor(redux) {
        this._redux = redux;
        this._reduxUnsubscribe = null;
        this._userId = null;
        this._loader = null;
    }

    start() {
        this._reduxUnsubscribe = this._redux.subscribe(this.onReduxAction.bind(this));
        this.onReduxAction();
    }

    stop() {
        if (this._reduxUnsubscribe) {
            this._reduxUnsubscribe();
        }
        if (this._loader) {
            this._loader.abort();
            this._loader = null;
        }
    }

    newLoader() {
        return new DelegatingLoader(); // effectively no-op loader
    }

    dispatch(action) {
        this._redux.dispatch(action);
    }

    getState() {
        return this._redux.getState();
    }

    get _reduxUserId() {
        const state = this._redux.getState();
        const identity = state.identity;
        if (!identity) return null;
        const localUser = identity.localUserIdentity;
        if (!localUser) return null;
        return localUser.user_id;
    }

    kick() {
        if (this._loader) {
            this._loader.abort();
            this._loader = this.newLoader();
            this._loader.begin();
        }
    }

    onReduxAction() {
        const nextUserId = this._reduxUserId;
        if (nextUserId !== this._userId) {
            if (this._loader) {
                this._loader.abort();
                this._loader = null;
            }
            this._userId = nextUserId;
            if (nextUserId) {
                this._loader = this.newLoader();
                this._loader.begin();
            }
        }
    }
}

export function reduxThunkWithStores(storesAccessor) {
    return ({getState, dispatch}) => {
        return next => action => {
            if (typeof action === "function") {
                action({ dispatch, getState, stores: storesAccessor() });
                return null;
            }
            else {
                return next(action);
            }
        };
    };
}

export function exposeStoreMethodsViaDispatch(storeName, methods) {
    return dispatch => {
        const actions = {};
        methods.forEach(method => {
            actions[method] = function() {
                const methodArgs = Array.prototype.slice.call(arguments, 1);
                dispatch(({ stores }) => {
                    const store = stores[storeName];
                    store[method].apply(store, methodArgs);
                });
            };
        });
        return actions;
    };
}