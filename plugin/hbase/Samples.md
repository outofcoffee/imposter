# Samples

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
