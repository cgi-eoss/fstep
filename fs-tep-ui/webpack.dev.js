const path = require('path');
const webpackMerge = require('webpack-merge');
const commonConfig = require('./webpack.common.js');

const config = (env) => {
    return webpackMerge(commonConfig(env),{
        module: {
            rules: [
                {
                    test: /\.ts$/,
                    use: [
                        {
                            loader: 'awesome-typescript-loader',
                            options: { 
                                configFileName: path.resolve(__dirname, 'tsconfig.json') 
                            }
                        },
                        {
                            loader: 'angular2-template-loader'
                        }
                    ]
                }
            ]
        },
        devtool: 'cheap-module-eval-source-map',
        devServer: {
            port: 8080,
            historyApiFallback: true,
            proxy: {
                '/proba-v': {
                    target: 'https://proba-v-mep.esa.int/api',
                    secure: false,
                    changeOrigin: true,
                    pathRewrite: {
                    '^/proba-v': ''
                    }
                },
                '/fstep-wms': {
                    target: 'http://cgi-int.eoss-cloud.it/geoserver/wms',
                    secure: false,
                    changeOrigin: true,
                    pathRewrite: {
                    '^/fstep-wms': ''
                    }
                }
            }
        }
    });
}

module.exports = config;
