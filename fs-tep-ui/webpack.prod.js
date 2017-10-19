const path = require('path');
const webpack = require('webpack');
const webpackMerge = require('webpack-merge');
const commonConfig = require('./webpack.common.js');
const AotPlugin = require('@ngtools/webpack').AotPlugin;
const CopyWebpackPlugin = require('copy-webpack-plugin');

const config = (env = {}) => {
    return webpackMerge(commonConfig(env), {
        module: {
            rules: [
                {
                    test: /\.ts$/,
                    use: [
                        {
                            loader: '@ngtools/webpack',
                            options: { 
                                
                            }
                        }
                    ]
            }
            ]
        },
        plugins: [
            new AotPlugin({
                tsConfigPath: './tsconfig.aot.json',
                entryModule: 'src/app/app.module#AppModule'
            }),
            new webpack.optimize.UglifyJsPlugin({
                sourceMap: true,
                beautify: false,
                mangle: {
                    screw_ie8: true,
                    keep_fnames: true
                },
                compress: {
                    warnings: false,
                    screw_ie8: true,
                     /**
                     * Turning sequences and conditionals off seems to be key to getting breakpoints to stay on
                     * the intended lines. https://github.com/webpack/webpack/issues/4084
                     */
                    sequences: false,
                    conditionals: false
                },
                comments: false
            }),
            new CopyWebpackPlugin([
                {
                    context: path.resolve(__dirname, 'assets', 'data'),
                    from: '**/*',
                    to: path.join(env.outpath || path.resolve(__dirname, "dist"), 'assets', 'data')
                }
            ])
        ],
        devtool: 'source-map'
    });
}
module.exports = config;
