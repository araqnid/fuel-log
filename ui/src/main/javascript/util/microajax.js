import _ from "lodash";
import nanoajax from "nanoajax";

function emptyCallback() {}

function singleCallback(input) {
    if (_.isFunction(input))
        return input;
    if (_.isArray(input)) {
        return function() {
            const callbackArguments = arguments;
            input.forEach(target => {
                target.call(callbackArguments);
            })
        };
    }
    if (!input) {
        return emptyCallback;
    }
    throw "Unhandled type of callback: " + input;
}

function serialise(data) {
    const parts = [];
    for (const key of Object.keys(data)) {
        const value = data[key];
        if (Array.isArray(value)) {
            value.forEach(v => {
                parts.push(encodeURIComponent(key + "[]") + "=" + encodeURIComponent(v));
            });
        }
        else {
            parts.push(encodeURIComponent(key) + "=" + encodeURIComponent(value));
        }
    }
    return parts.join("&");
}

export default function microajax(options) {
    const nanoOptions = {
        url: options.url,
        headers: Object.assign({}, options.headers),
        method: options.method
    };
    if (!nanoOptions.headers['Accept']) {
        nanoOptions.headers['Accept'] = 'application/json';
    }
    if (options.xhrFields) {
        _.forEach(options.xhrFields, (value, key) => {
            nanoOptions[key] = value;
        });
    }
    if (options.data) {
        if (!options.type || options.type === "GET") {
            if (typeof options.data === "string")
                nanoOptions.url = options.url + "?" + options.data;
            else
                nanoOptions.url = options.url + "?" + serialise(options.data);
        }
        else {
            nanoOptions.method = options.type ? options.type : "POST";
            if (options.contentType) {
                nanoOptions.headers['Content-Type'] = options.contentType;
                if (options.contentType === "application/json" && typeof options.data !== String) {
                    nanoOptions.body = JSON.stringify(options.data);
                }
                else {
                    nanoOptions.body = options.data;
                }
            }
            else {
                nanoOptions.body = serialise(options.data);
            }
        }
    }
    const onSuccess = singleCallback(options.success);
    const onError = singleCallback(options.error);
    const onComplete = singleCallback(options.complete);

    const xhr = nanoajax.ajax(nanoOptions, (code, responseText) => {
        if (code >= 200 && code < 300) {
            if (code === 200) {
                const contentType = xhr.getResponseHeader("Content-Type").toLowerCase();
                const data = contentType === "application/json" || contentType.startsWith("application/json;") ? JSON.parse(responseText) : responseText;
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
