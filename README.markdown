Image processing server (processed by JavaScript on WebDriver remote end)
==========

[![CircleCI](https://circleci.com/gh/nobuoka/wd-image-processor.svg?style=svg)](https://circleci.com/gh/nobuoka/wd-image-processor)
[![codecov](https://codecov.io/gh/nobuoka/wd-image-processor/branch/develop/graph/badge.svg)](https://codecov.io/gh/nobuoka/wd-image-processor)

* AWS ECS でのデモについては [demo](./demo/README.markdown) を参照。

## The way to run sample image processors on localhost

First, build application and run services by execute following commands.

```
./gradlew :app:installDist
docker-compose up --build
```

Then, visiting following URLs, you will see the processed images.

* http://localhost:8080/hello?arg={%22message%22:%20%22%E3%81%93%E3%82%93%E3%81%AB%E3%81%A1%E3%81%AF%22}&signature=DypjE3COpjC2C%2BGWid8jhA8SJbY%3D
* http://localhost:8080/map?arg={%22route%22:%20[10,10,10,15,10,20,10,25,13,30,16,33,20,35,25,36],%20%22width%22:%20200,%20%22height%22:%20200}&signature=yNPbr8uynVcgoCUVM59TBkIlHlE%3D

## 仕組み

### Docker Compose で定義されているサービス

* app : アプリケーション本体。
    * 設定ファイルで指定されたエンドポイントへのリクエストがあった際に、WebDriver を使用して指定された HTML を読み込み、JS を実行し、そのスクリーンショットを撮って返す。
* wd-server-*xxx* : WebDriver のブラウザ側。 geckodriver が複数セッションを持てないので、複数インスタンスがそれぞれ 1 セッションを持つ形にしている。

### 設定ファイル

app の `PROCESSORS_CONFIG_PATH` 環境変数で、画像処理の設定ファイルを指定する。
サンプル設定ファイルが [sampleProcessors/config.json](./sampleProcessors/config.json) にある。

```javascript
{
  // Access-Control-Allow-Origin ヘッダフィールドの値として返される origin の配列。
  // 省略した場合は Access-Control-Allow-Origin ヘッダフィールドは返されない。
  "accessControlAllowOrigins": [
    "http://example.com"
  ],
  // 画像処理器のオブジェクト。
  // プロパティ名がエンドポイントのパスとして使われる。
  // 値は HTML / JS のファイルのパス (html と js プロパティ) と、シグネチャ計算用の key。
  // シグネチャ計算用の key は省略可能。 (指定した場合はリクエストのクエリパラメータで signature を送る必要がある。)
  "processors": {
    "map": {
      "html": "./map/map.html",
      "js": "./map/map.js",
      "key": "sample-key-map"
    },
    "hello": {
      "html": "./simple/main.html",
      "js": "./simple/main.js",
      "key": "sample-key-hello"
    }
  }
}
```

`signature` の計算方法は、`arg` の文字列 (URL エンコード前) を data、設定ファイルの `key` を key として HMAC-SHA1
のダイジェストを Base64 エンコードしたものである。

### JS への入力と JS からの出力

JS への入力として、URL のクエリパラメータの次のものが使用される。

* `arg` : JS に渡される文字列。 JS 側では `arguments[0]` で取得できる。

JS からの出力としては、次の形のオブジェクトを期待する。

```javascript
{
  // ステータスコード。 省略しても良い。 省略した場合は 200 扱い。
  "statusCode": 200,
  // レスポンスの内容を表現するオブジェクト。 下記の例の他に、文字列も返すことができる。 詳細は後述。
  "content": {
    "type": "screenshot",
    "targetElement": element
  },
  // キャッシュに関する値。 省略しても良い。
  "httpCache": {
      // Cache-Control ヘッダの max-age の値。
      // この値が指定されている場合、`Cache-Control: public, max-age=...` というヘッダが付与される。
      // 省略された場合は `Cache-Control` ヘッダは付与されない。
      "maxAge": 28800
  },

  // 以下は過去互換のために残っているもの。

  // スクリーンショットの対象となる要素。 これは古い書き方で、最新のものは `content` プロパティを使う方法。
  // 省略された場合でステータスコードが 200 の場合は body 要素全体を対象とする。
  // 省略された場合でステータスコードが 200 以外の場合は、レスポンスは空となる。
  "targetElement": element
}
```

`content` の値としては、下記のものがある。

```javascript
// スクリーンショット。 `image/png` で返される。
"content": {
  "type": "screenshot",
  // スクリーンショットの対象となる要素。
  // 省略された場合でステータスコードが 200 の場合は body 要素全体を対象とする。
  // 省略された場合でステータスコードが 200 以外の場合は、レスポンスは空となる。
  "targetElement": element,
  // 画像形式。 "png" または "jpeg"。 省略した場合や他の値の場合には "png" 扱い。
  "imageType": "png",
}

// 文字列。 `text/plain; charset=utf-8` で返される。
"content": {
  "type": "text",
  "value": "この文字列がレスポンスボディとして返される。"
}
```

* `targetElement` : HTML 要素。 この要素の範囲がスクリーンショットとして取得される。 `null` でもよく、その場合は `body` 要素全体が対象となる。

出力する範囲などは JS 側で自由に設定できる。
呼び出し側で幅や高さを指定したい場合は、URL のクエリパラメータの `arg` をオブジェクトの形にして
`width` プロパティや `height` プロパティを渡せばよい。

## 開発する場合は

アプリケーション部分は Kotlin で書かれている。
IDE としては IntelliJ IDEA を使うのがオススメ。

## Acknowledgements

* This idea was provided by [wakaba](https://github.com/wakaba)
* Thanks to [OND Inc.](https://ond-inc.com/)
