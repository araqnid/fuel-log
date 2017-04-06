var webpack = require("webpack");

var assetsdir = __dirname + "/www/assets";

var config = {
    context: assetsdir + "/js",
    entry: {
        main: "app/pages/main",
        status: "app/pages/status"
    },
    output: {
        path: assetsdir + "/_pack",
        filename: "[name].js"
    },
    module: {
        loaders: [
            {
                test: /\.jsx?$/,
                exclude: /(node_modules|bower_components|js\/lib\/)/,
                loader: 'babel-loader?presets[]=react&presets[]=es2015'
            },
            {
                test: /jquery\.ba-hashchange\.js/,
                loader: "imports?this=>global"
            },
            {
                test: /galleria\.js/,
                loader: "imports?this=>global"
            },
            {
                test: /galleria\.classic\.js/,
                loader: "imports?Galleria=galleria"
            }
        ]
    },
    resolve: {
        root: [assetsdir + "/js/lib", assetsdir],
        extensions: ['', '.webpack.js', '.web.js', '.js', '.jsx'],
        alias: {
            "app": assetsdir + "/js/app",
            "lodash": "lodash/index.js",
            "jquery": "jquery/dist/jquery.js",
            "bootstrap": "bootstrap/dist/js/bootstrap.js",
            "react$": "react/react.js",
            "react-addons-css-transition-group": "react-addons-css-transition-group/index.js"
        }
    },
    plugins: [
        new webpack.ProvidePlugin({
            // needed by hashchange plugin
            $: "jquery",
            jQuery: "jquery",
            "window.jQuery": "jquery"
        })
    ]
}

if (process.env.NODE_ENV === "production") {
    config.plugins.push(new webpack.optimize.UglifyJsPlugin({ sourceMap: false, compress: { warnings: false } }));
    config.plugins.push(new webpack.optimize.OccurenceOrderPlugin());
    config.plugins.push(new webpack.DefinePlugin({ "process.env": { NODE_ENV: JSON.stringify("production") } }));
}

module.exports = config;

// Local Variables:
// compile-command: "nodejs node_modules/webpack/bin/webpack.js -d    --display-modules"
// End:
