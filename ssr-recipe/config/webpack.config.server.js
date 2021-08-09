const paths = require("./paths");
const getCSSModuleLocalIdent = require("react-dev-utils/getCSSModuleLocalIdent");
// CSS Module의 고유 className을 만들때 필요한 옵션

const webpack = require("webpack");
const getClientEnvironment = require("./env");
const nodeExternals = require("webpack-node-externals");

const env = getClientEnvironment(paths.publicUrlOrPath.slice(0, -1));

const cssRegex = /\.css$/;
const cssModuleRegex = /\.module\.css$/;
const sassRegex = /\.(scss|sass)$/;
const sassModuleRegex = /\.module\.(scss|sass)$/;

module.exports = {
  mode: "production", // 프로덕션 모드, 최적화 옵션 활성화
  entry: paths.ssrIndexJs, // 엔트리 경로
  target: "node", // node 환경에서 실행
  output: {
    path: paths.ssrBuild, // 빌드 경로
    filename: "server.js", // 파일 이름
    chunkFilename: "js/[name].chunk.js", // 청크 파일 이름
    publicPath: paths.publicUrlOrPath, // 정적 파일 제공될 경로
  },
  module: {
    rules: [
      {
        oneOf: [
          // 자바스크립트를 위한 처리
          // webpack.config.js를 참고하여 작성
          {
            test: /\.(js|mjs|jsx|ts|tsx)$/,
            include: paths.appSrc,
            loader: require.resolve("babel-loader"),
            options: {
              customize: require.resolve(
                "babel-preset-react-app/webpack-overrides"
              ),
              plugins: [
                [
                  require.resolve("babel-plugin-named-asset-import"),
                  {
                    loaderMap: {
                      svg: {
                        ReactComponent: "@svgr/webpack?-svgo![path]",
                      },
                    },
                  },
                ],
              ],
              cacheDirectory: true,
              cacheCompression: false,
              compact: false,
            },
          },
          // CSS를 위한 처리
          {
            test: cssRegex,
            exclude: cssModuleRegex,
            // exportOnlyLocals: true 옵션을 설정해야 실제 CSS 파일 생성 X
            loader: require.resolve("css-loader"),
            options: {
              modules: {
                exportOnlyLocals: true,
              },
            },
          },
          // CSS Module을 위한 처리
          {
            test: cssModuleRegex,
            loader: require.resolve("css-loader"),
            options: {
              modules: {
                exportOnlyLocals: true,
              },
              getLocalIdent: getCSSModuleLocalIdent,
            },
          },
          // Sass를 위한 처리
          {
            test: sassRegex,
            exclude: sassModuleRegex,
            use: [
              {
                loader: require.resolve("css-loader"),
                options: {
                  modules: {
                    exportOnlyLocals: true,
                  },
                },
              },
              require.resolve("sass-loader"),
            ],
          },
          // Sass + CSS Module을 위한 처리
          {
            test: sassRegex,
            exclude: sassModuleRegex,
            use: [
              {
                loader: require.resolve("css-loader"),
                options: {
                  modules: {
                    exportOnlyLocals: true,
                  },
                  getLocalIdent: getCSSModuleLocalIdent,
                },
              },
              require.resolve("sass-loader"),
            ],
          },
          // url-loader를 위한 설정
          {
            test: [/\.bmp$/, /\.gif$/, /\.jpe?g$/, /\.png$/],
            loader: require.resolve("url-loader"),
            options: {
              emitFile: false, // 파일을 따로 저장하지 않는 옵션
              limit: 10000,
              name: "static/media/[name].[hash:8].[ext]",
            },
          },
          // 위에서 설정된 확장자를 제외한 파일들은
          // file-loader를 사용합니다.
          {
            loader: require.resolve("file-loader"),
            exclude: [/\.(js|mjs|jsx|ts|tsx)/, /\.html$/, /\.json$/],
            options: {
              emitFile: false,
              name: "static/media/[name].[hash:8].[ext]",
            },
          },
        ],
      },
    ],
  },
  resolve: {
    modules: ["node_modules"],
  },
  externals: [nodeExternals()],
  plugins: [new webpack.DefinePlugin(env.stringified)],
};
