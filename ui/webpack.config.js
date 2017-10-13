var webpack = require("webpack");
var path = require('path');
var HtmlWebpackPlugin = require('html-webpack-plugin');

var assetsdir = path.resolve(__dirname, "src/main/web");
var mainSourceDir = path.resolve(__dirname, "src/main/javascript");

var production = process.env.NODE_ENV === "production";

module.exports = {
    context: mainSourceDir,
    entry: ["bootstrap", path.resolve(assetsdir, "css/styles.css"), "./main"],
    output: {
        path: path.resolve(__dirname, 'build/site'),
        filename: production ? "[name]-[hash].js" : "[name].js"
    },
    devServer: {
        contentBase: assetsdir,
        port: 3000,
        proxy: {
            "/_api": "http://localhost:64064"
        }
    },
    module: {
        rules: [
            {
                test: /\.js$/,
                exclude: /(node_modules|bower_components)/,
                use: {
                    loader: 'babel-loader',
                    options: {
                        presets: [['env', { modules: false }], 'react']
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
        extensions: ['.webpack.js', '.web.js', '.js'],
        alias: {
            "lodash$": "lodash/index.js",
            "jquery$": "jquery/dist/jquery.js",
            "bootstrap$": "bootstrap/dist/js/bootstrap.js"
        }
    },
    plugins: [
        new webpack.ProvidePlugin({
            // required by bootstrap
            jQuery: "jquery"
        }),
        new HtmlWebpackPlugin({
            title: "Fuel Log",
            template: "./template.html.ejs"
        })
    ]
};

// Local Variables:
// compile-command: "node_modules/.bin/webpack -d"
// End:
