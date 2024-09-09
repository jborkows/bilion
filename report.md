|name| jvm     |graal|
|-----|---------|----|
|Simple| 171.025 |389.655|
|OwnSplit| 149.7   |284.558|
|OwnSplitStringGetters| 141.456 |315.15|
|OwnSplitDoubleParser| 142.652 |279.852|
|OwnSplitDoubleActiveParser| 165.658 |235.776|
|OwnSplitDoubleActiveParserStaticWorkingArray| 163.29  |235.431|
|OwnSplitDoubleActiveParserIndexBased| 123.05  |189.373|
|OwnSplitDoubleActiveParserIndexBasedHashFun| 400.072 |857.141|
|OwnSplitDoubleActiveParserIndexBasedLimitedHashFun| 159.798 |272.198|
|OwnSplitDoubleActiveParserIndexBasedTwoThreads| 99.574  |134.769|
|pl.jborkows.bilion.runners.OwnSplitDoubleActiveParserIndexBasedMultipleThreads_1| 85.06   |432.068|
|pl.jborkows.bilion.runners.OwnSplitDoubleActiveParserIndexBasedMultipleThreads_2| 85.21   |124.621|
|pl.jborkows.bilion.runners.OwnSplitDoubleActiveParserIndexBasedMultipleThreads_3| 82.97   |116.461|
|pl.jborkows.bilion.runners.ReadBytesSync with buffer 262144| 132.6   |287.644|
|pl.jborkows.bilion.runners.ReadBytesSync2nd| 74.067  |115.42|
|pl.jborkows.bilion.runners.ReadBytesSyncFirstToLines| 30.433  |42.174|
|pl.jborkows.bilion.runners.ReadBytesSyncFirstToLines| 30.433  |42.174|
|StagedRunner|25.59|49.288|