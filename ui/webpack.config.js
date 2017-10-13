const webpack = require("webpack");
const path = require('path');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const FaviconsWebpackPlugin = require('favicons-webpack-plugin');

function relative(p) {
    return path.resolve(__dirname, p);
}

const assetsdir = relative("src/main/web");
const mainSourceDir = relative("src/main/javascript");

const production = process.env.NODE_ENV === "production";

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
        }),
        new FaviconsWebpackPlugin({
            logo: path.resolve(assetsdir, "images/if_fuel_103260.png"),
            title: "Fuel Log"
        })
    ]
};

// Local Variables:
// compile-command: "node_modules/.bin/webpack -d"
// End:
