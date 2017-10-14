export function logFactory(prefix, delegate = console) {
    return {
        info: function() {
            if (process.env.NODE_ENV !== "production") {
                const params = Array.prototype.slice.call(arguments, 1, arguments.length);
                params.unshift(prefix + ": " + arguments[0]);
                delegate.log.apply(delegate, params);
            }
        },
        log: function() {
            if (process.env.NODE_ENV !== "production") {
                const params = Array.prototype.slice.call(arguments, 1, arguments.length);
                params.unshift(prefix + ": " + arguments[0]);
                delegate.log.apply(delegate, params);
            }
        },
        warn: function() {
            const params = Array.prototype.slice.call(arguments, 1, arguments.length);
            params.unshift(prefix + ": " + arguments[0]);
            delegate.warn.apply(delegate, params);
        }
    }
}
