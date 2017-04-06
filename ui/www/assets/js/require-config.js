require.config({
    baseUrl: "assets/js/lib/",
    paths: {
        app: "../app",
        JSXTransformer: "jsx-transformer-requirejs"
    },
    map: {
        "*": {
            "react": "react-with-addons"
        }
    },
    jsx: {
        fileExtension: ".jsx"
    },
    shim: {
        bootstrap: { deps: ["jquery"] },
        'jquery.ba-hashchange': { deps: ["jquery"] },
        lodash: { exports: '_' },
        galleria: { exports: 'Galleria' },
        "galleria.classic": { deps: ["galleria"] }
    },
});
