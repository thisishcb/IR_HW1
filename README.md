# HW1 Probabilistic Ranking
```shell
java -cp ./out/artifacts/HW1_index_jar/HW1.jar IndexFiles \
  -index testdata/index -docs testdata/text
```
## Indexer

original Indexer only index the filepath  
New Indexer: read files and add each document,
with fields: **DocID, DocNo, Date, Title ,Text**  
*DocID* start with 1, count doc by number  
*DocNo* are unique document number come with the file e.g. "FT911-1"  
*Date, Title* are  titles and date  
*text* is the concatenation of doc title and body, ignore paragraphs

#### Common Fields
```
FT: DOCNO   DATE      HEADLINE    TEXT
FB: DOCNO   DATE1     TI          TEXT
LA: DOCNO   DATE<p>   HEADLINE<p> TEXT<p> 
```

## Searcher
### BM25
### LM
### RM1
### RM3