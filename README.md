# 研究室蔵書管理システム
## 概要
このシステムは、研究室の書誌に関する知識グラフを作成・管理するためのシステムです。

2024/10/14現在では、ISBN(13桁)のCSVを入力として知識グラフを作成し出力する機能のみを実装しています。

知識グラフの情報源として、[国会図書館のNDL Search API](https://ndlsearch.ndl.go.jp/help/api)を利用しています。

## 使用方法

1. このリポジトリの"LabBooksManagementSystem/labBooksManagementSystem.jar"をダウンロード
2. ダウンロードしたjarファイルを、引数に"inputのCSVファイルのパス" "outputファイルのパス" "研究室内の書籍を表すIRIのルート部分"を指定して実行

引数例: `` isbn_list_test.csv output/result.ttl http://example.com/ ``

バージョンは、Java17です。

入力のcsvファイルは1行目のみ読み込まれるため、入力したいISBNはすべて1行目にカンマ区切りで記入してください。

### 入力に使うCSVファイルの作り方例
おすすめの作り方は、バーコードリーダー + Excel(or Google Spread Sheet)を使う方法です。ここでは、実際に私が所属する大阪電気通信大学 古崎研究室が持つ書籍のISBNリストを作成した方法を紹介します。
1. [バーコードリーダー](https://www.iodata.jp/product/interface/barcodereader/br-ccdts2/index.htm)を使って、Google Spread Sheet(GSS)に入力
  1. このとき、バーコードリーダーの設定で、ターミネーターをTabに指定しておくと、自動で横のセルに移動してくれるので楽です
  2. ISBN10桁しかないときは、[本のみちしるべさんの変換サイト](https://www.hon-michi.net/isbn.cgi)で変換して手動で入力しました
3. すべての本についてISBNを入力できたら、"ファイル→ダウンロード→カンマ区切り形式(.csv)"でファイルを保存して作成完了です
