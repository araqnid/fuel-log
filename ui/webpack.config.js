const webpack = require("webpack");
const path = require('path');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const FaviconsWebpackPlugin = require('favicons-webpack-plugin');

function relative(p) {
    return path.resolve(__dirname, p);
}

const contentBaseDir = relative("src/main/web");
const sourceDir = relative("src/main/javascript");
const outputDir = relative('build/site');

function contentFile(p) {
    return path.resolve(contentBaseDir, p);
}

const production = process.env.NODE_ENV === "production";

module.exports = {
    context: sourceDir,
    entry: ["bootstrap", contentFile("css/styles.css"), "./main"],
    output: {
        path: outputDir,
        filename: production ? "[name]-[hash].js" : "[name].js"
    },
    devServer: {
        contentBase: contentBaseDir,
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
                test: /\.(png|woff|woff2|eot|ttf|svg)$/,
                loader: 'url-loader?limit=100000'
            }
        ]
    },
    resolve: {
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
            logo: contentFile("images/if_fuel_103260.png"),
            title: "Fuel Log"
        })
    ]
};
