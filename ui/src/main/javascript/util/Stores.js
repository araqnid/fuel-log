import {LoaderBase} from "./Loaders";

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
}

export class StoreBase extends LoaderBase {
    constructor() {
        super();
        this._listeners = [];
    }

    subscribe(actor, eventType, listener) {
        this._listeners.push([actor, eventType, listener]);
    }

    unsubscribeAll(actor) {
        _.remove(this._listeners, elt => elt[0] === actor)
    }

    emit(type, data) {
        this._listeners.forEach(([actor, eventType, listener]) => {
            if (eventType === type) {
                listener(data);
            }
        });
    }
}
