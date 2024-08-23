# Results

|test name|jvm|graal|
|-----|----|-----|
|Simple| 2m31s 429ms| |
|OwnSplit|2m14s 420ms| |
|OwnSplitStringGetters|2m13s 678ms| |
|OwnSplitDoubleParser|2m18s 268ms| |
|OwnSplitDoubleActiveParser|2m37s 259ms| |
|OwnSplitDoubleActiveParserStaticWorkingArray|2m38s 971ms| |
|OwnSplitDoubleActiveParserIndexBased|1m54s 862ms| |
|OwnSplitDoubleActiveParserIndexBasedHashFun|6m14s 564ms| |
|OwnSplitDoubleActiveParserIndexBasedLimitedHashFun|2m31s 465ms| |
|OwnSplitDoubleActiveParserIndexBasedTwoThreads|1m33s 71ms| |
|pl.jborkows.bilion.runners.OwnSplitDoubleActiveParserIndexBasedMultipleThreads_1|1m21s 547ms| |
|pl.jborkows.bilion.runners.OwnSplitDoubleActiveParserIndexBasedMultipleThreads_2|1m21s 609ms| |
|pl.jborkows.bilion.runners.OwnSplitDoubleActiveParserIndexBasedMultipleThreads_3|1m24s 141ms| |
|pl.jborkows.bilion.runners.ReadBytesSy|with buffer 262144-> 2m21s 545ms| |
|pl.jborkows.bilion.runners.ReadBytesSync2nd|1m16s 534ms| |
|pl.jborkows.bilion.runners.ReadBytesSyncFirstToLines|0m28s 802ms| |