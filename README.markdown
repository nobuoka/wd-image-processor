WebDriver を用いた画像処理サーバーのプロトタイプ
==========

## サンプルの動かし方

```
docker-compose up
```

で起動するはず。 Windows でしか試していないのでもしかしたらうまくいかないかも。

起動したら下記 URL にアクセスすると画像が表示される。

* http://localhost:8080/hello?arg={%22message%22:%20%22%E3%81%93%E3%82%93%E3%81%AB%E3%81%A1%E3%81%AF%22}&signature=DypjE3COpjC2C%2BGWid8jhA8SJbY%3D
* http://localhost:8080/map?arg={%22route%22:%20[10,10,10,15,10,20,10,25,13,30,16,33,20,35,25,36],%20%22width%22:%20200,%20%22height%22:%20200}&signature=yNPbr8uynVcgoCUVM59TBkIlHlE%3D

## 仕組み

### Docker Compose で定義されているサービス

* app : アプリケーション本体。
    * 設定ファイルで指定されたエンドポイントへのリクエストがあった際に、WebDriver を使用して指定された HTML を読み込み、JS を実行し、そのスクリーンショットを撮って返す。
* wd-server-*xxx* : WebDriver のブラウザ側。 geckodriver が複数セッションを持てないので、複数インスタンスがそれぞれ 1 セッションを持つ形にしている。

### 設定ファイル

* app の `PROCESSORS_CONFIG_PATH` 環境変数で、画像処理の設定ファイルを指定する。
    * サンプル設定ファイルが [sampleProcessors/processors.json](./sampleProcessors/processors.json) にある。
    * パスをキーとして、HTML ファイルと JS ファイルを値とする JSON オブジェクト。
    * オプショナルの設定値として `key` がある。 このプロパティを定義しておくと、クエリパラメータとして `signature` を送る必要がある。
        * `signature` の計算方法は、`arg` の文字列 (URL エンコード前) を data、設定ファイルの `key` を key として HMAC-SHA1 のダイジェストを Base64 エンコードしたものである。

### JS への入力と JS からの出力

JS への入力として、URL のクエリパラメータの次のものが使用される。

* `arg` : JS に渡される文字列。 JS 側では `arguments[0]` で取得できる。

JS からの出力としては、次の形のオブジェクトを期待する。

```javascript
{
  "targetElement": element,
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
