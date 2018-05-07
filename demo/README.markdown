WebDriver を用いた画像処理サーバーのデモ
==========

* AWS ECS 上で動かすデモ。

## Terraform でのインフラ構築

[terraform ディレクトリ](./terraform/) に、インフラ構築用の Terraform ファイルが入っている。

### 準備

準備として、./terraform/aws-token.auto.tfvars ファイルとして下記の内容を記述する。
(下記の内容の変数を指定できれば良いので、別の方法を採っても良い。)

```
aws_access_key = "YOUR_AWS_ACCESS_KEY"
aws_secret_key = "YOUR_AWS_SECRET_KEY"
```

この IAM ユーザーは、下記のサービスに対する権限を持っている必要がある。

* CloudWatch Logs
* ECS
* VPC
* IAM (Role の編集)

### 構築実行

実行は、terraform ディレクトリにおいて、下記コマンドを実行すること。

```
terraform apply
```
