# Results

|test name|jvm|graal|
|-----|----|-----|
|Simple| 2m31s 429ms| 344.403 |
|OwnSplit|2m14s 420ms| 261.533 |
|OwnSplitStringGetters|2m13s 678ms| 284.627 |
|OwnSplitDoubleParser|2m18s 268ms| 258.547 |
|OwnSplitDoubleActiveParser|2m37s 259ms| 220.126 |
|OwnSplitDoubleActiveParserStaticWorkingArray|2m38s 971ms| 221.96 |
|OwnSplitDoubleActiveParserIndexBased|1m54s 862ms| 175.773 |
|OwnSplitDoubleActiveParserIndexBasedHashFun|6m14s 564ms| 792.27 |
|OwnSplitDoubleActiveParserIndexBasedLimitedHashFun|2m31s 465ms| 242.127 |
|OwnSplitDoubleActiveParserIndexBasedTwoThreads|1m33s 71ms| 124.994 |
|pl.jborkows.bilion.runners.OwnSplitDoubleActiveParserIndexBasedMultipleThreads_1|1m21s 547ms| 313.982 |
|pl.jborkows.bilion.runners.OwnSplitDoubleActiveParserIndexBasedMultipleThreads_2|1m21s 609ms| 108.082 |
|pl.jborkows.bilion.runners.OwnSplitDoubleActiveParserIndexBasedMultipleThreads_3|1m24s 141ms| 107.795 |
|pl.jborkows.bilion.runners.ReadBytesSy|with buffer 262144-> 2m21s 545ms| 136.77 |
|pl.jborkows.bilion.runners.ReadBytesSync2nd|1m16s 534ms| 109.119 |
|pl.jborkows.bilion.runners.ReadBytesSyncFirstToLines|0m28s 802ms| 38.316 |