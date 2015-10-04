# ようこそ Queuelet Projectへ #
> queueletは、java軽量containerで、処理をqueue単位で実行するフレームワークです。
> 高性能なサーバを記述するのに適しています。
> アプリケーションは、以下のような単純な処理の連続で記述します。
  * Terminalからディスパッチされ、渡されたオブジェクトを処理して次のTermnalに渡す
> Terminalの配置や実行多重度は、xmlで定義し、後からチューニングできます。
> Aspect機能があり、queueletを意識せずに一般に作成されたプログラムをリコンパイルすることなく、実行時にqueuelet上で非同期処理にすることができます。

> ## 利用シーン ##
    * サーバ（デーモン）プログラムをjavaで簡単に書きたい
    * 実行多重度の最適値をダイナミックに設定したい
    * クラスパスの設定が面倒、Webアプリみたいに配置しただけで使いたい
    * 同一VMで複数のjavaプログラムを動作させたい
    * リコンパイルなしにログを取得したい
    * 同期処理を実行時に非同期処理に変更したい

> ## queuelet定義 ##
> queueletは、以下の要素から構成されています。
    * ClassLoader アプリケーションを動作させるclassLoader
    * Terminal queueの窓口
    * Queuelet 実処理を行うアプリケーション
> これらを定義するためにxmlファイルを記述します。記載方法の詳細は、[queuelet定義](QueueletXml.md)を参照してください。

> ## [Dependency](Dependency.md) ##