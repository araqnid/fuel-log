import {AjaxLoaderBase} from "./Loaders";

export function resetOn(resetEventType) {
    return reducer => {
        const initialState = reducer(undefined, { type: "@@INIT" });
        return (state, action) => action.type === resetEventType ? initialState : reducer(state, action);
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
