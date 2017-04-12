var webpack = require("webpack");
var path = require('path');
var HtmlWebpackPlugin = require('html-webpack-plugin');

var assetsdir = path.resolve(__dirname, "www/assets");

module.exports = {
    context: assetsdir + "/js",
    entry: ["app/pages/main", "bootstrap", path.resolve(assetsdir, "css/styles.css")],
    output: {
        path: assetsdir + "/_pack",
        filename: "[name].js"
    },
    module: {
        rules: [
            {
                test: /\.jsx?$/,
                exclude: /(node_modules|bower_components)/,
                use: {
                    loader: 'babel-loader',
                    options: {
                        presets: [['es2015', { modules: false }], 'react']
                    }
                }
            },
            {
                test: /\.css$/,
                use: [ 'style-loader', 'css-loader' ]
            },
            {
                test: /jquery\.ba-hashchange\.js/,
                loader: "imports-loader?this=>global"
            },
            {
                test: /\.(png|woff|woff2|eot|ttf|svg)$/,
                loader: 'url-loader?limit=100000'
            }
        ]
    },
    resolve: {
        // root: [assetsdir + "/js/lib", assetsdir],
        extensions: ['.webpack.js', '.web.js', '.js', '.jsx'],
        alias: {
            "app": assetsdir + "/js/app",
            "lodash$": "lodash/index.js",
            "jquery$": "jquery/dist/jquery.js",
            "bootstrap$": "bootstrap/dist/js/bootstrap.js",
            "react$": "react/react.js",
            "react-addons-css-transition-group$": "react-addons-css-transition-group/index.js",
            "jquery.ba-hashchange$": assetsdir + "/js/lib/jquery.ba-hashchange.js",
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
};

// Local Variables:
// compile-command: "nodejs node_modules/webpack/bin/webpack.js -d    --display-modules"
// End:
