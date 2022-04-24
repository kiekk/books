const paths = require('./paths')

module.exports = {
  mode: 'production', // 프로덕션 모드로 설정하여 최적화 옵션들을 활성화
  entry: paths.ssrIndexJs,  // 엔트리 경로
  target: 'node', // node 환경에서 실행될 것이라는 점을 명시
  output: {
    path: paths.ssrBuild, // 빌드 경로
    filname: 'server.js', // 파일 이름
    chunkFilename: 'js/[name].chunk.js', // 청크 파일 이름
    publicPath: paths.publicUrlOrPath // 정적 파일이 제공될 경로
  }
}