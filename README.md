# 研究室蔵書管理システム
## 概要
このシステムは、研究室の書誌に関する知識グラフを作成・管理するためのシステムです。

2024/10/14現在では、ISBNのCSVを入力として知識グラフを作成し出力する機能のみを実装しています。

知識グラフの情報源として、[国会図書館のNDL Search API](https://ndlsearch.ndl.go.jp/help/api)を利用しています。

## 使用方法

1\. このリポジトリの"LabBooksManagementSystem/labBooksManagementSystem.jar"をダウンロード

2\. ダウンロードしたjarファイルを、引数に"inputのCSVファイルのパス" "outputファイルのパス" "研究室内の書籍を表すIRIのルート部分"を指定して実行

引数例: `` isbn_list_test.csv output/result.ttl http://example.com/ ``

バージョンは、Java17です。
