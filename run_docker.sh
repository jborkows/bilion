#!/bin/bash
echo "BUILDING"
docker build --target graalRunner -t bilion:graalRunner .
echo "GRAAL version build"
docker build --target jvmRunner -t bilion:jvmRunner .
echo "JVM version build"

echo "RUNNING..."
docker run bilion:graalRunner | tee >(grep Meet > graal.csv)
sed -i "s/graal: //g" graal.csv   
echo "GRAAL done"
docker run bilion:jvmRunner | tee >(grep Meet > jvm.csv)
sed -i "s/jvm: //g" jvm.csv   
echo "JVM done"
echo "|name|jvm|graal|"> report.md
echo "|-----|----|----|">> report.md
#
paste -d ':' jvm.csv graal.csv | while IFS=':' read -r line1 line2; do
first_part=`echo "$line1" | sed "s/Meet \(.*\) seconds for \(.*\)/|\2|\1/g"`
second_part=`echo "$line2" | sed "s/Meet \(.*\) seconds for .*/|\1|/g"`
echo "$first_part""$second_part" >> report.md
done
