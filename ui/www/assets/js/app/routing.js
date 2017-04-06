define([], function routing$$init() {
    function route(template, target) {
        const placeholderPattern = /\{([a-zA-Z_]?[0-9a-zA-Z_]*)\}/g;
        const placeholders = [];
        let patternSource = '^';
        let lastpos = 0;
        while ((match = placeholderPattern.exec(template)) !== null) {
            patternSource += template.substring(lastpos, match.index);
            lastpos = placeholderPattern.lastIndex;
            placeholders.push(match[1]);
            patternSource += "([^/]+)";
        }
        patternSource += template.substring(lastpos);
        patternSource += '$';
        const pattern = new RegExp(patternSource);
        return function route$$exec(routeString) {
            const match = pattern.exec(routeString);
            if (!match) return null;
            let data;
            if (placeholders.length === 0) {
                data = null;
            }
            else if (placeholders.length === 1 && placeholders[0] === '') {
                data = match[1];
            }
            else {
                data = {};
                for (var i = 0; i < placeholders.length; i++) {
                    data[placeholders[i]] = match[i+1];
                }
            }
            return [target, data];
        };
    }

    const routes = [];
    routes.push(route("", "ToEmpty"));
    routes.push(route("activity", "ToActivity"));
    routes.push(route("agency_escorts/{}", "ToBrowse"));
    routes.push(route("agency_escort/{}", "ToEscort"));
    routes.push(route("agency_escort/{escort_id}/historical/{event_id}", "ToHistoricalEscort"));

    return function routing(routeString) {
        const routed = false;
        let to = null;
        for (let i = 0; i < routes.length && to === null; i++) {
            to = routes[i](routeString);
        }
        if (to) {
            BUS.broadcast("Navigate." + to[0], to[1]);
        }
        else {
            BUS.broadcast("Navigate.ToUnknown", routeString);
        }
    };
});
