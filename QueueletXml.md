# queuelet定義 #

> queuelet containerは、queuelet定義に沿って動作します。queuelet定義ファイルは、xmlファイルで以下の構成となっています。
```
<?xml version="1.0" ?>
<queueApp stopMode="1" >
  <loader name="loader1" callStack="true" home="${queuelet.user.home}" delegate="false">
  </loader>

  <queuelet type="generalJava" loaderName="loader1">
    <param name="className" value="naru.test.queuelet.MainCallFunction"/>
  </queuelet>

  <terminal name="async" threadCount="1" maxQueueLength="128" enqueWait="true">
    <queuelet type="asyncCall" loaderName="loader1"/>
  </terminal>
</queueApp>
```
> # タグの説明 #
  * queueAppタグ
    1. attribute
      * checkInterval
      * properties
      * managerPort
      * stopMode 99999|infinity|check|manager
      * check:interval毎にqueueを監視して動きがなければ停止する。
> > > > //停止したくない場合は、
> > > > 99999:99999秒後に停止する。
> > > > infinity:監視側からは停止しない。
> > > > //store | none[**]/remote/server
      * storeServer | none[**]/this/other
      * storePort
      * storeHost
> > > > //自力でHSQLDBを立ち上げるか否かの判定
> > > > //行き先がないterminalの行き先
    1. 子タグ
      * loader
      * terminal
      * queuelet
      * store
      * properties
      * sysProperty
  * propertiesタグ(queuelet.properties相当を複数持つ機能)
    1. attribute
      * name
      * value
    1. 子タグ

> > > なし
  * sysProperty(-Dをシュミレート)
    1. attribute
> > > file//この名前の指定には、queuelet.properties,環境変数の定義値が使える
    1. 子タグ
> > > なし
  * loaderタグ
    1. attribute
      * name
      * loaderClassName
      * delegate
      * resouceLoader
> > > > ["system"|"parent"|"null"]//外から与えられたclasspath設定を当該loaderでロードする場合使用(systemとparentはほぼ同じだがsystemはおまけ)
> > > > parentは、commonLoaderの親をresouceLoaderとする。デバッグ時に有効、delegate="false"の場合実質的な動作をおこなう
      * returnValue
      * before
      * after
      * callStack
    1. 子タグ
      * classHooker
      * classpath
      * lib
  * classpathタグ
    1. attribute
      * path
    1. 子タグ

> > > なし
  * libタグ
    1. attribute
      * path
    1. 子タグ
> > > なし
  * classHookerタグ
    1. attribute
      * name
      * returnValue
      * before
      * after
      * callStack
    1. 子タグ
      * methodHooker
  * methodHookerタグ
    1. attribute
      * name
      * signature
      * concurrence
      * returnValue
      * before
      * after
      * callStack
    1. 子タグ
> > > なし
  * terminalタグ
    1. attribute
      * name
      * threadCount
      * maxQueueLength
      * enqueBlock(前の名前enqueWait)
      * priority(2010/5/15追加)
> > > > //store | none[**]/inProcess/server
> > > > //checkInterval
      * storePort
      * storeHost
      * storeReflesh | true|false[**]
      * acceptPort
    1. 子タグ
      * queuelet
      * store
  * queueletタグ
    1. attribute
      * className
      * type
      * loaderName
      * nextTerminal
      * memberClassName//このクラス型の場合だけserviceが呼び出される。
      * factoryClassName
      * factoryMethodName
    1. 子タグ
      * param
  * paramタグ
    1. attribute
      * name
      * value
    1. 子タグ

> > > なし
  * storeタグ
    1. attribute
      * startupServer:true(typeは、自動的にserverとなる)|false、起動時にstoreサーバを起動する
      * type : server|inProcess
      * port
      * host
      * refresh : [false](false.md)|true ...そのterminalの情報を起動時に削除するか否か
      * interval
      * loaderName
    1. 子タグ
> > > なし