# Samples

## Using HBase CLI

Create table with one column family and add one row:

    create 'exampleTable', 'abc'
    put 'exampleTable', 'row1', 'abc:exampleCell', 'exampleValue'

Show table contents:

    scan 'exampleTable'
    
Delete cell from row:

    delete 'exampleTable', 'row1', 'abc:exampleCell'
    
Drop table

    disable 'exampleTable'
    drop 'exampleTable'

Export:

    ./hbase org.apache.hadoop.hbase.mapreduce.Export exampleTable /tmp/export

Import:

    ./hbase org.apache.hadoop.hbase.mapreduce.Import exampleTable /tmp/export

## Useful scanner JSON examples

https://gist.github.com/stelcheck/3979381

## Run HBase docker container

docker run -it --name=hbase -h hbase -p 2181:2181 -p 8080:8080 -p 16000:16000 -p 16010:16010 -p 16020:16020 -p 16030:16030 -v /c/hbase/hbasedata:/data/hbase -v /c/hbase/zookeeperdata:/data/zookeeper hyness/hbase-rest-standalone

## HBase REST requests

List tables:

    curl -X GET -H "Accept: application/json" -H "Cache-Control: no-cache" -H "Postman-Token: f47a243b-49e4-b005-650c-e8195331d212" "http://docker:8080"

Create:

    curl -X POST -H "Accept: application/json" -H "Content-Type: text/xml" -H "Cache-Control: no-cache" -H "Postman-Token: 5ebda3fe-e1e5-12a1-0010-c676564a2c96" -d '<?xml version="1.0" encoding="UTF-8"?>
    <TableSchema name="test">
      <ColumnSchema name="colfam" />
    </TableSchema>' "http://docker:8080/test/schema"

Get table schema:

    curl -X GET -H "Accept: application/json" -H "Cache-Control: no-cache" -H "Postman-Token: 3902bde6-0536-3460-10ac-e6343462cbee" "http://docker:8080/test/schema"
