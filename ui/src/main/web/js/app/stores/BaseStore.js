import MemoBus from "app/MemoBus";
import nanoajax from "nanoajax";

function emptyCallback() {}

export default class BaseStore {
    constructor(name) {
        this.bus = new MemoBus(name);
    }
    start() {
    }
    stop() {
    }
    subscribe(handlers, owner) {
        this.bus.subscribeAll(handlers, owner);
    }
    unsubscribe(owner) {
        this.bus.unsubscribe(owner);
    }
    _ajax(options) {
        const nanoOptions = {
            url: options.url,
            headers: Object.assign({}, options.headers),
            method: options.method
        };
        if (!nanoOptions.headers['Accept']) {
            nanoOptions.headers['Accept'] = 'application/json';
        }
        if (options.data) {
            if (options.contentType) {
                nanoOptions.headers['Content-Type'] = options.contentType;
                nanoOptions.body = options.data;
            }
            else {
                nanoOptions.headers['Content-Type'] = 'application/json';
                nanoOptions.body = JSON.stringify(options.data);
            }
        }
        const onSuccess = options.success || emptyCallback;
        const onError = options.error || emptyCallback;
        const onComplete = options.complete || emptyCallback;

        const xhr = nanoajax.ajax(nanoOptions, (code, responseText) => {
            if (code >= 200 && code < 300) {
                if (code === 200) {
                    const contentType = xhr.getResponseHeader("Content-Type");
                    const data = contentType === "application/json" ? JSON.parse(responseText) : responseText;
                    onSuccess(data, code, xhr);
                }
                else {
                    onSuccess(null, code, xhr);
                }
            }
            else {
                onError(xhr, code, null);
            }
            onComplete(xhr, code);
        });
        return xhr;
    }
}